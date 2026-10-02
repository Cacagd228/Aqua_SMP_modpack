"""Build the PrismLauncher import zip the way .github/workflows/release.yml does.

The workflow stages prism/mmc-pack.json + icon.png + mods/config/kubejs/
defaultconfigs under a minecraft/ dir, drops the .pw.toml metafiles (Prism
would otherwise try to fetch them) and zips the lot. Only the tracked jars
are present locally, so `tools/fetch-mods.py` runs first to fill in the
rest from the pins.

Usage:  python tools/build-prism-pack.py <version-tag>
"""
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STAGE = 'prism-stage'
# Oldest timestamp the zip format can store.
EPOCH = (1980, 1, 1, 0, 0, 0)


def main():
    tag = sys.argv[1] if len(sys.argv) > 1 else None
    if not tag:
        sys.exit(__doc__)
    out = os.path.join(ROOT, 'AquaSMP-%s-prism.zip' % tag)

    subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'fetch-mods.py')],
                   cwd=ROOT, check=True)

    if os.path.isdir(STAGE):
        shutil.rmtree(STAGE)
    mc = os.path.join(STAGE, 'minecraft')
    os.makedirs(mc)
    shutil.copy2(os.path.join(ROOT, 'prism', 'mmc-pack.json'),
                 os.path.join(STAGE, 'mmc-pack.json'))
    shutil.copy2(os.path.join(ROOT, 'icon.png'),
                 os.path.join(mc, 'icon.png'))
    for d in ('mods', 'config', 'kubejs', 'defaultconfigs'):
        shutil.copytree(os.path.join(ROOT, d), os.path.join(mc, d))
        for meta in os.listdir(os.path.join(mc, d)):
            if meta.endswith('.pw.toml'):
                os.remove(os.path.join(mc, d, meta))

    if os.path.isfile(out):
        os.remove(out)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        for base, _dirs, files in os.walk(STAGE):
            for f in files:
                full = os.path.join(base, f)
                arc = os.path.relpath(full, STAGE).replace(os.sep, '/')
                # Some checked-in config files carry pre-1980 mtimes, which the
                # zip format cannot represent. Clamp instead of dropping them.
                info = zipfile.ZipInfo(arc, date_time=EPOCH)
                mode = os.stat(full).st_mode
                info.external_attr = (mode & 0xFFFF) << 16
                info.compress_type = zipfile.ZIP_DEFLATED
                with open(full, 'rb') as fh:
                    z.writestr(info, fh.read())

    shutil.rmtree(STAGE, ignore_errors=True)
    print('%s: %.1f MB' % (os.path.basename(out),
                           os.path.getsize(out) / 1e6))


if __name__ == '__main__':
    main()
