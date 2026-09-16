"""Скачивает jar'ы по mods/*.pw.toml (для сборки полного zip в CI).

Использование: python tools/fetch-mods.py
Качает только отсутствующие/битые файлы, проверяет sha512.
Стандартная библиотека, без зависимостей.
"""
import hashlib
import os
import sys
import urllib.request

try:
    import tomllib
except ImportError:  # Python < 3.11
    print("need Python 3.11+", file=sys.stderr)
    sys.exit(1)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS = os.path.join(ROOT, "mods")


def sha512(path):
    h = hashlib.sha512()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(4 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    metas = sorted(f for f in os.listdir(MODS) if f.endswith(".pw.toml"))
    ok, failed = 0, []
    for i, meta in enumerate(metas, 1):
        with open(os.path.join(MODS, meta), "rb") as f:
            m = tomllib.load(f)
        dl = m.get("download", {})
        url, want = dl.get("url"), dl.get("hash")
        dest = os.path.join(MODS, m["filename"])
        if os.path.isfile(dest) and sha512(dest) == want:
            ok += 1
            continue
        try:
            req = urllib.request.Request(
                url, headers={"User-Agent": "AquaSMP-fetch/1.0"})
            with urllib.request.urlopen(req, timeout=120) as r, \
                    open(dest, "wb") as f:
                while True:
                    chunk = r.read(4 << 20)
                    if not chunk:
                        break
                    f.write(chunk)
            if sha512(dest) != want:
                raise ValueError("sha512 mismatch")
            ok += 1
            print(f"[{i}/{len(metas)}] got {m['filename']}", flush=True)
        except Exception as e:  # noqa: BLE001
            failed.append((m.get("filename", meta), str(e)))
            print(f"[{i}/{len(metas)}] FAIL {m.get('filename', meta)}: {e}",
                  flush=True)
    print(f"OK={ok} FAIL={len(failed)}")
    for fn, err in failed:
        print(f"  FAILED: {fn}: {err}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
