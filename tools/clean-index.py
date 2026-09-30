#!/usr/bin/env python3
"""Почистить index.toml от того, чего нет в git, и пересчитать хеш в pack.toml.

Зачем. packwiz индексирует модпак прямым обходом каталога и `.gitignore`
НЕ уважает. Поэтому `packwiz refresh`, запущенный на машине, где моды уже
собраны, затягивает в индекс всё подряд:

    src/meowhex/build/...   3126 файлов
    src/linage_core/build/   988
    src/meowhex/.mcprobe2/  1215
    ...                                            всего ~11 000 записей

Последствия:
  * в репозиторий уезжает мусорный index.toml (2.5 МБ, ~12 000 лишних строк);
  * `packwiz mr export` начинает вкладывать build-артефакты в overrides --
    локальный .mrpack раздувается с 71 МБ до 1.6 ГБ.

На CI этого не происходит: там свежий checkout, `src/**/build` не существует,
и `packwiz refresh` сам вычищает такие записи. Скрипт делает индекс таким же,
как у CI, ещё до коммита.

Что выживает:
  * mods/*.jar -- включая скачанные и не закоммиченные (лаунчер их качает,
    packwiz обязан их видеть; в репо они под .gitignore, но в паке нужны);
  * config/, kubejs/, defaultconfigs/, tools/, prism/ -- как есть;
  * src/** -- ровно то, что есть в git (исходники, libs/, билд-скрипты).

Отдельно выкидывается resourcepacks/: аудио комментаторов намеренно не лежит
в репозитории и раздаётся игрокам отдельным паком (см. .gitignore), поэтому
в .mrpack оно попадать не должно.

Использование:
    python tools/clean-index.py            # почистить и обновить pack.toml
    python tools/clean-index.py --check    # только проверить, ничего не писать
"""
import argparse
import hashlib
import os
import re
import subprocess
import sys

try:
    import tomllib
except ImportError:  # Python < 3.11
    sys.exit("нужен Python 3.11+")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INDEX = os.path.join(ROOT, "index.toml")
PACK = os.path.join(ROOT, "pack.toml")

# Каталоги, которых не бывает в свежем checkout. Дублируют .gitignore:
# packwiz его не читает, поэтому правила приходится держать ещё и здесь.
ALWAYS_DROP_PREFIXES = (
    "resourcepacks/",
    "src/meowhex/.mcprobe2/",
    "src/meowhex/.claude/",
    "src/meowhex/.vscode/",
    "src/meowhex/.kotlin/",
    "src/meowhex/data/",       # дамп KubeJS-датагена
    "src/meowhex/sosud/",
    "src/meowhex/oggs/",
    "src/meowhex/META-INF/",   # распакованный jarjar, генерится при сборке
    "src/meow relics/",        # в разработке, пока вне сборки
    "src/meowrelics/",
    "tools/__pycache__/",
)
ALWAYS_DROP_RE = re.compile(
    r"^src/[^/]+/(build|run|run2|\.gradle|\.kotlin)/"
    r"|^src/[^/]+/run_[^/]*\.log$"
    r"|^src/meowhex/kubejs(_logo\.(png|jpg))?"
    r"|^src/meowhex/kubejs\.(mixins\.json|plugins\.txt|classfilter\.txt)$"
)

# Блоки, которые packwiz пишет по одну на файл:
#   [[files]]
#   file = "..."
#   hash = "..."
#   hash-format = "sha256"
FILE_BLOCK = re.compile(
    r'(?ms)^\[\[files\]\]\nfile = "(?P<path>[^"]+)".*?(?=^\[\[|\Z)'
)


def git_tracked():
    """Множество путей, которые есть в индексе git (только отслеживаемые)."""
    out = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=ROOT, capture_output=True, check=True,
    )
    return {p for p in out.stdout.decode("utf-8").split("\0") if p}


def should_drop(path, tracked):
    if any(path.startswith(p) for p in ALWAYS_DROP_PREFIXES):
        return True
    if ALWAYS_DROP_RE.search(path):
        return True
    # Под src/ берём только то, что реально закоммичено.
    if path.startswith("src/") and path not in tracked:
        return True
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true",
                    help="только показать, что было бы выкинуто")
    args = ap.parse_args()

    if not os.path.isfile(INDEX):
        sys.exit(f"Нет индекса: {INDEX}")

    text = open(INDEX, encoding="utf-8", newline="").read()
    tracked = git_tracked()

    kept, dropped = [], []
    for m in FILE_BLOCK.finditer(text):
        path = m.group("path")
        (dropped if should_drop(path, tracked) else kept).append(path)

    print(f"Записей: {len(kept) + len(dropped)}  останется: {len(kept)}  "
          f"выкинем: {len(dropped)}")
    if dropped:
        buckets = {}
        for p in dropped:
            parts = p.split("/")
            key = "/".join(parts[:3]) if len(parts) > 2 else p
            buckets[key] = buckets.get(key, 0) + 1
        for key, n in sorted(buckets.items(), key=lambda kv: -kv[1])[:15]:
            print(f"  -{n:>6}  {key}")

    if args.check:
        return 0 if not dropped else 1
    if not dropped:
        print("Чистить нечего.")
        return 0

    # Пересобираем index.toml, сохраняя шапку до первого [[files]].
    head_end = text.index("[[files]]")
    header = text[:head_end]
    new_text = header + "".join(m.group(0) for m in FILE_BLOCK.finditer(text)
                                if not should_drop(m.group("path"), tracked))
    with open(INDEX, "w", encoding="utf-8", newline="") as f:
        f.write(new_text)

    digest = hashlib.sha256(open(INDEX, "rb").read()).hexdigest()
    pack_text = open(PACK, encoding="utf-8", newline="").read()
    new_pack = re.sub(r'(?m)^hash = "[0-9a-f]{64}"$', f'hash = "{digest}"',
                      pack_text)
    if new_pack != pack_text:
        with open(PACK, "w", encoding="utf-8", newline="") as f:
            f.write(new_pack)

    print(f"\nindex.toml: {len(dropped)} записей удалено")
    print(f"pack.toml hash = {digest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())