# Create Aeronautics Fix (`aerofix`)

Фикс-мод для **Create Aeronautics** (Simulated Project), который ставится **рядом с
оригинальным bundle-модом**, а не заменяет его.

В модепаке лежал `create-aeronautics-bundled-1.21.1-1.3.2-FIXED.jar` — это не
патч-мод, а **полная пересборка** оригинала (33 136 055 байт против 33 123 131 у
стока). Правки вшиты в перекомпилированные классы, поэтому мод закреплён
необновляемым пином на GitHub raw и блокирует апдейты Create Aeronautics.

`aerofix` решает ровно ту же задачу, но оставляет оригинальный jar нетронутым:
все изменения применяются mixin'ами в рантайме.

## Что чинит

Целевая версия — **стоковый `create-aeronautics-bundled-1.21.1-1.3.2.jar`**
(Modrinth, sha1 `ddbe9cbc25e8f663218aa1e9fdfc4b99f0e11e4b`).

### 1. Руль: серверная валидация пакета — `SteeringWheelPacketMixin`

`SteeringWheelPacket#handle` берёт угол из сети и пишет его в блок как есть.

| Проверка | Зачем |
|---|---|
| `Float.isFinite(targetAngle)` | NaN/Inf в Sable-физике не даёт «разъехаться» contraption — она застревает и тянет за собой симуляцию |
| `distanceToSqr(pos) <= steeringMaxRange²` | Раньше пакет с корректным `BlockPos` применялся к **любому** рулю на сервере, в т.ч. на другом конце карты |
| `getBlockEntity(pos) instanceof SteeringWheelBlockEntity` | Не дёргаем `getBlockEntity` по чужому `BlockPos` |

### 2. Руль: санация состояния — `SteeringWheelBlockEntityMixin`

| Точка | Правка |
|---|---|
| `updateTargetAngle(F)` HEAD | Отсекает неfinite-вход. Кламп в стоке не спасает: `Math.max(NaN, min) == NaN` |
| `updateTargetAngle(F)` HEAD | `angleInput.value` клампится в `[1, 360]` — тот же диапазон, что Create задаёт в `ScrollValueBehaviour.between(1, 360)`. Ноль давал деление на ноль в `SteeringWheelHandler#angleLimit` |
| `tick()` HEAD | Самовосстановление каждый тик: `angle`, `targetAngle`, `targetAngleToUpdate` — неfinite → 0; `targetAngleToUpdate` клампится в пределы руля; `sequencedAngleLimit` — неfinite → 0 и кламп в `[0, 720]` |
| `read(...)` HEAD | Чинит битый сейв **до** того, как Create его прочитает (те же ключи `Angle` / `TargetAngle` / `TargetAngleToUpdate` / `SequencedAngleLimit`) |

Кламп `targetAngleToUpdate` сделан в `tick()`, а не инлайн в пакете: поле
`targetAngleToUpdate` читается только в `tick()` и `write()`, то есть до следующего
тика никто не успеет увидеть невалидное значение. Это позволяет не дублировать тело
`handle()` — там нет доступа к BE из `@Redirect` без хака с полем на record-классе.

### 3. Поворотный подшипник: null-guard — `SwivelBearingBlockEntityMixin`

Стоковый `getAttachedSubLevel()`:

```java
SubLevel sl = SubLevelContainer.getContainer(level).getSubLevel(id);  // может вернуть null
validateConstraintHandle();                                          // handle может стать null
if (handle != null) reattachConstraint((ServerSubLevel) sl, true);   // sl — без проверки!
```

Два независимых источника `null` → отложенный NPE. Апстрим чинит это перестановкой
проверок; здесь guard стоит на самой точке входа в `reattachConstraint` и покрывает
все вызывающие места сразу. Проверять `handle.isValid()` нельзя — повторное
attach'ение к протухшему handle это ровно тот случай, ради которого метод существует.

### 4. Рендер горячего воздуха — `ClientBalloonEffectRendererMixin`

* **Краш:** сток берёт `Minecraft.getInstance().getWindow()` и сразу дёргает
  `getWidth()`. Окно бывает `null` до создания и при пересоздании — гарантированный
  NPE в кадре рендера.
* **Опция:** `hotAirRendering` в клиентском конфиге. Дымка Envelope идёт через Veil
  FBO + пост-шейдер и стоит ощутимо; раньше её можно было отключить только
  пересборкой bundle. На физику не влияет вообще — contraptions считаются на сервере.

Миксин лежит в секции `client`: на выделенном сервере целевой класс не грузится.

### 5. Покраска конвертов — датапак

16 рецептов «ванильная краска + любой Hot Air Envelope → цветной конверт» в
namespace `aerofix` (`data/aerofix/recipe/crafting/*_envelope_from_other_envelope.json`
+ ачивки). В FIXED-сборке они лежали в `data/simulated/`, но recipe id там жестко
прописан в ачивке — свой namespace избавляет от возможной коллизии при обновлении
мода. Теги `aeronautics:shaftless_envelope` и `c:dyes/*` — ванильные, ничего своего
мод не регистрирует.

## Чего мод НЕ делает

* **Не трогает `create:safe_nbt`.** В FIXED-сборке из этого тега убран
  `simulated:steering_wheel`. Это не крэш-фикс, а изменение геймплея: руль перестаёт
  тащить NBT в contraption. Включать это поведенческим изменением в фикс-моде —
  рискованно, поэтому вынесено за рамки. Если нужно — добавить
  `data/create/tags/block/safe_nbt.json` с `{"remove": ["simulated:steering_wheel"]}`.
* **Не переписывает `write()`.** FIXED-сборка перестала сохранять `InUse`,
  `SequencedAngleLimit`, `GeneratedSpeed`. Похоже на побочный эффект рефакторинга
  апстрима, а не самостоятельный фикс; оставлять сохранение как есть — безопаснее.
* Не трогает 1026 текстовых файлов, которые в FIXED-сборке были переведены из LF в
  CRLF (артефакт пересборки на Windows, на игру не влияет).

## Конфиги

`config/aerofix-server.toml` (мировые):

```toml
[fixes]
	fixSteeringWheel = true
	fixSwivelBearing = true
	steeringMaxRange = 64   # 8..512
```

`config/aerofix-client.toml`:

```toml
[rendering]
	hotAirRendering = true
```

Значения читаются через safe-хелперы: миксин может сработать раньше, чем конфиг
загрузится, и тогда возвращается дефолт, а не исключение.

## Сборка

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\gradlew.bat build
```

-> `build/libs/aerofix-1.0.0.jar`

Собирается против тех же версий, что и остальные моды пака:
MC 1.21.1, NeoForge 21.1.247, ModDevGradle 2.0.140, Gradle 9.4.1, Java 21.

### libs/

Зависимости для компиляции (`compileOnly`) — в jar не попадают. Inner-jar'ы
Create Aeronautics распаковываются из `META-INF/jarjar/` **стокового** bundle.
Подробности и порядок обновления — в `libs/README.txt`.

## Обновление Create Aeronautics

Мод рассчитан на 1.3.x. Стоит пересобрать `libs/` под новую версию bundle и
пересобрать мод: если сигнатуры целевых методов разошлись, mixin не применится и
мод не загрузится — это громкая ошибка при старте, а не тихая поломка в рантайме.
