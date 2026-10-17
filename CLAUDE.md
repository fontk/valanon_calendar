# Valanon Calendar

Android app in Kotlin + Jetpack Compose with a local SQLite database through Room.
Single Gradle module (`:app`), package `com.valanon.calendar`. UI strings are in Spanish.

## Build and test

Java is not on PATH; use the JDK bundled with Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug             # build
./gradlew testDebugUnitTest         # JVM unit tests (app/src/test)
./gradlew assembleDebugAndroidTest  # compile instrumented tests (app/src/androidTest)
./gradlew connectedDebugAndroidTest # run them (needs an emulator/device)
./gradlew lintDebug
```

Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.valanon.calendar.data.repository.EventRepositoryTest"`.

## Toolchain constraints (read before bumping versions)

- AGP 9.3.1 with **built-in Kotlin**: do NOT add `org.jetbrains.kotlin.android`. The Kotlin
  version in `libs.versions.toml` must stay equal to the one AGP bundles (2.2.10) because the
  Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) must match it.
- Gradle 9.5.0 (wrapper). AGP 9.4+ needs Gradle 9.6+.
- Only SDK platform 36.1 is installed, so `compileSdk = 36` + `compileSdkMinor = 1`.
  Library versions are the newest whose `minCompileSdk <= 36`: core-ktx 1.18.0, lifecycle
  2.10.0, compose BOM 2026.06.01. Newer ones fail the build with "requires ... compile
  against version 37". Upgrading them means installing platform 37 and setting `compileSdk = 37`.
- Annotation processing is KSP (2.3.12), never kapt. Room compiler runs through `ksp(...)`.
- Material 3 1.4 does not pull `material-icons-core`; it is an explicit dependency.
- `minSdk = 26`, so `java.time` is used directly (no core-library desugaring).

## Architecture

```
ui (Compose screens + ViewModels)  ->  data/repository  ->  data/local (Room: DAO, entities, AppDatabase)
```

- `CalendarApp` (Application) owns `di/AppContainer`, a manual DI container with lazy
  singletons (`database`, `eventRepository`). ViewModels get dependencies through
  `viewModelFactory { initializer { ... } }` reading `APPLICATION_KEY` (see `EventsViewModel.Factory`).
- Screens follow the Route/Screen split: `XRoute` is stateful (collects the ViewModel),
  `XScreen` is stateless and previewable.
- Repositories expose `Flow` for observation and `suspend` functions for writes. UI never
  touches DAOs.
- Dates: entities store epoch millis (UTC). Convert with `util/DateTimeExt.kt`
  (`LocalDate.toEpochMillisAtStartOfDay`, `Long.toLocalDate`). All-day events span
  `[start of day, start of next day)` in the device zone.

## Database rules

- `AppDatabase.VERSION` is the schema version. Any entity change requires: bump the version,
  add a `Migration`, register it in `AppDatabase.build()`, rebuild, and commit the JSON that
  Room exports to `app/schemas/`.
- Queries use half-open ranges `[from, to)` on `start_at`, which is indexed.
- JVM tests use `FakeEventDao` (in-memory `EventDao`); real SQLite behaviour is covered by
  `androidTest/.../EventDaoTest` with `Room.inMemoryDatabaseBuilder`.

## Conventions

- Kotlin official code style, trailing commas on (see `.editorconfig`).
- Resources: strings in `res/values/strings.xml` (Spanish). Launcher icon is an adaptive
  icon built from vector drawables (`mipmap-anydpi`), no PNGs.
- `local.properties` is machine-specific and git-ignored.
