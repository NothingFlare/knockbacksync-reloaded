# KnockbackSync-Reloaded

> [!NOTE]
> This project is a fork of [Axionize/knockback-sync](https://github.com/Axionize/knockback-sync).

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

A high-performance, low-jitter knockback synchronization plugin for Minecraft servers (Paper / Purpur / Folia / Spigot). Designed to eliminate desynchronization between server and client knockback calculations caused by network latency.

## Installation

1. Place `KnockbackSync-1.0.0.jar` into your `plugins/` directory on Paper / Purpur / Spigot / Folia / CraftBukkit (supports Java 17+).
2. Start or restart the server to generate the `config.yml`.
3. To reload the configuration, use `/knockbacksync reload` (available to OP / console).

## Key Features

- **High-Performance Physics**: Uses optimized voxel-based ground detection (`CollisionUtil`) instead of heavy ray-tracing to minimize CPU usage during PvP.
- **Latency Compensation**: Real-time measurement and smoothing of player ping using a median window algorithm to eliminate jitter.
- **Lag Spike Filtering**: Rejects erratic ping fluctuations (outliers) to ensure consistent knockback prediction.
- **Folia & Paper Support**: Full compatibility with modern threaded region architectures and asynchronous scheduling.
- **Asynchronous Processing**: All network and physics calculations are offloaded to dedicated thread pools to ensure zero impact on the main server loop.

## Configuration & Smoothing

The plugin employs an advanced ping compensation mechanism designed to provide fair combat regardless of connection quality.

```yaml
ping:
  offset: 25          # Base latency overhead
  tolerance: 20       # Grace window for jitter
  min_ping: 20        # Threshold to skip compensation
  spike_threshold: 30 # Outlier rejection limit
  sample_size: 5      # Rolling median window size

physics:
  fast_ground_check: true # High-speed voxel check enabled
```

## Commands

| Command | Description | Permission |
|---|---|---|
| `/kbsync ping [player]` | Checks measured & compensated latency | `knockbacksync.ping` |
| `/kbsync status [player]`| Views global & player synchronization status | `knockbacksync.status.self` |
| `/kbsync toggle [player]`| Toggles sync globally or for a specific player | `knockbacksync.toggle.global` |
| `/kbsync reload` | Reloads configuration | `knockbacksync.reload` |

---

# KnockbackSync-Reloaded

> [!NOTE]
> Этот проект форк [Axionize/knockback-sync](https://github.com/Axionize/knockback-sync).

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

Высокопроизводительный плагин для серверов Minecraft (Paper / Purpur / Folia / Spigot), предназначенный для синхронизации отбрасывания (knockback). Устраняет рассинхронизацию между серверными и клиентскими расчетами, вызванную сетевой задержкой.

## Установка

1. Поместите `KnockbackSync-1.0.0.jar` в директорию `plugins/` на Paper / Purpur / Spigot / Folia / CraftBukkit (требуется Java 17+).
2. Запустите или перезапустите сервер для генерации `config.yml`.
3. Для перезагрузки конфигурации используйте `/knockbacksync reload` (доступно для OP / консоли).

## Ключевые особенности

- **Высокая производительность**: Использование оптимизированного воксельного сканирования поверхности (`CollisionUtil`) вместо тяжёлого RayTracing'а для снижения нагрузки на CPU во время PvP.
- **Компенсация задержки**: Измерение и сглаживание пинга в реальном времени с использованием алгоритма медианного окна для устранения «джиттера».
- **Фильтрация лаг-спайков**: Игнорирование резких скачков пинга (выбросов) для обеспечения стабильной предсказательной модели.
- **Поддержка Folia и Paper**: Полная совместимость с современной архитектурой потоковых регионов и асинхронным планированием задач.
- **Асинхронность**: Все сетевые и физические вычисления выполняются в изолированных пулах потоков, не затрагивая главный поток сервера.

## Конфигурация и сглаживание

Плагин использует продвинутый механизм компенсации пинга, обеспечивающий честный PvP-процесс вне зависимости от качества соединения.

```yaml
ping:
  offset: 25          # Базовая сетевая задержка
  tolerance: 20       # Допуск на колебания (джиттер)
  min_ping: 20        # Порог, ниже которого компенсация не применяется
  spike_threshold: 30 # Предел для отсеивания лаг-спайков
  sample_size: 5      # Размер скользящего окна медианы

physics:
  fast_ground_check: true # Включена высокоскоростная проверка поверхности
```

## Команды

| Команда | Описание | Права |
|---|---|---|
| `/kbsync ping [player]` | Проверка измеренного и компенсированного пинга | `knockbacksync.ping` |
| `/kbsync status [player]`| Просмотр статуса синхронизации | `knockbacksync.status.self` |
| `/kbsync toggle [player]`| Включение/отключение (глобально или игроку) | `knockbacksync.toggle.global` |
| `/kbsync reload` | Перезагрузка конфигурации | `knockbacksync.reload` |
