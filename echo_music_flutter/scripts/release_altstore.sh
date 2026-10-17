#!/usr/bin/env bash
# Builds the iOS app, publishes it as a GitHub release and adds the new version
# to the AltStore source (source.json at the repo root).
#
# Usage: scripts/release_altstore.sh [-n "What's new"] [--dry-run]
#
# Bump `version:` in pubspec.yaml and commit it first. The script refuses to run
# with uncommitted changes or when the release tag already exists.
#
# Env: GH_USER  GitHub account that owns the repo (default: dev-zayn)
#      REPO     owner/name of the repo (default: dev-zayn/Echo-Music)
set -euo pipefail

GH_USER="${GH_USER:-dev-zayn}"
REPO="${REPO:-dev-zayn/Echo-Music}"
NOTES=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--notes) NOTES="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

die() { echo "error: $*" >&2; exit 1; }

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT="$(git -C "$APP_DIR" rev-parse --show-toplevel)"
SOURCE="$ROOT/source.json"

# --- Checks -----------------------------------------------------------------
[[ -f "$SOURCE" ]] || die "missing $SOURCE"
command -v fvm >/dev/null || die "fvm is not installed"
if [[ $DRY_RUN -eq 0 ]]; then
  [[ -z "$(git -C "$ROOT" status --porcelain)" ]] || die "commit or stash your changes first"
  [[ "$(git -C "$ROOT" branch --show-current)" == main ]] || die "switch to main first"
fi

GH_TOKEN="$(gh auth token --user "$GH_USER")" || die "gh is not logged in as $GH_USER (run: gh auth login)"
export GH_TOKEN GH_USER

PUBSPEC_VERSION="$(sed -n 's/^version:[[:space:]]*//p' "$APP_DIR/pubspec.yaml")"
VERSION="${PUBSPEC_VERSION%%+*}"
BUILD="${PUBSPEC_VERSION#*+}"
[[ "$BUILD" != "$PUBSPEC_VERSION" ]] || BUILD=1
TAG="ios-v$VERSION"
IPA_NAME="EchoMusic-$VERSION.ipa"

if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  die "release $TAG already exists; bump version in pubspec.yaml"
fi
python3 -I - "$SOURCE" "$VERSION" "$BUILD" <<'PY' || die "version $VERSION ($BUILD) is already in source.json"
import json, sys
src, version, build = sys.argv[1:]
for app in json.load(open(src))["apps"]:
    for v in app["versions"]:
        if v["version"] == version and v["buildVersion"] == build:
            sys.exit(1)
PY

echo "==> Releasing Echo Music $VERSION ($BUILD) as $TAG"

# --- Build & package ----------------------------------------------------------
cd "$APP_DIR"
fvm flutter build ios --release --no-codesign

APP="$APP_DIR/build/ios/iphoneos/Runner.app"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir "$STAGE/Payload"
cp -R "$APP" "$STAGE/Payload/"
rm -f "$APP_DIR/build/$IPA_NAME"
(cd "$STAGE" && zip -qr "$APP_DIR/build/$IPA_NAME" Payload)
IPA="$APP_DIR/build/$IPA_NAME"
SIZE="$(stat -f%z "$IPA")"
echo "==> Packaged $IPA ($SIZE bytes)"

# --- Update source.json (in place; committed after the release exists) ------
# Version, build and minimum iOS come from the built Info.plist, and the privacy
# strings are synced from it, because AltStore refuses to install an app whose
# permissions differ from the source listing.
python3 -I - "$SOURCE" "$APP/Info.plist" "$SIZE" \
  "https://github.com/$REPO/releases/download/$TAG/$IPA_NAME" \
  "$(date +%Y-%m-%d)" "${NOTES:-Echo Music $VERSION.}" <<'PY'
import json, plistlib, sys
src, plist_path, size, url, date, notes = sys.argv[1:]
info = plistlib.load(open(plist_path, "rb"))
source = json.load(open(src))
app = next(a for a in source["apps"] if a["bundleIdentifier"] == info["CFBundleIdentifier"])
app["versions"].insert(0, {
    "version": info["CFBundleShortVersionString"],
    "buildVersion": info["CFBundleVersion"],
    "date": date,
    "localizedDescription": notes,
    "downloadURL": url,
    "size": int(size),
    "minOSVersion": info.get("MinimumOSVersion", "15.0"),
})
app["appPermissions"]["privacy"] = {
    k: v for k, v in sorted(info.items()) if k.endswith("UsageDescription")
}
with open(src, "w") as f:
    json.dump(source, f, indent=2, ensure_ascii=False)
    f.write("\n")
PY

if [[ $DRY_RUN -eq 1 ]]; then
  echo "==> Dry run: built $IPA and updated source.json locally; nothing published."
  echo "    Revert with: git -C \"$ROOT\" checkout source.json"
  exit 0
fi

# --- Publish ----------------------------------------------------------------
git_push() {
  git -C "$ROOT" -c credential.helper= \
    -c 'credential.helper=!f(){ echo username=$GH_USER; echo password=$GH_TOKEN; }; f' \
    push -q origin main
}

# The release tag must point at a commit that exists on GitHub, so push first
# with source.json set aside, and put it back whether or not the push works.
git -C "$ROOT" stash push -q -- source.json
if ! git_push; then
  git -C "$ROOT" stash pop -q
  die "push failed; source.json is updated locally but nothing was released"
fi
git -C "$ROOT" stash pop -q

gh release create "$TAG" "$IPA" -R "$REPO" \
  --target "$(git -C "$ROOT" rev-parse HEAD)" \
  --title "Echo Music iOS $VERSION" \
  --notes "${NOTES:-Echo Music $VERSION for iOS.}

Install with AltStore by adding the source https://raw.githubusercontent.com/$REPO/main/source.json, or sideload the unsigned IPA below (AltStore/Sideloadly re-sign it with your Apple ID)."

git -C "$ROOT" add source.json
git -C "$ROOT" commit -q -m "chore(ios): publish $VERSION ($BUILD) to AltStore source"
git_push

echo "==> Done: https://github.com/$REPO/releases/tag/$TAG"
