#!/usr/bin/env python3
"""Resolve a single explicit APK path while checking package/native versions."""
import argparse
import json
from pathlib import Path
import re


def artifact_identity(package, gradle, mode, variant):
    if mode not in ('github', 'nongithub') or variant not in ('debug', 'release'):
        raise ValueError('Unsupported build mode or variant')
    names = re.findall(r'^\s*versionName\s+"([^"\n]+)"\s*$', gradle, re.MULTILINE)
    codes = re.findall(r'^\s*versionCode\s+(\d+)\s*$', gradle, re.MULTILINE)
    if len(names) != 1 or len(codes) != 1:
        raise ValueError('Expected one literal Android versionName/versionCode')
    version = package.get('version')
    if not isinstance(version, str) or not re.fullmatch(r'\d+\.\d+\.\d+', version) or names[0] != version:
        raise ValueError('Package and Android versions must be matching numeric semantic versions')
    code = int(codes[0])
    if code <= 0:
        raise ValueError('Android versionCode must be positive')
    suffix = '-unsigned' if variant == 'release' else ''
    return f'muajjin-{version}-{code}-{mode}-{variant}{suffix}.apk'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mode', required=True, choices=('github', 'nongithub'))
    parser.add_argument('--variant', required=True, choices=('debug', 'release'))
    parser.add_argument('--path', action='store_true', help='Print repo-relative artifact path rather than filename')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    name = artifact_identity(json.loads((root / 'package.json').read_text(encoding='utf-8')), (root / 'android/app/build.gradle').read_text(encoding='utf-8'), args.mode, args.variant)
    print(f'android/app/build/outputs/apk/{args.variant}/{name}' if args.path else name)


if __name__ == '__main__':
    main()
