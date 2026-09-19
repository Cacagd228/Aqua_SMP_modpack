# Changelog

## v1.1.1-beta.4

- `meowaddons` 1.0.0 пересобран из исходников (`src/meowaddons`):
  - Вырезан спавн структур/мобов из мода (глушится kubejs-датапаком:
    `kubejs/data` no_op-оверрайды + `kubejs/server_scripts/disable_mobs.js`).
  - Новое зачарование лука «Мульти выстрел» (`meowaddons:triple_shot`): залп из 3 стрел.
  - Новое зачарование лука «Авто выстрел» (`meowaddons:auto_shot`): автоспуск тетивы на полном натяге.

## v1.1.1-beta.3

- Fix: `biolith` side=both (теперь на клиенте и сервере)
- Добавлен `fmm_worldgen` в исходники (pinned jar)
- Добавлен `fmm_teams` в исходники (pinned jar)
- Удалены kubejs скрипты HexJS (haste_cast, haste_register, haste_scroll, ovid_test)

## v1.1.1-beta.2

- Fix: `biolith` side=both (теперь на клиенте и сервере)
- Добавлены минимальные файлы FTB Quests (тема, глава квестов) для предотвращения краша
- NeoForge installer включён в server pack
- Force-add PonderJS в server pack
- Exclude UIQuest из server pack

## v1.1.1-beta.1

- `lineage_core` 2.0.1 (сборка из исходников `src/linage_core`): пофикшена раса Лудоман.
- Убран `create_parachute` (jar, конфиг, записи packwiz).
- `powergrid` 0.6.1 → 0.6.2 (PR #1 от MrPe4henika; дочинен `index.toml`).
- `meowaddons` 1.0.0 пересобран из исходников: тирные mixer/saw/millstone/crusher/deployer/fan (T1–T6).
- `create_avionics` 0.5.2 → 0.6.0.

## v1.1.0-beta.1

- `lineage_core`: починенный билд 2.0.0.
- Добавлен `fmm_teams` 0.1.0 (бета): команды в стиле Panoptic.
- Сборка в статусе беты.

## v1.0.0 — миграция на packwiz

- Переезд со старой репы `Factory_must_meowing` (ветка `master`, ~958 МБ в `.git`).
- Формат: **packwiz** — в гите только манифест, конфиги, KubeJS; моды качаются
  с Modrinth, готовые сборки — в Releases (`.mrpack`).
- 196 модов с Modrinth с запиненными версиями (проверены хеши sha512
  против рабочей сборки; MC 1.21.1, NeoForge 21.1.248).
- 15 запиненных jar'ов в `mods/`: 5 кастомных + 10 отсутствующих на Modrinth.
- `config/`: 345 файлов, вычищен рантайм (`.bak`, iris/sodium-fingerprint,
  JEI-выборки, xaero, миникарты, голосовалка преференсов).
- `kubejs/`: 22 файла без изменений (абсолютных путей нет).
- Выкинуто: `jeiexport` (дев-тулза), дубль `moonlight-3.5.2`,
  `temp.class`/`temp_eh.class`, машинный `instance.cfg`.

### Известно / TODO

- 8 сторонних пинов без автообновлений (CurseForge-only): FTB-трио,
  `framework`, `harderdiesel`, `waterwheelbearing`, `UIQuest`,
  `create-aeronautics-FIXED`. Нужен `CF_API_KEY` + `packwiz cf` либо ручной bump.
- Кастомные моды кладутся в пак вручную; следующий шаг —
  сборка их в CI из исходников.
- `fmm_teams` (0.1.0) в пак не входит, ждёт готовности.
