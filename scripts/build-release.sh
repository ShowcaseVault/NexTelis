#!/usr/bin/env bash
# Builds the Android debug APK and drops a copy named nextelis-v<version>-debug.apk
# into releases/ (repo root), ~/Downloads on this machine, and /sdcard/Download
# on a connected device (via adb, if one is attached). Version comes from
# android/app/build.gradle.kts's versionName — bump it there, not here.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$REPO_ROOT/android"
BUILD_GRADLE="$ANDROID_DIR/app/build.gradle.kts"

VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_GRADLE")
if [ -z "$VERSION" ]; then
    echo "Couldn't read versionName from $BUILD_GRADLE" >&2
    exit 1
fi

echo "Building NexTelis v$VERSION..."
(cd "$ANDROID_DIR" && ./gradlew :app:assembleDebug)

OUTPUT_DIR="$ANDROID_DIR/app/build/outputs/apk/debug"

# ABI splits are enabled (app/build.gradle.kts), so Gradle emits one APK per
# ABI plus a universal fallback instead of a single app-debug.apk. arm64-v8a
# covers virtually all real phones and is far smaller than the universal
# APK — that's what gets installed by default.
APK_SRC="$OUTPUT_DIR/app-arm64-v8a-debug.apk"
if [ ! -f "$APK_SRC" ]; then
    echo "arm64-v8a APK not found, falling back to universal APK." >&2
    APK_SRC="$OUTPUT_DIR/app-universal-debug.apk"
fi
if [ ! -f "$APK_SRC" ]; then
    echo "No debug APK found in $OUTPUT_DIR" >&2
    exit 1
fi

APK_NAME="nextelis-v${VERSION}-debug.apk"

mkdir -p "$REPO_ROOT/releases"
cp "$APK_SRC" "$REPO_ROOT/releases/$APK_NAME"
echo "Copied to releases/$APK_NAME"

if command -v adb >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
    adb push "$APK_SRC" "/sdcard/Download/$APK_NAME" >/dev/null
    echo "Pushed to device: /sdcard/Download/$APK_NAME"
else
    echo "No adb device connected — skipped pushing to phone." >&2
fi
