# Changelog

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
