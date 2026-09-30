"""Check the upstream MoreIotas signatures against the ones meowhex already owns.

MoreIotas 0.1.2 targets Hexcasting 1.20.1; meowhex is a 1.21.1 fork that has since
added ~190 of its own runes (the meowhex: namespace spells plus retunes). Any
upstream signature that is already taken here would silently steal the existing
rune's match, because PatternRegistryManifest keys actions by anglesSignature()
alone -- the start direction is not part of the key, and the arrow of time does
not make a "CW" twin safe.

Run:  python tools/check_upstream.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from check_sigs import canonical, occupied  # noqa: E402

# (registry id, angles signature) copied from MoreIotas MoreIotasActions.kt.
UPSTREAM = [
    # -- strings ------------------------------------------------------------
    ("string/empty", "awdwa"),
    ("string/space", "awdwaaww"),
    ("string/comma", "qa"),
    ("string/newline", "waawaw"),
    ("string/split", "aqwaqa"),
    ("string/parse", "aqwaq"),
    ("string/case", "dwwdwwdwdd"),
    ("string/iota", "wawqwawaw"),
    ("string/action", "wdwewdwdw"),
    ("string/name/get", "deqqeddqwqqqwq"),
    ("string/name/set", "aqeeqaaeweeewe"),
    ("string/block/get", "awqwawqe"),
    ("string/block/set", "dwewdweq"),
    ("string/chat/caster", "waqa"),
    ("string/chat/all", "wded"),
    ("string/chat/prefix/get", "ewded"),
    ("string/chat/prefix/set", "qwaqa"),
    # string concat, spelled as alt arithmetic operators
    ("altadd", "waawawaeawwaea"),
    ("altmul", "waqawawwaeaww"),
    ("altdiv", "wdedwdwwdqdww"),
    ("altpow", "wedewqawwawqwa"),
    # -- types --------------------------------------------------------------
    ("type/to_item", "qaqqaea"),
    ("type/entity", "qawde"),
    ("type/iota", "awd"),
    ("type/item_held", "edeedqd"),
    ("get_entity/type", "dadqqqqqdad"),
    ("zone_entity/type", "waweeeeewaw"),
    ("zone_entity/not_type", "wdwqqqqqwdw"),
    # -- items --------------------------------------------------------------
    ("item/main_hand", "adeq"),
    ("item/off_hand", "qeda"),
    ("item/inventory/stacks", "aqwed"),
    ("item/inventory/items", "dewqa"),
    ("item/make", "adeeedew"),
]


def main():
    taken = occupied()
    taken_shapes = {canonical(s) for s in taken}
    print(f"meowhex occupied: {len(taken)} signatures, {len(taken_shapes)} shapes\n")

    blocked = []
    for rid, sig in UPSTREAM:
        clash = taken.get(sig)
        shape_clash = canonical(sig) in taken_shapes
        if clash or shape_clash:
            reason = []
            if clash:
                reason.append("signature at " + ", ".join(clash))
            if shape_clash:
                reason.append("shape already drawn")
            blocked.append((rid, sig, "; ".join(reason)))
            print(f"  BLOCKED  {rid:<26} {sig:<18} {'; '.join(reason)}")
        else:
            print(f"  free     {rid:<26} {sig}")

    print(f"\n{len(UPSTREAM) - len(blocked)} free, {len(blocked)} blocked")


if __name__ == "__main__":
    main()
