#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: bun run reproducible:build --mode github|nongithub (--time EPOCH | --tag TAG) [--install] [--output NEW_DIRECTORY]

Build an UNSIGNED release APK. Requires Bun 1.2.2, Node >=22.12,
JDK 21, Python 3 (PYTHON override), SDK platform/build-tools 36/36.0.0
and the checked-in Gradle 8.14.3 wrapper. No signing or publishing occurs.

SIDE EFFECTS: --install explicitly permits a frozen dependency install;
otherwise existing local dependencies are required. Every build replaces dist,
syncs Capacitor's generated Android assets/config, runs Gradle clean/release
assembly (Gradle may populate/download caches), and creates a NEW output folder.
It does not restore these generated files. Outputs/metadata survive failure;
no temporary extraction files are created. Run in a disposable clean checkout
for controlled comparisons. Existing output folders are never overwritten.

--mode is required; no implicit GitHub distribution.
--time is the actual build epoch in seconds, not a release publication date.
--tag uses that local tag's commit epoch and requires matching HEAD.
Environment SOURCE_DATE_EPOCH alone is deliberately not an input.
Default output: reproducible-output/<mode>-<epoch>
EOF
}
fail() { printf 'Error: %s\n' "$*" >&2; exit 1; }
mode='' epoch='' tag='' output='' install=false
while (($#)); do
  case "$1" in
    --mode|-m|--time|-t|--tag|-g|--output)
      (($# >= 2)) && [[ "$2" != --* ]] || fail "Missing value for $1"
      case "$1" in
        --mode|-m) [[ -z "$mode" ]] || fail 'Duplicate mode'; mode="$2" ;;
        --time|-t) [[ -z "$epoch" ]] || fail 'Duplicate time'; epoch="$2" ;;
        --tag|-g) [[ -z "$tag" ]] || fail 'Duplicate tag'; tag="$2" ;;
        --output) [[ -z "$output" ]] || fail 'Duplicate output'; output="$2" ;;
      esac
      shift 2 ;;
    --install) install=true; shift ;;
    --help|-h) usage; exit 0 ;;
    *) fail "Unknown option: $1" ;;
  esac
done
[[ "$mode" == github || "$mode" == nongithub ]] || fail '--mode github|nongithub is required'
[[ -z "$tag" || -z "$epoch" ]] || fail 'Choose --time OR --tag, not both'
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
if [[ -n "$tag" ]]; then
  commit="$(git rev-parse --verify "refs/tags/$tag^{commit}")" || fail 'Local tag not found'
  [[ "$commit" == "$(git rev-parse HEAD)" ]] || fail 'HEAD does not match supplied tag'
  epoch="$(git show -s --format=%ct "$commit")"
fi
[[ "$epoch" =~ ^[0-9]+$ ]] || fail 'An explicit numeric --time or valid --tag is required'
for command in bun node java git; do command -v "$command" >/dev/null || fail "Missing tool: $command"; done
python="${PYTHON:-python3}"
command -v "$python" >/dev/null || fail 'Python 3 is required; set PYTHON to its executable on Windows'
[[ "$(bun --version)" == 1.2.2 ]] || fail 'Bun must match pinned 1.2.2'
node -e 'const [major,minor]=process.versions.node.split(".").map(Number); if(major<22||(major===22&&minor<12)) throw Error("Node >=22.12 required")'
epoch="$(node -e 'const n=Number(process.argv[1]); if(!Number.isSafeInteger(n)||n<0) throw Error("Epoch must be a nonnegative safe integer"); console.log(n)' "$epoch")"
java_version="$(java -version 2>&1)"
[[ "$java_version" == *'version "21.'* || "$java_version" == *'version "21"'* ]] || fail 'JDK 21 is required'
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$sdk" && -d "$sdk/platforms/android-36" && -d "$sdk/build-tools/36.0.0" ]] || fail 'SDK platform 36 and build-tools 36.0.0 are required'
if "$install"; then bun install --frozen-lockfile; fi
[[ -f node_modules/@capacitor/cli/bin/capacitor && -f node_modules/vite/package.json ]] || fail 'Local dependencies missing; use --install explicitly'
export SOURCE_DATE_EPOCH="$epoch"
output="${output:-reproducible-output/$mode-$epoch}"
[[ ! -e "$output" ]] || fail "Output already exists: $output"
mkdir -p "$(dirname "$output")"
mkdir "$output"
output="$(cd "$output" && pwd)"
apk="$("$python" -B scripts/build-identity.py --mode "$mode" --variant release --path)"
metadata="$output/build-metadata.txt"
{
  printf 'SOURCE_DATE_EPOCH=%s\nBUILD_MODE=%s\nVARIANT=release-unsigned\nGIT_TAG=%s\nGIT_COMMIT=%s\n' "$epoch" "$mode" "$tag" "$(git rev-parse HEAD)"
  printf 'TRACKED_SOURCE_DELTA_SHA256='; git diff --binary HEAD | sha256sum
  printf 'TRACKED_SOURCE_STATUS\n'; git status --porcelain --untracked-files=no
  printf 'UNTRACKED_SOURCE_STATUS (review locally before making a source claim)\n'; git ls-files --others --exclude-standard
  printf 'UNTRACKED_SOURCE_SHA256\n'; git ls-files --others --exclude-standard -z | xargs -0 -r sha256sum
  printf 'BUN='; bun --version
  printf 'NODE='; node --version
  printf 'PYTHON='; "$python" --version
  printf '%s\n' "$java_version"
  printf 'LOCAL_PACKAGE_VERSIONS\n'
  node -e 'for(const p of ["@capacitor/cli","@capacitor/core","@capacitor/android","vite","typescript"]) console.log(p+"="+require("./node_modules/"+p+"/package.json").version)'
  printf 'LOCKFILE_SHA256='; sha256sum bun.lock
  printf 'ANDROID_SDK=%s\n' "$sdk"
  cat "$sdk/platforms/android-36/source.properties" "$sdk/build-tools/36.0.0/source.properties"
  java -jar "$sdk/build-tools/36.0.0/lib/apksigner.jar" version
  aapt2="$sdk/build-tools/36.0.0/aapt2"
  [[ -f "$aapt2" ]] || aapt2="$aapt2.exe"
  "$aapt2" version
  bash android/gradlew -p android --version
} > "$metadata"
printf 'Build replaces dist/Capacitor generated assets and cleans Android outputs. Mode=%s Epoch=%s\n' "$mode" "$epoch"
if [[ "$mode" == github ]]; then bun run build:github; else bun run build; fi
bun run sync:android
bash android/gradlew -p android clean assembleRelease -PsourceDateEpoch="$epoch" -PbuildMode="$mode" -PsignRelease=false --stacktrace
[[ -f "$apk" ]] || fail "Expected explicit artifact missing: $apk"
cp "$apk" "$output/$(basename "$apk")"
{
  printf '\nAPK_SHA256\n'; sha256sum "$output/$(basename "$apk")"
  printf 'APK_SHA512\n'; sha512sum "$output/$(basename "$apk")"
  printf 'WEB_PAYLOAD_SHA256\n'; find dist -type f -print0 | sort -z | xargs -0 sha256sum
} >> "$metadata"
printf 'Unsigned artifact: %s/%s\nBuild inputs and checksums: %s\nNo signature validity or reproducibility comparison claimed by this build command.\n' "$output" "$(basename "$apk")" "$metadata"
