JAR-файлы в этой папке нужны ТОЛЬКО на этапе компиляции (compileOnly).
В итоговый мод они не попадают — см. build.gradle.

Содержимое:

  create-1.21.1-6.0.10.jar
      Create 6 — нужны сигнатуры KineticBlockEntity / ScrollValueBehaviour / тегов.

  sable-neoforge-1.21.1-2.0.5.jar
  sable-companion-common-1.21.1-1.6.0.jar
      Sable 2.0.x — RotaryConstraintHandle, SubLevelContainer, ServerSubLevel.

  veil-neoforge-1.21.1-4.3.2.jar
      Veil — ServerPacketContext, VeilRenderLevelStageEvent, AdvancedFbo.

  dev.eriksonn.aeronautics.aeronautics-neoforge-1.21.1-1.3.2.jar
  dev.simulated_team.simulated.simulated-neoforge-1.21.1-1.3.2.jar
  dev.ryanhcode.offroad.offroad-neoforge-1.21.1-1.3.2.jar
      Inner-jar'ы из bundle create-aeronautics-bundled-1.21.1-1.3.2.jar
      (Modrinth, Simulated Project). Распаковываются из META-INF/jarjar/.
      Это СТОКОВЫЙ 1.3.2, а не FIXED-сборка: мод-фикс рассчитан именно на него.

Если обновляешь Create Aeronautics — пересобери эти три файла так:

  1. скачай create-aeronautics-bundled-1.21.1-<версия>.jar
  2. распакуй META-INF/jarjar/*.jar в эту папку, удалив старые
