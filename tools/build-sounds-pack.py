#!/usr/bin/env python3
"""Собрать ресурс-пак со звуками комментаторов в zip.

Ассеты лежат распакованными в resourcepacks/AquaSMP-Sounds/, этот скрипт
собирает из них AquaSMP-Sounds.zip — так его и раздают игрокам
(кинуть в .minecraft/resourcepacks и включить в настройках).

Важно: записи в zip пишутся с прямыми слэшами. Compress-Archive из
PowerShell кладёт обратные слэши, и такой пак Minecraft не читает.

Каталог целиком в git не отслеживается (см. .gitignore) — аудио
комментаторов намеренно лежит вне репозитория.
"""
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK_DIR = os.path.join(ROOT, "resourcepacks", "AquaSMP-Sounds")
OUTPUT_FILE = os.path.join(ROOT, "resourcepacks", "AquaSMP-Sounds.zip")

# Файлы, которые обязаны попасть в пак, иначе он не загрузится.
REQUIRED = ("pack.mcmeta", "assets/meowhex/sounds.json")


def collect(pack_dir):
    """Относительные пути всех файлов пака, прямыми слэшами."""
    paths = []
    for root, _dirs, files in os.walk(pack_dir):
        for name in files:
            full = os.path.join(root, name)
            paths.append(os.path.relpath(full, pack_dir).replace(os.sep, "/"))
    return sorted(paths)


def main():
    if not os.path.isdir(PACK_DIR):
        sys.exit(f"Каталог пака не найден: {PACK_DIR}")

    entries = collect(PACK_DIR)
    missing = [r for r in REQUIRED if r not in entries]
    if missing:
        sys.exit("В паке не хватает: " + ", ".join(missing))

    sounds = [e for e in entries if e.endswith(".ogg")]
    if not sounds:
        sys.exit("В паке нет ни одного .ogg — нечего собирать.")

    with zipfile.ZipFile(OUTPUT_FILE, "w", zipfile.ZIP_DEFLATED) as zf:
        for rel in entries:
            zf.write(os.path.join(PACK_DIR, rel), rel)

    size = os.path.getsize(OUTPUT_FILE)
    print(f"Готово: {os.path.relpath(OUTPUT_FILE, ROOT)}")
    print(f"  {len(entries)} записей, из них .ogg: {len(sounds)}")
    print(f"  {size / 1024 / 1024:.2f} MB")


if __name__ == "__main__":
    main()
