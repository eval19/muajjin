#!/usr/bin/env python3
"""Read-only APK signature validation and unsigned ZIP payload comparison."""
import argparse
import hashlib
import re
import stat
import subprocess
from pathlib import Path
import sys
import zipfile


def jar_signature_entry(name):
    # Only the top-level JAR signing namespace defined by the JAR format.
    upper = name.upper()
    if not upper.startswith('META-INF/') or '/' in upper[len('META-INF/'):]:
        return False
    leaf = upper[len('META-INF/'):]
    return leaf == 'MANIFEST.MF' or leaf.endswith(('.SF', '.RSA', '.DSA', '.EC')) or leaf.startswith('SIG-')


def payload_manifest(path, verified_v1=False):
    entries = {}
    seen = set()
    with zipfile.ZipFile(path) as archive:
        for entry in archive.infolist():
            # Python normalizes backslashes on Windows and truncates NUL in
            # filename; validate the original archive name before either loss.
            name = entry.orig_filename
            if name in seen:
                raise ValueError('Duplicate ZIP entry: ' + name)
            seen.add(name)
            parts = name.rstrip('/').split('/')
            if name.startswith('/') or '\\' in name or '\x00' in name or any(part in ('', '.', '..') for part in parts):
                raise ValueError('Unsafe ZIP entry name: ' + name)
            if entry.flag_bits & 1 or stat.S_ISLNK(entry.external_attr >> 16):
                raise ValueError('Encrypted or symlink ZIP entry: ' + name)
            if verified_v1 and jar_signature_entry(name):
                continue
            digest = hashlib.sha256()
            with archive.open(entry) as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(chunk)
            entries[name] = {'size': entry.file_size, 'sha256': digest.hexdigest()}
    if not entries:
        raise ValueError('APK payload is empty')
    return entries


def require_unsigned(path):
    with zipfile.ZipFile(path) as archive:
        if any(jar_signature_entry(entry.filename) for entry in archive.infolist()):
            raise ValueError('Local candidate must be unsigned; JAR signing metadata found')
        # APK v2/v3 blocks are outside ZIP entries, immediately before the central
        # directory. Never infer unsignedness just from absence of META-INF.
        offset = archive.start_dir
    with open(path, 'rb') as stream:
        if offset >= 24:
            stream.seek(offset - 16)
            if stream.read(16) == b'APK Sig Block 42':
                raise ValueError('Local candidate must be unsigned; APK Signing Block found')


def signature_details(output, expected_certificate):
    expected = expected_certificate.replace(':', '').lower()
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        raise ValueError('Expected certificate must be a SHA-256 hexadecimal fingerprint')
    certificates = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$', output, re.MULTILINE)
    if len(certificates) != 1:
        raise ValueError('Exactly one verified signer is required; found ' + str(len(certificates)))
    if certificates[0].lower() != expected:
        raise ValueError('Verified signer certificate does not match the independently trusted fingerprint')
    return bool(re.search(r'^Verified using v1 scheme \(JAR signing\): true\s*$', output, re.MULTILINE))


def validate_build_metadata(path, mode, epoch, candidate):
    lines = path.read_text(encoding='utf-8').splitlines()
    for key, expected in (('BUILD_MODE', mode), ('SOURCE_DATE_EPOCH', epoch), ('VARIANT', 'release-unsigned')):
        values = [line.split('=', 1)[1] for line in lines if line.startswith(key + '=')]
        if values != [expected]:
            raise ValueError('Build metadata must match declared ' + key)
    positions = [index for index, line in enumerate(lines) if line == 'APK_SHA256']
    if len(positions) != 1 or positions[0] + 1 >= len(lines):
        raise ValueError('Build metadata must contain exactly one APK_SHA256')
    recorded = lines[positions[0] + 1].split()[0]
    digest = hashlib.sha256()
    with candidate.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    if digest.hexdigest() != recorded:
        raise ValueError('Local APK does not match its recorded build checksum')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', required=True, type=Path, help='Reference APK; never modified')
    parser.add_argument('--local-apk', required=True, type=Path, help='Previously built unsigned release APK; never rebuilt')
    parser.add_argument('--metadata', required=True, type=Path, help='Local build-metadata.txt with the candidate checksum')
    parser.add_argument('--mode', required=True, choices=('github', 'nongithub'))
    parser.add_argument('--time', required=True, help='Actual SOURCE_DATE_EPOCH from the local build')
    parser.add_argument('--apksigner', help='Android SDK build-tools apksigner executable (36.0.0 recommended)')
    signature_scope = parser.add_mutually_exclusive_group(required=True)
    signature_scope.add_argument('--certificate', help='Independently trusted signer certificate SHA-256; not a value taken on trust from this APK')
    signature_scope.add_argument('--unsigned-reference', action='store_true', help='Compare two unsigned artifacts; make NO signature-validity claim')
    args = parser.parse_args()
    if not re.fullmatch(r'0|[1-9][0-9]*', args.time):
        raise ValueError('--time must be a canonical nonnegative epoch in seconds')
    validate_build_metadata(args.metadata, args.mode, args.time, args.local_apk)
    print('Recorded local build mode/epoch/checksum match declared inputs: ' + args.mode + '/' + args.time)
    require_unsigned(args.local_apk)
    verified_v1 = False
    if args.unsigned_reference:
        require_unsigned(args.apk)
        print('Signature scope: NONE. Both supplied artifacts are unsigned; no authenticity/signature-validity claim.')
    else:
        if not args.apksigner:
            raise ValueError('--apksigner is required for a signed reference')
        # apksigner validates v1/v2/v3 and platform acceptance. ZIP parsing alone
        # cannot authenticate an APK Signing Block or a JAR manifest.
        result = subprocess.run([args.apksigner, 'verify', '--verbose', '--print-certs', str(args.apk.resolve())], capture_output=True, text=True, check=False)
        if result.returncode != 0:
            raise ValueError('APK signature verification failed:\n' + result.stdout + result.stderr)
        verified_v1 = signature_details(result.stdout, args.certificate)
        if not verified_v1:
            with zipfile.ZipFile(args.apk) as archive:
                has_v1_metadata = any(jar_signature_entry(entry.filename) for entry in archive.infolist())
            if has_v1_metadata:
                # minSDK24+ verification may use only v2/v3. API 23 forces a
                # separate v1 check without replacing normal platform validation.
                v1_result = subprocess.run([args.apksigner, 'verify', '--verbose', '--print-certs', '--min-sdk-version', '23', '--max-sdk-version', '23', str(args.apk.resolve())], capture_output=True, text=True, check=False)
                if v1_result.returncode != 0:
                    raise ValueError('APK v1 signature verification failed:\n' + v1_result.stdout + v1_result.stderr)
                verified_v1 = signature_details(v1_result.stdout, args.certificate)
                if not verified_v1:
                    raise ValueError('JAR signing metadata requires a verified v1 signature')
        print(result.stdout.rstrip())
        print('Signature validity and expected signer fingerprint: VERIFIED (not a certificate-chain/PKI claim).')
    reference = payload_manifest(args.apk, verified_v1=verified_v1)
    candidate = payload_manifest(args.local_apk)
    missing = sorted(reference.keys() - candidate.keys())
    extra = sorted(candidate.keys() - reference.keys())
    changed = sorted(name for name in reference.keys() & candidate.keys() if reference[name] != candidate[name])
    if missing or extra or changed:
        raise ValueError('Unsigned ZIP payload differs.\nMissing locally: ' + repr(missing) + '\nExtra locally: ' + repr(extra) + '\nDifferent bytes: ' + repr(changed))
    print('Unsigned ZIP payload: MATCH (' + str(len(reference)) + ' entries, names/sizes/uncompressed SHA-256).')
    print('Excluded only top-level JAR signing metadata: ' + ('yes, after verified v1 signature' if verified_v1 else 'no; v1 not present'))
    print('APK Signing Blocks are outside ZIP entries. ZIP order/compression/timestamps, signing blocks and whole-file identity are NOT compared.')
    print('This proves payload equivalence to the supplied local artifact, not that its source/toolchain was independently audited.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile, RuntimeError) as error:
        print('Verification FAILED: ' + str(error), file=sys.stderr)
        sys.exit(1)
