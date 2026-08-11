#!/usr/bin/env bash
# Publishes a GitHub release for the current version and uploads the signed
# APK. Version comes from android/app/build.gradle.kts's versionName, so the
# tag can never drift from what the APK actually reports.
#
#   ./scripts/publish-release.sh            # publish v<version>
#   ./scripts/publish-release.sh --draft    # create as a draft to review first
#
# Requires: gh (authenticated), and a release APK built by
# ./scripts/build-release.sh --release
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_GRADLE="$REPO_ROOT/android/app/build.gradle.kts"

DRAFT_FLAG=""
if [ "${1:-}" = "--draft" ]; then
    DRAFT_FLAG="--draft"
fi

VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_GRADLE")
if [ -z "$VERSION" ]; then
    echo "Couldn't read versionName from $BUILD_GRADLE" >&2
    exit 1
fi

TAG="v$VERSION"
APK="$REPO_ROOT/releases/nextelis-v${VERSION}-release.apk"

if ! command -v gh >/dev/null 2>&1; then
    echo "gh CLI not found. See https://cli.github.com/" >&2
    exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
    echo "gh is not authenticated. Run: gh auth login" >&2
    exit 1
fi

if [ ! -f "$APK" ]; then
    echo "No release APK at $APK" >&2
    echo "Build one first: ./scripts/build-release.sh --release" >&2
    exit 1
fi

# A debug-signed APK in a public release would be installable-but-untrusted,
# and can't be upgraded to a properly signed build later. Refuse early.
APKSIGNER=$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
if [ -x "${APKSIGNER:-}" ]; then
    if ! "$APKSIGNER" verify "$APK" >/dev/null 2>&1; then
        echo "ERROR: $APK failed signature verification." >&2
        exit 1
    fi
    echo "Signature verified."
else
    echo "apksigner not found — skipping signature check." >&2
fi

if gh release view "$TAG" >/dev/null 2>&1; then
    echo "Release $TAG already exists." >&2
    echo "Delete it first (gh release delete $TAG) or bump versionName." >&2
    exit 1
fi

# Warn rather than block: tagging a dirty tree is sometimes intentional, but
# it means the tag won't reproduce the artifact being uploaded.
if [ -n "$(git -C "$REPO_ROOT" status --porcelain)" ]; then
    echo "WARNING: working tree has uncommitted changes; $TAG won't match them." >&2
fi

NOTES_FILE=$(mktemp)
trap 'rm -f "$NOTES_FILE"' EXIT

cat > "$NOTES_FILE" <<'NOTES'
Private, self-hosted Internet telephony. Register a user, get a NexTelis
number, pair an Android device, and place and receive real voice calls
through the phone's native dialer.

### Install

Download the APK below and sideload it. Android will ask you to allow
installs from unknown sources.

If you have an older NexTelis build installed, **uninstall it first** — this
release is signed with a different key, so Android will refuse to upgrade
over it.

### First run

1. Enter your server's host and port (not `localhost` — the phone needs the
   server's LAN address).
2. Register, then **save the recovery code**. It is shown once and is the
   only way to move your number to another phone.
3. Grant permissions, then enable NexTelis in
   **Settings › Calling accounts**.
4. Dial another NexTelis number from the normal dialer.

Server setup is in [the project README](https://github.com/ShowcaseVault/NexTelis#readme).

### Known limitations

- **OnePlus/OxygenOS is unsupported** — the ROM removes the calling-accounts
  toggle, so NexTelis can register but never be enabled. Samsung One UI is
  the tested target.
- **No TLS**, on either the control or media plane. Use on a trusted network.
- **Verified on a LAN.** NAT traversal across the open Internet is not solved.
- Losing your recovery code means losing the account; there is no reset.

Full detail: [docs/NEXTELIS-V1.md](https://github.com/ShowcaseVault/NexTelis/blob/main/docs/NEXTELIS-V1.md)
NOTES

echo "Publishing $TAG..."
gh release create "$TAG" "$APK#NexTelis $VERSION (arm64-v8a, signed)" \
    --title "NexTelis $VERSION" \
    --notes-file "$NOTES_FILE" \
    --latest \
    $DRAFT_FLAG

echo
gh release view "$TAG" --json url --jq '"Published: " + .url'
