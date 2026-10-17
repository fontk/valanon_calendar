# Valanon Calendar

Aplicación Android nativa escrita en **Kotlin** con interfaz en **Jetpack Compose** y
persistencia local en **SQLite** a través de **Room**.

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| Android Studio | 2026.1 o superior | Incluye el JDK 21 que usa Gradle |
| Android SDK Platform | 36.1 (`platforms/android-36.1`) | Es el que instala Android Studio por defecto |
| Build Tools | 36.0.0 | |
| Gradle | 9.5.0 | Lo descarga el wrapper (`./gradlew`) automáticamente |

El archivo `local.properties` (no versionado) debe apuntar al SDK:

```properties
sdk.dir=/Users/<usuario>/Library/Android/sdk
```

## Abrir en Android Studio

`File > Open…` y seleccionar la carpeta del proyecto. Android Studio usa su propio JDK
y el wrapper de Gradle, no hace falta configurar nada más.

## Compilar desde terminal

No hay Java en el `PATH` del sistema, así que se usa el JDK que trae Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug              # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest          # tests unitarios (JVM)
./gradlew connectedDebugAndroidTest  # tests instrumentados (necesita emulador o dispositivo)
./gradlew installDebug               # instala en el dispositivo conectado
./gradlew lintDebug                  # análisis estático
```

Conviene añadir la línea `export JAVA_HOME=...` a `~/.zshrc` para no repetirla.

## Estructura

```
app/src/main/kotlin/com/valanon/calendar/
├── CalendarApp.kt              Application: crea el AppContainer
├── di/AppContainer.kt          Contenedor manual de dependencias (BBDD, repositorios)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt      @Database de Room (versión, DAOs, migraciones)
│   │   ├── dao/EventDao.kt     Consultas SQL de la tabla events
│   │   └── entity/EventEntity.kt  Tabla events
│   └── repository/EventRepository.kt  API de datos que consume la UI
├── ui/
│   ├── MainActivity.kt         Única Activity; monta Compose
│   ├── events/                 Pantalla de lista de eventos + ViewModel
│   └── theme/                  Material 3 (colores, tipografía, tema)
└── util/DateTimeExt.kt         Conversión LocalDate <-> epoch millis

app/schemas/                    Esquemas JSON exportados por Room (versionar siempre)
app/src/test/                   Tests unitarios (JVM, sin Android)
app/src/androidTest/            Tests instrumentados (Room en memoria sobre SQLite real)
gradle/libs.versions.toml       Catálogo de versiones de plugins y librerías
```

## Base de datos (SQLite / Room)

- Archivo: `valanon_calendar.db` en el almacenamiento privado de la app.
- Tabla inicial `events` (`id`, `title`, `description`, `start_at`, `end_at`, `all_day`, `created_at`).
  Las fechas se guardan como *epoch millis* (UTC) para ordenar y filtrar por rango con índices.
- Para cambiar el esquema:
  1. Modificar la entidad.
  2. Subir `AppDatabase.VERSION`.
  3. Añadir una `Migration` y registrarla en `AppDatabase.build()` con `.addMigrations(...)`.
  4. Compilar: Room exporta el nuevo esquema a `app/schemas/`. Hay que incluirlo en el commit.
- `androidx.room:room-testing` ya está en `androidTest` para probar migraciones con `MigrationTestHelper`.

## Stack

| Componente | Versión |
|---|---|
| Android Gradle Plugin | 9.3.1 (Kotlin integrado, sin plugin `kotlin-android`) |
| Kotlin | 2.2.10 (la que incluye AGP) |
| KSP | 2.3.12 |
| Room | 2.8.5 |
| Jetpack Compose BOM | 2026.06.01 (Compose UI 1.11.4, Material 3 1.4.0) |
| AndroidX Core / Lifecycle / Activity | 1.18.0 / 2.10.0 / 1.13.0 |
| Coroutines | 1.11.0 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36.1 |

### Pasar a API 37

Las versiones más recientes de `core-ktx` (1.19+), `lifecycle` (2.11+) y el BOM de Compose
(2026.08+) exigen `compileSdk 37`. Para actualizarlas: instalar el platform 37 desde el SDK
Manager, poner `compileSdk = 37` (y quitar `compileSdkMinor`) en `app/build.gradle.kts`
y subir las versiones en `gradle/libs.versions.toml`.
