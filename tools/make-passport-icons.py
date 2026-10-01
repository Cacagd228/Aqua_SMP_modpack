"""Рисует 3 варианта иконки паспорта (16x16) в src/colonycard/candidates/passport/.

Это кандидаты на выбор: модом не используются, в jar не попадают
(папка вне src/main/resources). Взять понравившийся -> положить в
src/main/resources/assets/colonycard/textures/item/passport.png
и указать слой в models/item/passport.json.

Запуск: python tools/make-passport-icons.py
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "colonycard", "candidates", "passport")

# Палитра паспорта (совпадает с CardStyle в colonycard).
PARCHMENT = (242, 226, 190, 255)
PARCHMENT_DEEP = (228, 200, 150, 255)
EDGE = (176, 141, 87, 255)
INK = (58, 42, 24, 255)
INK_FAINT = (138, 115, 85, 255)
COVER_RED = (126, 33, 33, 255)
COVER_DEEP = (87, 19, 19, 255)
GOLD = (217, 180, 92, 255)
CREAM = (246, 231, 200, 255)
SHADOW = (60, 45, 30, 90)


def new_canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def book_closed(d):
    """Закрытая книга-паспорт с сургучной печатью и золотым корешком."""
    d.rectangle([3, 2, 12, 13], fill=COVER_RED, outline=COVER_DEEP)
    d.rectangle([2, 2, 3, 13], fill=COVER_DEEP)          # корешок
    d.line([5, 3, 5, 12], fill=GOLD, width=1)           # золотое тиснение
    d.line([7, 4, 10, 4], fill=GOLD, width=1)
    d.line([7, 6, 10, 6], fill=GOLD, width=1)
    d.rectangle([8, 9, 11, 12], fill=CREAM)             # наклейка-номер
    d.rectangle([8, 9, 11, 12], outline=INK_FAINT)
    d.point([9, 10]), d.point([10, 11])


def booklet_open(d):
    """Открытый паспорт: разворот с фото и линейками."""
    d.polygon([(1, 3), (7, 2), (7, 13), (1, 14)], fill=PARCHMENT, outline=INK)
    d.polygon([(8, 2), (14, 3), (14, 14), (8, 13)], fill=PARCHMENT, outline=INK)
    d.line([7, 2, 8, 13], fill=INK_FAINT, width=1)      # корешок
    d.rectangle([2, 4, 6, 8], fill=PARCHMENT_DEEP, outline=INK_FAINT)   # фото
    d.rectangle([3, 5, 5, 7], fill=INK_FAINT)
    d.line([2, 10, 6, 10], fill=INK_FAINT)
    d.line([2, 12, 6, 12], fill=INK_FAINT)
    d.line([9, 5, 13, 5], fill=INK)
    d.line([9, 7, 13, 7], fill=INK_FAINT)
    d.line([9, 9, 12, 9], fill=INK_FAINT)
    d.rectangle([9, 10, 12, 12], outline=INK_FAINT)


def card_photo(d):
    """Удостоверение-карточка: скруглённая карточка с фото и линиями."""
    d.rectangle([2, 3, 13, 12], fill=PARCHMENT, outline=INK)
    d.rectangle([2, 3, 13, 4], fill=PARCHMENT_DEEP)    # верхняя полоса
    d.line([2, 3, 13, 3], fill=EDGE)
    d.line([2, 12, 13, 12], fill=EDGE)
    d.rectangle([3, 5, 7, 10], fill=PARCHMENT_DEEP, outline=INK)
    d.rectangle([4, 6, 6, 7], fill=INK_FAINT)          # «лицо» на фото
    d.rectangle([4, 8, 6, 9], fill=INK_FAINT)
    d.line([8, 6, 12, 6], fill=INK)
    d.line([8, 8, 12, 8], fill=INK_FAINT)
    d.line([8, 10, 11, 10], fill=INK_FAINT)
    d.point([12, 11])


VARIANTS = {
    "passport_a_book": book_closed,
    "passport_b_booklet": booklet_open,
    "passport_c_card": card_photo,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, draw_fn in VARIANTS.items():
        img = new_canvas()
        d = ImageDraw.Draw(img)
        draw_fn(d)
        path = os.path.join(OUT, name + ".png")
        img.save(path, optimize=True)
        print("%-22s -> %s (%d bytes)" % (name, os.path.relpath(path, ROOT),
                                         os.path.getsize(path)))


if __name__ == "__main__":
    main()
