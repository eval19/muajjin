#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == --help || "${1:-}" == -h ]]; then
  cat <<'EOF'
Usage: bun run reproducible:verify --apk SIGNED.apk --local-apk UNSIGNED.apk --metadata BUILD-METADATA.txt --mode github|nongithub --time EPOCH --apksigner SDK/build-tools/36.0.0/apksigner --certificate TRUSTED_SHA256

READ ONLY: no dependency install, rebuild, Capacitor sync, clean, extraction,
signing, publishing or network access. First build the unsigned local artifact
with reproducible:build using the actual mode/epoch and the intended source.
Use its build-metadata.txt here; --time is not the release publication date.
Requires Python 3 (PYTHON override on Windows) and the explicit SDK apksigner.

The signed APK must have a valid Android signature and exactly one signer with
the independently trusted certificate SHA-256 fingerprint. Verification does
not trust a fingerprint merely printed from the APK being checked.

Compares names, sizes and SHA-256 of ALL uncompressed ZIP payload entries.
Only standard top-level v1 JAR signature metadata is excluded, and only after
apksigner verifies v1. APK v2/v3 signing blocks lie outside ZIP entries. ZIP
order/compression/timestamps and whole-file bit identity are not claimed.
No extraction/temporary files exist, so no cleanup side effects are needed.
EOF
  exit 0
fi
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python="${PYTHON:-python3}"
command -v "$python" >/dev/null || { printf 'Python 3 required; set PYTHON to its executable on Windows.\n' >&2; exit 1; }
exec "$python" -B "$root/scripts/apk-payload.py" "$@"
