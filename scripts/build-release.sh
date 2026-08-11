#!/usr/bin/env bash
# Builds the Android APK and drops a copy named nextelis-v<version>-<type>.apk
# into releases/ (repo root) and /sdcard/Download on a connected device (via
# adb, if one is attached). Version comes from android/app/build.gradle.kts's
# versionName — bump it there, not here.
#
#   ./scripts/build-release.sh            # debug build (default, for testing)
#   ./scripts/build-release.sh --release  # signed release build (for distribution)
#
# --release requires android/keystore.properties (gitignored) — see
# docs/NEXTELIS-V1.md §8. Without it the build would be unsigned and
# uninstallable, so this script refuses rather than emitting a broken APK.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$REPO_ROOT/android"
BUILD_GRADLE="$ANDROID_DIR/app/build.gradle.kts"

BUILD_TYPE="debug"
if [ "${1:-}" = "--release" ]; then
    BUILD_TYPE="release"
fi

VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_GRADLE")
if [ -z "$VERSION" ]; then
    echo "Couldn't read versionName from $BUILD_GRADLE" >&2
    exit 1
fi

if [ "$BUILD_TYPE" = "release" ] && [ ! -f "$ANDROID_DIR/keystore.properties" ]; then
    echo "Release build needs $ANDROID_DIR/keystore.properties (gitignored)." >&2
    echo "Create it with storeFile/storePassword/keyAlias/keyPassword." >&2
    exit 1
fi

echo "Building NexTelis v$VERSION ($BUILD_TYPE)..."
if [ "$BUILD_TYPE" = "release" ]; then
    (cd "$ANDROID_DIR" && ./gradlew :app:assembleRelease)
else
    (cd "$ANDROID_DIR" && ./gradlew :app:assembleDebug)
fi

OUTPUT_DIR="$ANDROID_DIR/app/build/outputs/apk/$BUILD_TYPE"

# ABI splits are enabled (app/build.gradle.kts), so Gradle emits one APK per
# ABI plus a universal fallback instead of a single app-<type>.apk. arm64-v8a
# covers virtually all real phones and is far smaller than the universal
# APK — that's what gets installed by default.
APK_SRC="$OUTPUT_DIR/app-arm64-v8a-$BUILD_TYPE.apk"
if [ ! -f "$APK_SRC" ]; then
    echo "arm64-v8a APK not found, falling back to universal APK." >&2
    APK_SRC="$OUTPUT_DIR/app-universal-$BUILD_TYPE.apk"
fi
if [ ! -f "$APK_SRC" ]; then
    echo "No $BUILD_TYPE APK found in $OUTPUT_DIR" >&2
    exit 1
fi

# A release APK that didn't actually get signed installs nowhere, and the
# failure is obscure on-device — catch it here instead. Must use apksigner:
# v2+ signatures live in a binary block outside the zip entries, so looking
# for META-INF/*.RSA only detects legacy v1 JAR signing and false-alarms on
# every modern build.
if [ "$BUILD_TYPE" = "release" ]; then
    APKSIGNER=$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
    if [ -x "${APKSIGNER:-}" ]; then
        if "$APKSIGNER" verify "$APK_SRC" >/dev/null 2>&1; then
            echo "Signature verified."
        else
            echo "ERROR: $APK_SRC failed signature verification." >&2
            exit 1
        fi
    else
        echo "apksigner not found — skipped signature verification." >&2
    fi
fi

APK_NAME="nextelis-v${VERSION}-${BUILD_TYPE}.apk"

mkdir -p "$REPO_ROOT/releases"
cp "$APK_SRC" "$REPO_ROOT/releases/$APK_NAME"
echo "Copied to releases/$APK_NAME"

if command -v adb >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
    adb push "$APK_SRC" "/sdcard/Download/$APK_NAME" >/dev/null
    echo "Pushed to device: /sdcard/Download/$APK_NAME"
else
    echo "No adb device connected — skipped pushing to phone." >&2
fi
