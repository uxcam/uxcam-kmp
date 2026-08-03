#!/usr/bin/env bash
# Bundle the locally staged Maven artifacts and upload them to the Sonatype Central Portal.
#
# Mirrors the native Android SDK's release mechanism: modules publish into one staging
# directory laid out as a Maven repository, that directory is zipped into a single deployment
# bundle, and the bundle is POSTed to the Portal Publisher API. The Portal names the deployment
# after the zip, so this produces "uxcam-kmp-<version>-bundle.zip" rather than the anonymous
# "com.uxcam (via OSSRH Staging API)" that PUT-ing to the OSSRH shim yields.
#
# Usage:  scripts/publish-central.sh <version> [staging-dir]
#
# Credentials (Central Portal user token — NOT your portal login):
#   OSSRH_USERNAME / OSSRH_PASSWORD, or ossrhUsername / ossrhPassword in gradle.properties.
#
# Publishing type:
#   UXCAM_MAVEN_CENTRAL_AUTO_PUBLISH=true  -> AUTOMATIC: released as soon as validation passes.
#   anything else (default)                -> USER_MANAGED: lands VALIDATED, a human presses
#                                             Publish at central.sonatype.com/publishing/deployments.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VERSION="${1:?usage: publish-central.sh <version> [staging-dir]}"
STAGING="${2:-$ROOT/build/maven-central-staging}"
BUNDLE_DIR="$ROOT/build/maven-central-bundle"
BUNDLE="$BUNDLE_DIR/uxcam-kmp-$VERSION-bundle.zip"

read_gradle_prop() {
  local key="$1" file="$HOME/.gradle/gradle.properties"
  [[ -f "$file" ]] && sed -n "s/^$key=\(.*\)$/\1/p" "$file" | head -1
}

USER="${OSSRH_USERNAME:-$(read_gradle_prop ossrhUsername)}"
PASS="${OSSRH_PASSWORD:-$(read_gradle_prop ossrhPassword)}"
if [[ -z "$USER" || -z "$PASS" ]]; then
  echo "error: Central Portal credentials missing (OSSRH_USERNAME/OSSRH_PASSWORD)" >&2
  exit 1
fi

if [[ ! -d "$STAGING/com/uxcam" ]]; then
  echo "error: no staged artifacts at $STAGING/com/uxcam" >&2
  echo "       run the publishAllPublicationsToCentralStagingRepository tasks first" >&2
  exit 1
fi

# Central rejects a deployment whose artifacts lack signatures, and the failure surfaces late
# and cryptically. Catch it here instead.
missing_sigs="$(find "$STAGING/com/uxcam" -type f \
  \( -name '*.jar' -o -name '*.aar' -o -name '*.klib' -o -name '*.pom' -o -name '*.module' \) \
  ! -name '*.asc' -print0 | while IFS= read -r -d '' f; do
    [[ -f "$f.asc" ]] || echo "$f"
  done)"
if [[ -n "$missing_sigs" ]]; then
  echo "error: these staged artifacts have no .asc signature:" >&2
  echo "$missing_sigs" >&2
  echo "       set SIGNING_KEY/SIGNING_PASSWORD (or signingKey/signingPassword) and re-stage" >&2
  exit 1
fi

# Every staged artifact must carry the version being released; a stale staging directory
# would otherwise silently ship the wrong bits. Derive version directories from where the
# .pom files actually are rather than assuming a fixed depth — the Gradle plugin marker
# publishes under group com.uxcam.kmp, so it sits one level deeper than the other modules.
stale="$(find "$STAGING/com/uxcam" -name '*.pom' -exec dirname {} \; | sort -u | while IFS= read -r dir; do
    [[ "$(basename "$dir")" == "$VERSION" ]] || echo "$dir"
  done)"
if [[ -n "$stale" ]]; then
  echo "error: $STAGING contains artifacts for a version other than $VERSION:" >&2
  echo "$stale" >&2
  echo "       clean the staging directory and re-stage" >&2
  exit 1
fi

mkdir -p "$BUNDLE_DIR"
rm -f "$BUNDLE"
( cd "$STAGING" && zip -qry "$BUNDLE" com )
echo "==> Bundle: $BUNDLE ($(wc -c < "$BUNDLE" | tr -d ' ') bytes, \
$(find "$STAGING/com/uxcam" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ') artifacts)"

if [[ "${UXCAM_MAVEN_CENTRAL_AUTO_PUBLISH:-false}" == "true" ]]; then
  PUBLISHING_TYPE=AUTOMATIC
else
  PUBLISHING_TYPE=USER_MANAGED
fi
echo "==> Uploading to Central Portal (publishingType=$PUBLISHING_TYPE)"

RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RESPONSE_FILE"' EXIT
CODE="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
  -X POST \
  -u "$USER:$PASS" \
  -F "bundle=@$BUNDLE" \
  -F "publishingType=$PUBLISHING_TYPE" \
  "https://central.sonatype.com/api/v1/publisher/upload")"

BODY="$(cat "$RESPONSE_FILE")"
if [[ "$CODE" != "201" && "$CODE" != "200" ]]; then
  echo "error: upload failed with HTTP $CODE: $BODY" >&2
  exit 1
fi

echo "==> Deployment ID: $BODY"
if [[ "$PUBLISHING_TYPE" == "AUTOMATIC" ]]; then
  echo "==> Auto-publish enabled — releases once validation passes."
else
  echo "==> Review and publish: https://central.sonatype.com/publishing/deployments"
fi
