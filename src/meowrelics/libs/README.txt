Положи сюда JAR-файлы (только для компиляции, в мод они не попадают —
см. compileOnly/runtimeOnly в build.gradle):

  relics-1.21.1-0.12.8.jar
      Relics — RelicConfigData, включённые в enabledExtendedConfigs
      конфига relics.yaml.

JAR не коммитится (5 МБ стороннего кода). Возьми его из mods/:

  copy ..\..\mods\relics-1.21.1-0.12.8.jar libs\

Без него `gradlew build` не соберётся: классы Relics нужны на этапе компиляции.