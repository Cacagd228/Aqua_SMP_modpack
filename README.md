# Aqua SMP — модпак

Индустриально-исследовательская сборка: **Minecraft 1.21.1 + NeoForge 21.1.248**,
вокруг экосистемы **Create 6**, физики кораблей и дирижаблей, артиллерии,
кастомной генерации архипелага, экономики и квестов.

> Репа хранит **только исходники** (packwiz-манифест, конфиги, KubeJS).
> Готовые сборки лежат в **Releases** в виде `.mrpack`.

## Установка (игроку)

Вариант 1 — через лаунчер (рекомендуется):
1. Открой **Releases** справа на странице репозитория.
2. Скачай последний `AquaSMP-vX.Y.Z.mrpack`.
3. Импортируй:
   - **PrismLauncher**: Добавить сборку → Импорт → выбрать `.mrpack`.
   - **Modrinth App**: + → From file.

Вариант 2 — полным архивом:
1. Скачай `AquaSMP-vX.Y.Z-prism.zip` из Releases.
2. **PrismLauncher**: Добавить сборку → Импорт → выбрать `.zip`.
   (Внутри уже все моды, конфиги и KubeJS — ничего докачивать не надо.)

Память: минимум 6 ГБ, рекомендовано **8 ГБ**. Java 21.

## Состав

- **220 модов** тянутся с Modrinth по `mods/*.pw.toml` (версии запинены).
- **16 jar'ов** лежат в `mods/` напрямую — их нет на Modrinth:
- кастомные из `src/`: `aerofix`, `FMMWorldgen`, `lineage_core`, `meowaddons`,
    `meowhex`, `meowrelics`, `apofix`, `fmm_teams`, `hexsable`, `colonycard`;
  - CurseForge-only, запинены: `framework`, `harderdiesel`, `structure_pool_api`;
  - приватные/репаки: `panoptic_recipe_builder`, `Design-n-Decor`, `colorwheel`.
- Убрано из старой сборки: `jeiexport` (дев-инструмент), дубль
  `moonlight-3.5.2`, `neoforge.mods.toml` из `mods/`.
- Убрано в 1.1.6: вся линейка FTB (`ftb-library`, `ftb-quests`, `ftb-teams`,
  `FTBQuestsOptimizer`, `UIQuest`), `waterwheelbearing`, `rogues-and-warriors`,
  `Structure Pool API`, а также `Axiom`, `Create: Bits n' Bobs`,
  `Create: Cyber Goggles`, `Create: Goggles`, `sablexaeromaps`, `Tree Physics`,
  `Xaero's Minimap`, `Xaero's World Map`, `Xaero's Maps: Multiplayer+`.
- Create Aeronautics теперь **стоковый** с Modrinth, а правки навешивает
  отдельный мод `aerofix` — обновления больше не заблокированы.
- Добавлено в 1.1.8: `CC: Sable` 1.3.4 (аддон CC: Tweaked для Sable-задней части
  Create: Simulated) и `Controlling` 19.0.5 (подсказки по клавишам). Оба запинены
  по Modrinth — версии запиненные, автообновление работает.

> Отдельно раздаётся ресурс-пак `AquaSMP-Sounds` (звуки комментаторов и тишина).
> В моде `meowhex` аудио больше нет; пак собирается `tools/build-sounds-pack.py`
> и кидается игрокам в `.minecraft/resourcepacks`.

| Кастомный мод | mod_id | Версия | Что делает |
|---|---|---|---|
| AeroFix | `aerofix` | 1.0.0 | Фикс Create Aeronautics поверх стокового bundle |
| FMM Worldgen | `fmm_worldgen` | 1.1.0 | Кастомная генерация мира-архипелага |
| Lineage Core | `lineage_core` | 2.1.1 | Ядро: расы/происхождения, команды, статистика |
| Meow Addons | `meowaddons` | 1.0.1 | Create-аддоны: блоки, передатчики, пондеры |
| MeowHex | `meowhex` | 1.4.0 | Hex-магия: мана, паттерны, assembly, контент |
| MeowRelics | `meowrelics` | 1.1.0 | Ребаланс Relics, мешочки вместо дропа, аддон-артефакты |
| HexSable | `hexsable` | 1.1.0 | Мост Hex Casting ↔ Sable |
| Colony Card | `colonycard` | 1.0.0 | Карточки colony |
| Apofix | `apofix` | 1.0.0 | Доп. атрибуты для Apothic Attributes |
| FMM Teams | `fmm_teams` | 0.1.0 | Команды, клеймы островов и админ-панель в стиле Panoptic |

## Разработка

Нужны: packwiz (последний билд с
`nightly.link/packwiz/packwiz/workflows/go/main`), Java 21.

```sh
packwiz update --all               # проверить обновления модов (пины без [update] не трогает)

# пересборка релиза — обе команды работают в чистом дереве из tracked-файлов
python tools/refresh-index-clean.py                       # index.toml
python tools/sync-index-hash.py                            # хеш индекса в pack.toml
python tools/export-mrpack-clean.py AquaSMP-vX.Y.Z.mrpack  # .mrpack
python tools/build-prism-pack.py vX.Y.Z                   # zip для PrismLauncher
python tools/verify_release.py                             # проверка всего пака
```

`verify_release.py` берёт версию из переменной окружения `AQUA_VERSION`
(по умолчанию — текущая) и ищет по ней `.mrpack` и prism-zip, поэтому после
бампа версии её нужно задать явно:

```sh
AQUA_VERSION=1.1.8-release python tools/verify_release.py
```

> **Почему не `packwiz refresh` / `packwiz mr export` напрямую.** `packwiz`
> индексирует модпак прямым обходом каталога и `.gitignore` не читает, а
> `mr export` ещё и сам вызывает refresh. Если сборка модов уже лежит в
> `src/*/build`, индекс раздувается на ~8 000 записей, а `.mrpack` — с 37 МБ
> до 1.5 ГБ. `*-clean.py` выгружают tracked-файлы во временный каталог и
> запускают packwiz там, то есть получают ровно то, что увидит CI (свежий
> checkout + скачанные `mods/*.jar`).

Структура:

```
Aqua_SMP_modpack/
├── pack.toml / index.toml   # манифест пака (MC 1.21.1, NeoForge 21.1.248)
├── mods/*.pw.toml           # 220 модов с Modrinth (id версии запинен)
├── mods/*.jar               # 16 пинов (см. выше), исключения в .gitignore
├── config/                  # 364 файла (рантайм-мусор вычищен, см. ниже)
├── kubejs/                  # скрипты (server/client/startup)
├── defaultconfigs/
├── .github/workflows/release.yml
└── CHANGELOG.md
```

В `config/` намеренно **не** коммитится: `*.bak`, `iris.properties`,
`sodium-fingerprint.json`, `packetfixer.properties`, JEI-выборки,
`xaero/`, `justzoom/`, `spark/`, `WildfireGender/`, `quickskin*`,
`observable.json`, `MouseTweaks.cfg`, `recipe-tree-*.json`.

## Релизы

Тег `vX.Y.Z` → workflow собирает `.mrpack` через `packwiz mr export`
и прикладывает к GitHub Release. История изменений — в `CHANGELOG.md`.
