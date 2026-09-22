Положи сюда JAR-файлы (только для компиляции, в мод они не попадают):

  meowhex.jar                               (порт Hex Casting)
  sable-neoforge-1_21_1-2_0_5.jar           (Sable)
  sable-companion-common-1.21.1-1.6.0.jar   (лежит ВНУТРИ jar-а Sable: META-INF/jarjar/, достань оттуда)

Если Gradle начнёт ругаться на отсутствующие классы Veil, достань оттуда же
META-INF/jarjar/veil-neoforge-*.jar и положи сюда тоже.
