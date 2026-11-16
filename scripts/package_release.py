#!/usr/bin/env python3
"""Build the versioned public update manifest only for stable-key signed builds."""
import argparse
import hashlib
import json
import re
from pathlib import Path


def package(apk: Path, config: Path, output: Path, stable: bool) -> dict:
    text = config.read_text()
    version = re.search(r'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"', text).group(1)
    code = int(re.search(r'versionCode\s*=\s*(\d+)', text).group(1))
    if code <= 0:
        raise ValueError('versionCode must be positive')
    output.mkdir(parents=True, exist_ok=True)
    filename = f'RS-Tube-v{version}{"" if stable else "-test"}.apk'
    data = apk.read_bytes()
    (output / filename).write_bytes(data)
    metadata = dict(versionCode=code, versionName=version, size=len(data),
        sha256=hashlib.sha256(data).hexdigest(),
        url=f'https://github.com/rajibsarder72942/RS-Tube/releases/download/v{version}/{filename}')
    if stable:
        (output / 'update.json').write_text(json.dumps(metadata, indent=2) + '\n')
    return metadata


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--stable', action='store_true')
    parser.add_argument('--output', type=Path, default=Path('dist'))
    args = parser.parse_args()
    metadata = package(args.apk, Path('app/build.gradle.kts'), args.output, args.stable)
    print(f'Packaged RS Tube {metadata["versionName"]} (versionCode {metadata["versionCode"]})')
