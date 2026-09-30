"""Pick free hexcasting signatures for the new meowhex runes.

Two separate notions of "free" are checked:

1. Signature-free  - the angles string is not already used. This is the hard
   requirement: PatternRegistryManifest indexes actions by anglesSignature()
   alone (the start direction is NOT part of the key), so a repeat silently
   steals the existing rune's match.
2. Shape-free      - the drawn glyph, up to rotation, is not already used.
   Not required by the engine, but without it players cannot tell the new
   runes apart from the old ones, and the pack already teaches ~190.

Usage:
    python tools/check_sigs.py --lengths 3 4 5 --count 40
    python tools/check_sigs.py --min-turns 1 --lengths 1 2 --count 30
"""
import argparse
import itertools
import re
from pathlib import Path

ROOT = Path(r"D:\games\minecraft\Aqua_SMP_modpack\src\meowhex")
SRC = ROOT / "src" / "main" / "java"

# Built programmatically in the source (spiral generators), so a regex scan
# cannot see them. Their shapes are long spirals; short candidates cannot
# collide with them, but keep this list as documentation.
DYNAMIC = ["OpHerasWrath.buildSpiralPattern", "OpDeadeye.buildDeadeyePattern"]

ANG = "wedsaq"
DIRS = [(1, 0), (1, -1), (0, -1), (-1, 0), (-1, 1), (0, 1)]


def occupied():
    """Map angles-signature -> list of source locations claiming it."""
    out = {}
    for path in SRC.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r'fromAngles\("([wedsqa]+)"', text):
            line = text[: m.start()].count("\n") + 1
            out.setdefault(m.group(1), []).append(f"{path.name}:{line}")
    return out


def walk(sig, start=0):
    """Absolute direction per stroke, plus the visited point set."""
    compass = start
    pts = {(0, 0)}
    dirs = []
    x = y = 0
    for ch in sig:
        compass = (compass + ANG.index(ch)) % 6
        dirs.append(compass)
        dx, dy = DIRS[compass]
        x, y = x + dx, y + dy
        pts.add((x, y))
    return dirs, pts


def is_valid(sig):
    """Mirror HexPattern.tryAppendDir: no reused undirected edge, no backtrack."""
    seen = set()
    dirs, _ = walk(sig)
    x = y = 0
    for d in dirs:
        dx, dy = DIRS[d]
        nx, ny = x + dx, y + dy
        a = (x, y, d)
        b = (nx, ny, (d + 3) % 6)
        if a in seen or b in seen:
            return False
        seen.add(a)
        seen.add(b)
        x, y = nx, ny
    return all(b != (a + 3) % 6 for a, b in zip(dirs, dirs[1:]))


def canonical(sig):
    """Smallest point set over all 6 rotations - a rotation-invariant glyph id."""
    return min(tuple(sorted(walk(sig, rot)[1])) for rot in range(6))


def bbox(sig):
    _, pts = walk(sig)
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    return (max(xs) - min(xs), max(ys) - min(ys))


def propose(taken, taken_shapes, count, lengths, min_turns):
    free = []
    for n in lengths:
        for combo in itertools.product("wedsqa", repeat=n):
            sig = "".join(combo)
            if sig in taken or not is_valid(sig):
                continue
            if len(set(sig)) < min_turns:
                continue
            if canonical(sig) in taken_shapes:
                continue
            free.append(sig)
            if len(free) >= count:
                return free
    return free


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lengths", type=int, nargs="+", default=[3, 4, 5, 6])
    ap.add_argument("--count", type=int, default=40)
    ap.add_argument("--min-turns", type=int, default=2,
                    help="min distinct angle chars; 1 allows straight lines")
    ap.add_argument("--max-run", type=int, default=2,
                    help="reject if any single angle char repeats longer")
    a = ap.parse_args()

    taken = occupied()
    taken_shapes = {canonical(s) for s in taken}
    print(f"occupied signatures: {len(taken)}")
    print(f"occupied shapes (rotation-invariant): {len(taken_shapes)}")
    print(f"dynamically built, shape unknown: {DYNAMIC}")

    free = propose(taken, taken_shapes, a.count, a.lengths, a.min_turns)
    print(f"\nfree signature+shape candidates "
          f"(lengths {a.lengths}, min-turns {a.min_turns}): {len(free)}")
    for s in free:
        w, h = bbox(s)
        print(f"  {s:<8} len={len(s)} dirs={len(set(walk(s)[0]))} bbox={w}x{h}")


if __name__ == "__main__":
    main()
