# Mis turnos (Valanon Calendar)

Android app in Kotlin + Jetpack Compose with a local SQLite database through Room: a monthly
shift calendar (with PDF export) and a reusable checklist. Single Gradle module (`:app`),
package `com.valanon.calendar`. UI strings are in Spanish.

The product spec and design live in `mis-turnos-kit/` and are the source of truth for
behaviour, texts and visuals: `AGENTS.md`, `spec/SPEC.md`, `spec/USER_STORIES.md`,
`spec/TASKS.md`, `design/DESIGN.md`, `design/tokens/tokens.json`, `db/schema.sql`.
Do not change the five shift types or their colours, and do not replace Room with
DataStore/SharedPreferences/files (AGENTS.md → "Qué no hacer").

## Build and test

Java is not on PATH; use the JDK bundled with Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug             # build
./gradlew testDebugUnitTest         # JVM unit tests (app/src/test)
./gradlew assembleDebugAndroidTest  # compile instrumented tests (app/src/androidTest)
./gradlew connectedDebugAndroidTest # run them (needs an emulator/device)
./gradlew lintDebug                 # must report 0 errors
```

Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.valanon.calendar.ui.checklist.ChecklistViewModelTest"`.

No AVD is configured on this machine. The only system image installed is
`system-images/android-35/google_apis_playstore_tablet/arm64-v8a`; an AVD with a phone
resolution (1080x2340, 420 dpi) on that image works for `connectedDebugAndroidTest`.

## Toolchain constraints (read before bumping versions)

- AGP 9.3.1 with **built-in Kotlin**: do NOT add `org.jetbrains.kotlin.android`. The Kotlin
  version in `libs.versions.toml` must stay equal to the one AGP bundles (2.2.10) because the
  Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) must match it.
- Gradle 9.5.0 (wrapper). AGP 9.4+ needs Gradle 9.6+.
- Only SDK platform 36.1 is installed, so `compileSdk = 36` + `compileSdkMinor = 1`.
  Library versions are the newest whose `minCompileSdk <= 36`: core-ktx 1.18.0, lifecycle
  2.10.0, compose BOM 2026.06.01, navigation-compose 2.9.8. Newer ones fail the build with
  "requires ... compile against version 37". Upgrading means installing platform 37 and
  setting `compileSdk = 37`.
- Annotation processing is KSP (2.3.12), never kapt. Room compiler runs through `ksp(...)`.
- `minSdk = 26`, so `java.time` is used directly (no core-library desugaring).

## Architecture

```
ui (Compose screens + ViewModels)  ->  data/repository  ->  data/local (Room: DAOs, entities, AppDatabase)
                                   ->  pdf (ExportadorPdf via the GeneradorPdf interface)
```

- `CalendarApp` (Application) owns `di/AppContainer`, a manual DI container with lazy
  singletons (`database`, `turnosRepositorio`, `tareasRepositorio`, `exportadorPdf`).
  ViewModels get dependencies through `viewModelFactory { initializer { ... } }` reading
  `APPLICATION_KEY` plus `createSavedStateHandle()` (see `CalendarioViewModel.Factory`).
- `ui/MisTurnosApp.kt`: Scaffold with a custom bottom bar and a `NavHost` with string routes
  `calendario` (start) and `checklist`. Tab switches use `saveState`/`restoreState`, so each
  tab keeps its ViewModel (visible month, typed draft).
- Screens follow the Route/Screen split: `XRoute` is stateful (ViewModel, snackbar, share
  intent), `XScreen` is stateless and previewable. Previews use `ui/preview/DatosMuestra.kt`;
  production never preloads sample data.
- Visible month and selected day live in `SavedStateHandle`. The checklist draft is Compose
  state in the ViewModel (synchronous for the text field) mirrored to `SavedStateHandle`.
- Visual values come from tokens: `ui/theme/Color.kt`, `ColoresTurno.kt`, `Type.kt`
  (`MisTurnosText`, Manrope variable font). Icons are `ImageVector`s built from the mockups'
  SVG path data (`ui/icons`).
- Dark mode follows the system: `MisTurnosTheme(oscuro = isSystemInDarkTheme())` provides
  `PaletaClara` or `PaletaOscura` through `LocalPaleta`. In composables read colours via
  `MisTurnosColors.X` (composable getters) and `TipoTurno.colores`; never hard-code
  `Color.White`. Use `AccentOnBackground` for accent text/borders/icons on the background
  and `Accent` only for filled surfaces with white content. Outside composition (the PDF,
  always light) use `PaletaClara` and `TipoTurno.coloresClaros()`.
  `PaletaContrasteTest` checks text contrast for both palettes; `res/values-night` sets the
  dark window background so there is no white flash on launch.
- Spanish date texts come from `util/FormatoFechas` (hard-coded names, not device locale);
  month cells from `util/CalendarioMes` (Monday first, leading `null` blanks).

## Database rules

- Schema mirrors `mis-turnos-kit/db/schema.sql` name by name. `turnos.fecha` is ISO
  `yyyy-MM-dd` text (`LocalDate.toString()`), so `BETWEEN` and ordering work on strings.
  `turnos.tipo` stores `TipoTurno.valor` through `Conversores`. Room cannot express the SQL
  `CHECK` constraints; the enum conversion enforces them.
- New tasks go to the end in one statement (`TareaDao.agregarAlFinal`, `MAX(orden) + 1`).
  Undo re-inserts the deleted `Tarea` with its id and `orden`.
- `AppDatabase.VERSION` is the schema version. Any entity change requires: bump the version,
  add a `Migration`, register it in `AppDatabase.build()`, rebuild, and commit the JSON that
  Room exports to `app/schemas/`.
- JVM tests use `testing/Fakes.kt` (in-memory DAOs + `MainDispatcherRule`); real SQLite is
  covered by `androidTest/.../TurnoDaoTest` and `TareaDaoTest`.

## Conventions

- Kotlin official code style, trailing commas on (see `.editorconfig`).
- Strings in `res/values/strings.xml`, Spanish, exactly as in `spec/SPEC.md`. In composables
  read strings with `stringResource`/`LocalResources`, not `LocalContext` (lint error).
- Touch targets >= 44 dp and every icon-only button has a `contentDescription`.
- Launcher icon is an adaptive vector icon (`mipmap-anydpi`), no PNGs.
- `local.properties` is machine-specific and git-ignored.
