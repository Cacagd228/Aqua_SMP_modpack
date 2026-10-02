"""Export the Modrinth pack from a git-clean tree.

See refresh-index-clean.py for why this cannot run in the working tree:
packwiz does not read .gitignore, so `mr export` would inline gradle build/
run/.gradle output into overrides/ (~8k files, +400 MB). CI exports from a
fresh checkout, so the published .mrpack never had those. This reproduces it.

The temp tree also gets an explicit `packwiz refresh` first: `mr export`
re-reads the working tree rather than trusting the committed index, so
without it the export picks up whatever is on disk in the temp dir.

Usage: python tools/export-mrpack-clean.py <output.mrpack>
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def sh(*args, cwd=None):
    r = subprocess.run(args, cwd=cwd, capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit('FAILED %s\n%s\n%s' % (args, r.stdout, r.stderr))
    return r.stdout


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'AquaSMP.mrpack')

    tracked = [f for f in sh('git', 'ls-files', '-z', cwd=ROOT).split('\0') if f]
    print('tracked files: %d' % len(tracked))

    tmp = tempfile.mkdtemp(prefix='aqua-mrpack-')
    try:
        for rel in tracked:
            src = os.path.join(ROOT, rel)
            if not os.path.isfile(src):
                print('  missing, skipped: %s' % rel)
                continue
            dst = os.path.join(tmp, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)

        sh('packwiz', 'refresh', cwd=tmp)
        target = os.path.join(tmp, os.path.basename(out))
        sh('packwiz', 'mr', 'export', '-o', os.path.basename(out), cwd=tmp)

        shutil.copy2(target, out)
        size = os.path.getsize(out)
        print('exported %s (%.1f MB)' % (os.path.basename(out), size / 1048576))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    main()
