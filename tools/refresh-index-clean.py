"""Build a clean packwiz index from a git-clean tree.

packwiz does not read .gitignore, so running `packwiz refresh` in a working
tree that has gradle build/ run/ .gradle/ output indexed ~9k dev files.
CI never sees them (fresh checkout = tracked files only), so the committed
index.toml was always clean. This script reproduces that: it exports HEAD's
tracked files to a temp dir, refreshes the index there, and copies the
result back.
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
    tracked = sh('git', 'ls-files', '-z', cwd=ROOT).split('\0')
    tracked = [f for f in tracked if f]
    print('tracked files: %d' % len(tracked))

    tmp = tempfile.mkdtemp(prefix='aqua-clean-')
    try:
        for rel in tracked:
            src = os.path.join(ROOT, rel)
            if not os.path.isfile(src):
                # tracked but deleted in the worktree; skip, git commit will drop it
                print('  missing, skipped: %s' % rel)
                continue
            dst = os.path.join(tmp, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)

        sh('packwiz', 'refresh', cwd=tmp)

        shutil.copy2(os.path.join(tmp, 'index.toml'), os.path.join(ROOT, 'index.toml'))
        print('index.toml refreshed in clean tree -> copied back')
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    main()