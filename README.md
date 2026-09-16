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

- **196 модов** тянутся с Modrinth по `mods/*.pw.toml` (версии запинены).
- **16 jar'ов** лежат в `mods/` напрямую — их нет на Modrinth:
- кастомные: `FMMWorldgen`, `lineage_core`, `meowaddons`, `meowhex`, `apofix`,
  `fmm_teams`;
  - CurseForge-only, запинены: `ftb-library`, `ftb-quests`, `ftb-teams`,
    `framework`, `harderdiesel`, `waterwheelbearing`, `UIQuest`;
  - приватные/репаки: `create-aeronautics-bundled-*-FIXED`,
    `panoptic_recipe_builder`, `sablexaeromaps`.
- Убрано из старой сборки: `jeiexport` (дев-инструмент), дубль
  `moonlight-3.5.2`, `neoforge.mods.toml` из `mods/`.

| Кастомный мод | mod_id | Версия | Что делает |
|---|---|---|---|
| FMM Worldgen | `fmm_worldgen` | 1.0.0 | Кастомная генерация мира-архипелага |
| Lineage Core | `lineage_core` | 2.0.0 | Ядро: расы/происхождения, команды, статистика |
| Meow Addons | `meowaddons` | 1.0.0 | Create-аддоны: блоки, передатчики, пондеры |
| MeowHex | `meowhex` | 1.0.0 | Hex-магия: мана, паттерны, контент |
| Apofix | `apofix` | 1.0.0 | Доп. атрибуты для Apothic Attributes |
| FMM Teams | `fmm_teams` | 0.1.0 | Команды в стиле Panoptic (бета) |

## Разработка

Нужны: packwiz (последний билд с
`nightly.link/packwiz/packwiz/workflows/go/main`), Java 21.

```sh
packwiz refresh          # пересобрать index.toml, проверить хеши
packwiz update --all     # проверить обновления модов (пины без [update] не трогает)
packwiz mr export        # собрать .mrpack локально для теста
```

Структура:

```
Aqua_SMP_modpack/
├── pack.toml / index.toml   # манифест пака (MC 1.21.1, NeoForge 21.1.248)
├── mods/*.pw.toml           # 196 модов с Modrinth (id версии запинен)
├── mods/*.jar               # 15 пинов (см. выше), исключения в .gitignore
├── config/                  # 345 файлов (рантайм-мусор вычищен, см. ниже)
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
