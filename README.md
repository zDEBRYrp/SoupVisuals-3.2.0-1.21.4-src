# Soup Visuals — исходники 3.2.0 / Source code 3.2.0

Русская версия

## Описание
Исходный код модификации **Soup Visuals 3.2.0** для Minecraft 1.21.4 (Fabric).

### 🔗 Ссылки
- Страница мода (Modrinth): https://modrinth.com/mod/soup-api/version/3.2.0
- Скачать .jar (Fabric 1.21.4): https://modrinth.com/mod/soup-api?version=1.21.4&loader=fabric#download

---

## Требования
- Minecraft 1.21.4 (Fabric)
- Fabric Loader, совместимый с 1.21.4
- Fabric API (если требуется модом)
- Java JDK 12 (указано по запросу)

> Примечание: если в вашей среде разработки используется другая версия JDK, подстройте её при необходимости. Здесь явно указана JDK 12 по вашему требованию.

## Сборка
Клонируйте репозиторий и соберите проект.

Unix / macOS:

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.2.0-1.21.4-src.git
cd SoupVisuals-3.2.0-1.21.4-src
./gradlew build
```

Windows (PowerShell или cmd.exe):

```powershell
git clone https://github.com/zDEBRYrp/SoupVisuals-3.2.0-1.21.4-src.git
cd SoupVisuals-3.2.0-1.21.4-src
gradlew.bat build
```

Если возникают проблемы с зависимостями, выполните:

```bash
./gradlew --refresh-dependencies
```

Собранный артефакт появится в `build/libs/`. Полученный `.jar` можно поместить в папку `mods/` клиента/сервера.

---

## Установка (для игроков)
1. Установите Fabric Loader и (при необходимости) Fabric API.
2. Скопируйте полученный `SoupVisuals-*.jar` в `minecraft/mods/`.
3. Запустите Minecraft с профилем Fabric.

---

## Разработка / запуск в IDE
- Откройте проект в IntelliJ IDEA или VSCode.
- Импортируйте Gradle-проект.
- Используйте конфигурации `runClient` / `runServer` (Fabric Loom) для тестирования.

---

*Опубликовано автором:* [zDEBRY](https://github.com/zDEBRYrp)

---

English version

# Soup Visuals — source 3.2.0

## Overview
Source code for the Soup Visuals mod version 3.2.0 targeting Minecraft 1.21.4 (Fabric).

### Links
- Mod page (Modrinth): https://modrinth.com/mod/soup-api/version/3.2.0
- Download .jar (Fabric 1.21.4): https://modrinth.com/mod/soup-api?version=1.21.4&loader=fabric#download

---

## Requirements
- Minecraft 1.21.4 (Fabric)
- Fabric Loader compatible with 1.21.4
- Fabric API (if required by the mod)
- Java JDK 12 (specified for building and running in this repository per request)

Note: If your development environment uses a different JDK version, adjust as needed. The repository README explicitly states JDK 12 because you asked to indicate that version.

## Build
Clone the repository and build the project.

Unix / macOS:

```bash
git clone https://github.com/zDEBRYrp/SoupVisuals-3.2.0-1.21.4-src.git
cd SoupVisuals-3.2.0-1.21.4-src
./gradlew build
```

Windows (PowerShell or cmd.exe):

```powershell
git clone https://github.com/zDEBRYrp/SoupVisuals-3.2.0-1.21.4-src.git
cd SoupVisuals-3.2.0-1.21.4-src
gradlew.bat build
```

If you have dependency issues run:

```bash
./gradlew --refresh-dependencies
```

The built artifact will appear in `build/libs/`. Place the resulting `.jar` into the `mods/` folder of the client or server.

---

## Installation (for players)
1. Install Fabric Loader and Fabric API (if required).
2. Copy the `SoupVisuals-*.jar` into your `minecraft/mods/` directory.
3. Launch Minecraft using the Fabric profile.

---

## Development / IDE
- Open the project in IntelliJ IDEA or VSCode.
- Import the Gradle project.
- Use `runClient` / `runServer` (Fabric Loom) configurations for testing.

---

Published by: [zDEBRY](https://github.com/zDEBRYrp)
