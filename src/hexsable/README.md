# Hex Sable Bridge

Аддон к MeowHex (порт Hex Casting 1.21.1) и Sable: даёт хексам работать с физическими
постройками Sable так же, как они работают с Entity.

## Установка
Положи `hexsable-1.0.1.jar` в `mods/` рядом с `meowhex`, `sable`, `sablecompanion`. Больше ничего не нужно.
Конфиг: `config/hexsable-server.toml` (стоимости, лимиты, переключатель совместимости с блочными заклинаниями).

## Как это работает
Новый тип итоты — **Physical Structure** (аналог Entity iota). Дальше — обычные паттерны:
получил итоту → передал её в заклинание. Список паттернов (рисунки) — `patterns_cheatsheet.png`,
описания есть и в книге Hex (раздел паттернов, запись «Physical Structures»).

| Паттерн | Вход → выход |
|---|---|
| Structure Purification | вектор или сущность → структура / Null |
| Structure Zone Distillation | вектор, число → [структуры] |
| Compass' Purification: Structure | структура → мировой центр масс |
| Pace / Whirl Purification: Structure | структура → скорость (бл/тик) / угловая (рад/тик) |
| Scales' Purification: Structure | структура → масса |
| Surveyor's Purification: Structure | структура → min, max мирового AABB |
| Structure Projection: Outward / Inward | структура, вектор → точка в мир / в координаты блоков структуры |
| Structure Orientation: Outward / Inward | структура, вектор → направление в мир / в систему структуры |
| Structure Impulse | структура, Δv → толчок центра масс |
| Structure Impulse: Offset | структура, точка, Δv → толчок в точке (+вращение) |
| Structure Torque | структура, Δω → раскрутка |
| Structure Blink | структура, смещение → телепорт всей постройки |

Все векторы — в мировой системе; скорости — блоки/тик, угловые — рад/тик (как у Entity).
Цена импульса ≈ пыль × масса × |Δv|² (для массы 1 совпадает с Add Motion).

**Блочные заклинания на постройках:** `Structure Projection: Inward` переводит мировую точку в
координаты блоков структуры; Break Block / Place Block и др. принимают их — проверка дальности
проецирует такую точку обратно в мир (см. `SableRangeComponent`).

## Сборка из исходников
1. Положи JAR из `libs/README.txt` в `libs/`.
2. `./gradlew build` (нужна Java 21) → `build/libs/hexsable-1.0.1.jar`.

## Что важно знать
- Собрано без реального Minecraft и **не запускалось в игре**. Код компилировался против настоящих jar
  MeowHex/Sable, все вызовы Minecraft/NeoForge/JOML сверены по байткоду этих jar.
- Паттерны подобраны так, чтобы не пересекаться с Hex/HexJS из MeowHex. С другими аддонами
  возможны совпадения — тогда поменяй строку в `HexSableActions.java`.
- Страницы книги привязаны к форку MeowHex (`meowhex:pattern`); на других сборках Hex удали файл
  `assets/meowhex/patchouli_books/.../hexsable_structures.json`.
