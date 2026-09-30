#!/usr/bin/env python3
"""Validate HexPattern signatures without launching the game.

Reimplements HexPattern.fromAngles / tryAppendDir exactly, from:

  HexPattern.kt:18-42   tryAppendDir  -- the two restrictions
  HexPattern.kt:144-166 fromAngles     -- drives it, throws on failure
  HexDir.kt:5-26        six dirs, mod 6, asDelta()
  HexAngle.kt:3-7       six angles, mod 6

A signature that would throw "made the pattern invalid by looping back on
itself" at mod-load time is caught here instead. That throw aborts the whole
RegisterEvent dispatch, so one bad rune silently takes every other rune in the
mod down with it -- which is exactly how this was found.

Usage:
    python tools/check_patterns.py                # check the five assembly runes
    python tools/check_patterns.py qa w e d       # check arbitrary signatures
    python tools/check_patterns.py --collide      # + collision check vs the tree
    python tools/check_patterns.py --selftest    # trust check, see below
    python tools/check_patterns.py --gen P L N   # N candidate words, len L
    python tools/check_patterns.py --show qaqwawaw
"""
import re
import sys
from pathlib import Path

# HexAngle ordinals.
FORWARD, RIGHT, RIGHT_BACK, BACK, LEFT_BACK, LEFT = range(6)

# HexDir ordinals -- six of them, not eight. Getting this wrong (as an eight-dir
# guess does) silently changes every verdict below.
NE, E, SE, SW, W, NW = range(6)
DIR_DELTA = {
    NE: (1, -1), E: (1, 0), SE: (0, 1),
    SW: (-1, 1), W: (-1, 0), NW: (0, -1),
}
DIR_NAMES = {NE: "NORTH_EAST", E: "EAST", SE: "SOUTH_EAST",
             SW: "SOUTH_WEST", W: "WEST", NW: "NORTH_WEST"}

CHAR_TO_ANGLE = {
    'w': FORWARD, 'e': RIGHT, 'd': RIGHT_BACK,
    's': BACK, 'a': LEFT_BACK, 'q': LEFT,
}
ANGLE_CHARS = "wedsaq"

START_DIR = E  # HexDir.EAST, matching registerAssemblyRunes


def rotate(d, a):
    """HexDir.rotatedBy / HexAngle.rotatedBy -- both (ordinal + angle) mod 6."""
    return (d + a) % 6


def angle_from(new_dir, base):
    """`newDir - compass` == HexDir.angleFrom == (this - other) mod 6."""
    return (new_dir - base) % 6


def step(coord, d):
    dq, dr = DIR_DELTA[d]
    return (coord[0] + dq, coord[1] + dr)


def try_append_dir(angles, start_dir, new_dir):
    """Mirror of HexPattern.tryAppendDir. Appends on success, returns bool."""
    lines_seen = set()

    compass = start_dir
    cursor = (0, 0)
    for a in angles:
        lines_seen.add((cursor, compass))
        # "Line from here to there also blocks there to here"
        lines_seen.add((step(cursor, compass), rotate(compass, BACK)))
        cursor = step(cursor, compass)
        compass = rotate(compass, a)
    cursor = step(cursor, compass)

    if (cursor, new_dir) in lines_seen:
        return False
    if angle_from(new_dir, compass) == BACK:
        return False

    angles.append(angle_from(new_dir, compass))
    return True


def build(signature, start_dir=START_DIR):
    """Mirror of HexPattern.fromAngles. Returns (start_dir, angles)."""
    out = []
    compass = start_dir
    for idx, c in enumerate(signature):
        if c not in CHAR_TO_ANGLE:
            raise ValueError(f"Cannot match {c} at idx {idx} to a direction")
        compass = rotate(compass, CHAR_TO_ANGLE[c])
        if not try_append_dir(out, start_dir, compass):
            raise ValueError(
                f"Adding the angle {c} at index {idx} made the pattern "
                f"invalid by looping back on itself"
            )
    return start_dir, out


def trace(signature, start_dir=START_DIR):
    """Per-character walk, for working out *why* a word is rejected."""
    compass, cursor, angles = start_dir, (0, 0), []
    rows = []
    for idx, c in enumerate(signature):
        ndir = rotate(compass, CHAR_TO_ANGLE[c])
        ok = try_append_dir(angles, start_dir, ndir)
        rows.append((idx, c, DIR_NAMES[ndir], cursor, ok))
        if not ok:
            break
        cursor, compass = step(cursor, ndir), ndir
    return rows


def points(signature, start_dir=START_DIR):
    """Every vertex, and the direction of each outgoing segment."""
    _, angles = build(signature, start_dir)
    compass, cursor = start_dir, (0, 0)
    pts, dirs = [cursor], []
    for a in angles:
        dirs.append(compass)
        cursor = step(cursor, compass)
        pts.append(cursor)
        compass = rotate(compass, a)
    dirs.append(compass)
    pts.append(step(cursor, compass))
    return pts, dirs


def draw(signature, start_dir=START_DIR):
    """ASCII render of the actual segments, so crossings are visible."""
    pts, _ = points(signature, start_dir)

    # Doubled horizontal coords keep the 0.866 vertical rows on grid cells.
    def px(c):
        return 2 * c[0] + c[1], 2 * round(0.866 * c[1])

    pix = [px(p) for p in pts]
    ox = min(p[0] for p in pix)
    oy = min(p[1] for p in pix)
    w = max(p[0] for p in pix) - ox + 1
    h = max(p[1] for p in pix) - oy + 1
    grid = [[" " for _ in range(w)] for _ in range(h)]
    for i in range(len(pix) - 1):
        (x0, y0), (x1, y1) = pix[i], pix[i + 1]
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for s in range(n + 1):
            x = int(round(x0 + (x1 - x0) * s / n))
            y = int(round(y0 + (y1 - y0) * s / n))
            grid[y - oy][x - ox] = "o" if s in (0, n) else "-"
    return "\n".join("".join(r) for r in grid)


def readability(word):
    """Score for how easy a rune is to trace and tell from its neighbours.

    Two things matter in practice: it must not collapse into a tight blob, and
    it needs a couple of real corners so the eye can follow it. The min of the
    two extents is the one that matters -- a word can be wide and still squat.
    """
    pts, dirs = points(word, START_DIR)
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    extent = min(max(xs) - min(xs), max(ys) - min(ys))
    turns = sum(1 for i in range(1, len(dirs) - 1) if dirs[i] != dirs[i - 1])
    return (extent * 10 + turns, turns, word)


def existing_signatures(root):
    """Every fromAngles("...") literal in the source tree, keyed by word+dir."""
    found = {}
    pat = re.compile(r'fromAngles\(\s*"([wesdaq]+)"\s*,\s*HexDir\.(\w+)')
    for ext in ("*.java", "*.kt"):
        for path in root.rglob(ext):
            try:
                text = path.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            for m in pat.finditer(text):
                found.setdefault((m.group(1), m.group(2)), []).append(path.name)
    return found


def default_root():
    return Path(__file__).resolve().parent.parent / "src" / "meowhex"


# The five sequenced-assembly runes, as written in HexJSInitializer.
#
# The first four were originally qaqwawa{q,a,w,q} -- sharing a 7-char prefix
# and differing only in the last letter. That was doubly wrong: tryAppendDir
# rejects all four (the pen returns to the origin on char 6, so the seventh
# line would retrace the first), and even had they drawn, the four would have
# been near-identical shapes sitting next to each other in the book.
#
# These were picked from the 302827 valid 8-char words by corner count and
# pairwise vertex distance, not by eye. Regenerate with --gen if you ever need
# replacements, but keep the four activator runes visually distinct.
ASSEMBLY_RUNES = {
    "merge_entities": "qeqqeqew",
    "absorb_gifts":   "adadqqew",
    "purify_essence": "adaeeaew",
    "draw_sacrifice": "qqeqeeqw",
    "infuse_aether":  "wqqqqqadqw",
}


def selftest(root):
    """Trust check.

    The simulator is only worth anything if it accepts every pattern that
    already works in game. Any failure here is a bug in the simulator, not in
    the mod -- except for our own new signatures, which are reported
    separately since they are the thing under test.
    """
    found = existing_signatures(root)
    good = bad = skipped = 0
    ours = set(ASSEMBLY_RUNES.values())
    for (sig, dir_name), files in sorted(found.items()):
        sd = next((d for d, n in DIR_NAMES.items() if n == dir_name), None)
        if sd is None:
            continue
        try:
            build(sig, sd)
            good += 1
        except ValueError as e:
            if sig in ours:
                skipped += 1  # our own, the thing under test -- not a sim bug
                continue
            bad += 1
            print(f"SELFTEST FAIL {sig} {dir_name} in {sorted(set(files))} -> {e}")
    print(f"selftest: {good}/{good + bad} pre-existing patterns accepted, "
          f"{skipped} of our own rejected as expected")
    return bad == 0 and good > 0


def valid_extensions(prefix, length, taken):
    """Every way to finish `prefix` to `length` that tryAppendDir accepts.

    Brute force over the 6-letter alphabet; 6**4 for an 8-char word is 1296,
    and the prefix check prunes long before that.
    """
    out, stack = [], [prefix]
    while stack:
        cur = stack.pop()
        if len(cur) == length:
            if cur not in taken:
                out.append(cur)
            continue
        for c in ANGLE_CHARS:
            nxt = cur + c
            try:
                build(nxt, START_DIR)
            except ValueError:
                continue
            stack.append(nxt)
    return sorted(out)


def gen(prefix, length, count, taken):
    cands = valid_extensions(prefix, length, taken)
    cands.sort(key=readability, reverse=True)
    return cands[:count]


def main():
    args = sys.argv[1:]

    if args and args[0] == "--selftest":
        return 0 if selftest(default_root()) else 1

    if args and args[0] == "--show":
        for sig in args[1:]:
            print(f"=== {sig}")
            try:
                print(draw(sig))
            except ValueError as e:
                print(f"invalid: {e}")
                for idx, c, d, cur, ok in trace(sig):
                    print(f"  idx {idx} char {c} dir={d:12s} cursor={str(cur):10s} ok={ok}")
            print()
        return 0

    if args and args[0] == "--gen":
        prefix, length, count = args[1], int(args[2]), int(args[3])
        taken = {sig for (sig, d) in existing_signatures(default_root())
                 if d == DIR_NAMES[START_DIR]}
        for w in gen(prefix, length, count, taken):
            print(f"  {w}  turns={readability(w)[1]}")
        return 0

    collide = False
    if not args or args[0] == "--all":
        sigs = ASSEMBLY_RUNES
    elif args[0] == "--collide":
        sigs, collide = ASSEMBLY_RUNES, True
    else:
        sigs = {f"arg{i}": a for i, a in enumerate(args)}

    bad = 0
    for name, sig in sigs.items():
        try:
            build(sig, START_DIR)
            print(f"OK    {name:20s} {sig}")
        except ValueError as e:
            bad += 1
            print(f"FAIL  {name:20s} {sig}  -> {e}")

    if collide:
        existing = existing_signatures(default_root())
        print(f"\ncollisions against {len(existing)} existing fromAngles in the tree:")
        for name, sig in sigs.items():
            hit = existing.get((sig, DIR_NAMES[START_DIR]))
            if hit:
                # Ourselves is fine; that is just where they are defined.
                hit = [f for f in hit if f != "HexJSInitializer.java"] or hit
                if hit and hit != ["HexJSInitializer.java"]:
                    bad += 1
                    print(f"  DUP  {name:20s} {sig} -> {', '.join(sorted(set(hit)))}")
                else:
                    print(f"  own  {name:20s} {sig} (defined here)")
            else:
                print(f"  free {name:20s} {sig}")

    if bad:
        print(f"\n{bad} problem(s).")
        return 1
    print("\nall signatures valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())
