"""Копирует 3D-спрайт картонного шлема Create и затемняет его гамму.

Не перерисовывает: берёт попиксельно тот же PNG, только темнее.
Запуск: python tools/make-balaclava-texture.py
"""
import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CREATE_JAR = os.path.join(ROOT, "mods", "create-1.21.1-6.0.10.jar")
RES = os.path.join(ROOT, "src", "colonycard", "src", "main", "resources", "assets", "colonycard")

# Иконка предмета (16x16) + слои 3D-брони, которые надеваются на голову (64x32).
SOURCES = [
    ("assets/create/textures/item/cardboard_helmet.png",
     os.path.join(RES, "textures", "item", "balaclava.png")),
    ("assets/create/textures/models/armor/cardboard_layer_1.png",
     os.path.join(RES, "textures", "models", "armor", "balaclava_layer_1.png")),
]

# Насколько темнее: 1.0 = как в Create, больше = чернее (гамма по нормализованной яркости).
GAMMA = 3.0
# Минимум, чтобы совсем не слиплось в ноль (силуэт шлема должен читаться), 0..255.
MIN_LUMA = 12


def luma(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def darken(img):
    """Тот же спрайт, но темнее: гамма по яркости + пол + сведение к нейтральному углю."""
    px = img.load()
    changed = 0
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = luma(r, g, b)
            t = (l / 255.0) ** GAMMA
            nl = max(MIN_LUMA, t * 255.0)
            scale = nl / l if l > 0 else 0.0
            nr = min(255, int(r * scale))
            ng = min(255, int(g * scale))
            nb = min(255, int(b * scale))
            # Убираем остаточный цветной оттенок: приводим к нейтральному углю.
            avg = (nr + ng + nb) / 3.0
            nr = int(nr * 0.35 + avg * 0.65)
            ng = int(ng * 0.35 + avg * 0.65)
            nb = int(nb * 0.35 + avg * 0.65)
            px[x, y] = (nr, ng, nb, a)
            changed += 1
    return changed


def main():
    jar = CREATE_JAR
    if not os.path.isfile(jar):
        alt = os.path.join(ROOT, "..", "AquaSMP-v1.1.5-pre-release(1)", "minecraft", "mods",
                           "create-1.21.1-6.0.10.jar")
        if os.path.isfile(alt):
            jar = alt
        else:
            sys.exit("create jar not found")

    with zipfile.ZipFile(jar) as z:
        for entry, dst in SOURCES:
            img = Image.open(io.BytesIO(z.read(entry))).convert("RGBA")
            size = img.size
            changed = darken(img)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            img.save(dst, optimize=True)
            print("%s %s -> %s (pixels: %d, %d bytes)"
                  % (entry, size, os.path.relpath(dst, ROOT), changed, os.path.getsize(dst)))


if __name__ == "__main__":
    main()
