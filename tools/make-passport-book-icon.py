"""Иконка паспорта: ванильная книга (minecraft:item/written_book) в палитре паспорта.

Эталон -- вариант E (written_book). Правки к нему:
  * обложка и переплёт уходят в красный (страницы/бумага остаются пергаментными);
  * весь спрайт темнеет на 10%;
  * белая печать в левом нижнем углу -- ОТКЛЮЧЕНА (APPLY_SEAL = False), код
    печати остался в файле на случай возврата.

Смена гаммы идёт по яркости, поэтому светотень и силуэт ванильные.

Результат:
  src/colonycard/src/main/resources/assets/colonycard/textures/item/passport.png   (живая текстура)
  src/colonycard/candidates/passport/passport_e_vanilla_written.png                (эталон до правок)
  src/colonycard/candidates/passport/passport_f_final.png                          (эталон + правки)

Запуск: python tools/make-passport-book-icon.py
"""
import io
import os
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VANILLA = os.path.join(ROOT, "src", "colonycard", "build", "moddev", "artifacts",
                       "neoforge-21.1.248-client-extra-aka-minecraft-resources.jar")
CAND = os.path.join(ROOT, "src", "colonycard", "candidates", "passport")
LIVE = os.path.join(ROOT, "src", "colonycard", "src", "main", "resources", "assets",
                    "colonycard", "textures", "item", "passport.png")

# Обложка: чем светлее исходный пиксель, тем светлее красный (тёплый верхний блик).
COVER_RAMP = [
    (16, (46, 10, 10)),
    (26, (74, 14, 14)),
    (34, (94, 18, 18)),
    (43, (122, 26, 26)),
    (49, (140, 31, 31)),
    (60, (168, 41, 41)),
    (67, (196, 62, 52)),
    (76, (218, 112, 70)),
    (90, (232, 168, 100)),
]
# Бумага: серые пиксели исходника сюда, красным их НЕ красим.
PAPER_RAMP = [
    (90, (176, 152, 108)),
    (153, (196, 170, 122)),
    (182, (224, 206, 162)),
    (214, (243, 227, 194)),
]
GREY_MAX_SPREAD = 14   # серый, если разброс каналов меньше этого
DARKEN = 0.90          # -10% яркости
APPLY_SEAL = False     # белая печать: выключена по просьбе

# Белая печать в левом нижнем углу обложки: точка (cx, cy), радиус, ободок.
# Печать рисуется ПОСЛЕ затемнения, чтобы остаться белой.
SEAL = {
    "center": (4, 11),
    "r": 2.1,
    "rim_from": 1.1,
    "fill": (248, 248, 248, 255),
    "rim": (206, 206, 206, 255),
    "emblem": (170, 170, 170, 255),
}


def luma(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def pick(ramp, lum):
    return min(ramp, key=lambda s: abs(s[0] - lum))[1]


def recolor(img):
    """Красная обложка + пергаментная бумага, структура исходника сохранена."""
    out = Image.new("RGBA", img.size)
    src, dst = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            lum = luma(r, g, b)
            if max(r, g, b) - min(r, g, b) <= GREY_MAX_SPREAD:
                color = pick(PAPER_RAMP, lum)          # бумага -- не краснеет
            else:
                color = pick(COVER_RAMP, lum)          # обложка -- красная
            dst[x, y] = (color[0], color[1], color[2], a)
    return out


def darken(img, factor=DARKEN):
    out = Image.new("RGBA", img.size)
    src, dst = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = src[x, y]
            dst[x, y] = (min(255, round(r * factor)),
                         min(255, round(g * factor)),
                         min(255, round(b * factor)), a)
    return out


def add_seal(img, seal=SEAL):
    """Белая печать в левом нижнем углу (рисуем поверх, только по непрозрачным пикселям)."""
    out = img.copy()
    px = out.load()
    cx, cy = seal["center"]
    r = seal["r"]
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            if px[x, y][3] == 0:
                continue
            dist = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if dist > r:
                continue
            px[x, y] = seal["rim"] if dist > seal["rim_from"] else seal["fill"]
    # рельеф внутри печати: одна точка по центру
    ex, ey = cx, cy
    if px[ex, ey][3] != 0:
        px[ex, ey] = seal["emblem"]
    return out


def preview(img):
    px = img.load()
    for y in range(img.size[1]):
        print("   " + "".join(" " if px[x, y][3] == 0 else
                              ("#" if sum(px[x, y][:3]) < 200 else
                               ("o" if sum(px[x, y][:3]) < 450 else "."))
                              for x in range(img.size[1])))


def main():
    if not os.path.isfile(VANILLA):
        raise SystemExit("не найден ванильный jar: %s" % VANILLA)
    os.makedirs(CAND, exist_ok=True)
    z = zipfile.ZipFile(VANILLA)

    base = recolor(Image.open(io.BytesIO(
        z.read("assets/minecraft/textures/item/written_book.png"))).convert("RGBA"))
    base.save(os.path.join(CAND, "passport_e_vanilla_written.png"), optimize=True)

    final = darken(base)
    if APPLY_SEAL:
        final = add_seal(final)
    final.save(os.path.join(CAND, "passport_f_final.png"), optimize=True)
    os.makedirs(os.path.dirname(LIVE), exist_ok=True)
    final.save(LIVE, optimize=True)
    print("эталон E -> candidates/passport/passport_e_vanilla_written.png")
    print("правки   -> candidates/passport/passport_f_final.png + " + os.path.relpath(LIVE, ROOT))
    preview(final)


if __name__ == "__main__":
    main()
