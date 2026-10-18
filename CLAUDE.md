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
  singletons (`database`, `turnosRepositorio`, `tareasRepositorio`, `listasRepositorio`, `plantillasRepositorio`,
  `asignacionesRepositorio`,
  `exportadorPdf`).
  ViewModels get dependencies through `viewModelFactory { initializer { ... } }` reading
  `APPLICATION_KEY` plus `createSavedStateHandle()` (see `CalendarioViewModel.Factory`).
- `ui/MisTurnosApp.kt`: Scaffold with a custom bottom bar and a `NavHost` with string routes
  `calendario` (start), `turnos`, `checklist` and the `plantillas_tab` graph (bar order). Tab switches use `saveState`/`restoreState`, so each
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

## Task-preset templates + multi-list Checklist (spec 4 & 4b, stories P2, P10, L0–L8, TASKS Fase 9) — implemented

- Second template type `TipoPlantilla.TAREAS`, items in `tareas_plantilla` (`TareaPlantillaDao`).
  Both types share `PlantillaDetalleScreen` + `PlantillaDetalleViewModel`: the repository
  works on `ElementoPlantilla` (id, plantillaId, texto, orden) and routes each operation by
  type (`elementos`, `agregarElemento`, `cambiarTextoElemento`, `eliminarElemento`,
  `restaurarElemento`); the ViewModel reads the type from the shared template flow. Per-type
  texts are `TipoPlantilla.textos` (`TextosTipoPlantilla`), the badge is `InsigniaTipo`
  (Tareas: `templateTasksBg/Fg`), the avatar is `AvatarElemento` (initials vs. `AvatarNumero`
  with radius 10). Do not fork the screen or the ViewModel for a new type.
- Checklist is multi-list (`ui/checklist/`): `ListasRepositorio.listas` emits all lists plus the
  active one together (`ListasChecklist`; active = `ajustes` `checklist.lista_id` or the first
  by `orden`). `ChecklistViewModel` shares that flow, and the active list's tasks are tagged
  with its id (`flatMapLatest`) so a chip switch never shows the previous list's tasks.
  UI: `FilaChipsListas` (`LazyRow`, active chip filled, "n/N" pill, dashed "+ Nueva"),
  `TarjetaListaActiva` (rename, origin, Reiniciar, Eliminar), `SelectorPlantillaTareas`
  sheet (`HojaNuevaLista.kt`), `DialogoEliminar`. There is no FAB any more.
- Creating from a template COPIES its items as unchecked tasks in ONE transaction
  (`TareaDao.copiarDePlantilla`); `listas.plantilla_id` is informational only
  (`ON DELETE SET NULL`), and the card shows the template's CURRENT name through a LEFT JOIN
  ("Lista en blanco" once it is deleted). Never link `tareas` to `tareas_plantilla`.
- Deleting the active list activates the next one (or the previous if it was last). Undoing a
  task delete after its list is gone is a no-op (`TareaDao.restaurar` is `INSERT … WHERE EXISTS`).
- Shared pieces added to `ui/componentes`: `DialogoEliminar` (filled `danger` "Eliminar", also
  used by the template detail), `EstadoVacioInsignia` (64 dp badge empty state of Checklist and
  Turnos), `CampoEnLinea` + `BotonGuardar` (in-place rename), `Modifier.bordeDiscontinuo`.

## Turnos tab (spec 3b, stories T1–T7, TASKS Fase 8) — implemented

- Second bottom-bar destination (`Destino.TURNOS`, route `turnos`): the team's shifts of the
  active "Trabajadores" template. `ui/turnos/TurnosViewModel` + `TurnosScreen` + `HojasTurnos`.
- Own visible month: `NavegacionMes(handle, clock, prefijo = "turnos")` → keys `turnosMes`,
  `turnosSelectorMesAbierto`, `turnosAnioSelector`. Calendario uses the same class with the
  historical unprefixed keys. Never read or write the `turnos` table from this tab.
- Active template: `AsignacionesRepositorio.plantillaActiva` = the id saved in `ajustes`
  (`turnos.plantilla_id`) or, if missing/deleted, the first template by `orden`.
- Grid: shared `RejillaMes` with `CeldaDiaPuntos` (day number + up to 5 dots + "+N"; on very
  narrow cells it draws fewer dots rather than clipping). Dot colour = `TipoTurno.colorPuntoCelda`
  (the "noche" `dot` token is too light on white, so light mode uses the mockup's #2E2A6B).
- "Equipo del día": ONE `ModalBottomSheet` whose content swaps (`AnimatedContent`) between the
  team list and the shared `ContenidoSelectorTurno` (back arrow + "Quitar turno"). While the
  picker is shown, swipe/scrim (`confirmValueChange`) and Back (`BackHandler`, sheet created
  with `shouldDismissOnBackPress = false`) return to the team list instead of closing.
  Picking a shift saves immediately (`INSERT OR REPLACE`) and returns to the list in edit mode.
- Day, edit mode, selected worker and the template sheet live in SavedStateHandle.
- Deleting a worker returns its assignments so the Plantillas "Deshacer" restores them; the
  delete-template dialog adds "y sus N turnos asignados".

## Plantillas tab (spec 4b, stories P1–P9, TASKS Fase 7) — implemented

- Fourth bottom-bar destination. `Destino.PLANTILLAS.ruta` (`plantillas_tab`) is a nested
  navigation graph with `RutasPlantillas.LISTA` (`plantillas`) and `RutasPlantillas.DETALLE`
  (`plantillas/{id}?renombrar={renombrar}`). The active tab is found through
  `destination.hierarchy`; re-selecting the current tab does nothing, and switching tabs keeps
  the open template (`saveState`/`restoreState`). System Back in the detail pops to the list.
- `ui/plantillas/`: `PlantillasScreen` (cards, empty state, FAB, `SelectorTipoPlantilla`
  sheet) + `PlantillasViewModel` (`hojaTiposAbierta` in SavedStateHandle, `suspend crear(tipo)`
  returns the id); `PlantillaDetalleScreen` + `PlantillaDetalleViewModel` (reads the
  nav args `id`/`renombrar` from SavedStateHandle; drafts are Compose state mirrored to the
  handle; `editandoId`, `renombrando`, `confirmandoBorrado` live in the handle). Deleting the
  template makes `uiState.cerrada` true and the route pops back.
- Type-specific UI lives in `TipoPlantillaUi.kt` (`icono`, `descripcion`, `InsigniaTipo`,
  `AvatarElemento`, `textos`). `TipoPlantilla` has `TRABAJADORES` and `TAREAS`: do not add
  other types until asked (AGENTS.md).
- Shared month pieces are in `ui/componentes`: `Mes.kt` (`CabeceraMes`, `RejillaMes` with a
  cell slot, `CabeceraDiasSemana`, `LeyendaTurnos`, `AccionesSelectorMes`), `SelectorMes.kt`,
  `SelectorTurno.kt` (`ContenidoSelectorTurno`), `NavegacionMes.kt` and `Avatar.kt`.
- Shared list-tab pieces are in `ui/componentes/Pestanas.kt` (`CabeceraPestana`, `CampoAlta`,
  `EstadoVacio`, `EstadoVacioInsignia`, `FabAccion`), used by Checklist, Plantillas and Turnos.
- Avatars (`util/iniciales`) are decorative for accessibility (`clearAndSetSemantics`); the
  row's name is what TalkBack reads.

## Month/year picker (spec 3.1.1, story C2b, TASKS Fase 6) — implemented

- The month title in `CabeceraMes` is a pill button (`BotonSelectorMes`, contentDescription
  "Elegir mes y año") that opens `ui/calendario/SelectorMes.kt`, a `ModalBottomSheet` built
  like `SelectorTurno.kt`: year row with ±1 arrows, 3×4 grid of `meses_abreviados`, and
  "Hoy" / "Cancelar". Arrow navigation (`mesAnterior`/`mesSiguiente`) is unchanged.
- State: `CalendarioUiState.selectorMesAbierto` and `anioSelector`, both in
  `SavedStateHandle` (`KEY_SELECTOR_MES`, `KEY_ANIO_SELECTOR`). `abrirSelectorMes()` sets the
  year to the visible month's; the year arrows change only `anioSelector`.
  `irAMes(anio, mes)` / `irAHoy()` write the same `KEY_MES` as the arrows, so `turnosDelMes`
  reloads by itself, and close the sheet.
- The screen receives the picker callbacks grouped in `AccionesSelectorMes`
  (`AccionesSelectorMes.Vacias` for previews).
- The sheet animates out before running the chosen action (same as `SelectorTurno`).
- Both sheets use the `Sheet` / `SheetButton` palette tokens (dark: sheet = dark surface)
  and the shared `AsaHoja()` handle, and scroll when taller than the screen (landscape).
- Month buttons expose `selected` semantics (active month) and the contentDescription
  "octubre de 2026" (`mes_de_anio`). "Mes de hoy" ring = `TodayRing` token.

## Database rules

- Schema **version 4**, mirroring `mis-turnos-kit/db/schema.sql` name by name:
  v1 `turnos`, `tareas`; v2 adds `plantillas` and `trabajadores` (FK `plantilla_id` →
  `plantillas.id` ON DELETE CASCADE, index `idx_trabajadores_plantilla`); v3 adds
  `asignaciones` (FK `trabajador_id` ON DELETE CASCADE; the SQL `UNIQUE(fecha, trabajador_id)`
  is the unique index `idx_asignaciones_fecha_trabajador`) and `ajustes` (key/value); v4 adds
  `listas` (FK `plantilla_id` ON DELETE SET NULL + index `idx_listas_plantilla`, which
  schema.sql lacks but avoids a full scan on template delete), `tareas_plantilla` (CASCADE,
  `idx_tareas_plantilla`) and `tareas.lista_id` (CASCADE, `idx_tareas_lista(lista_id, orden)`).
- Migrations are kept separate, one per shipped version. 1→2 and 2→3 only create tables;
  `MIGRATION_3_4` also rebuilds `tareas` (temp table + copy + rename, keeping ids, `hecha` and
  `orden`), puts existing tasks in "Mi checklist" (`NOMBRE_LISTA_MIGRADA`) only if there were
  any, and makes it active (`CLAVE_LISTA_CHECKLIST`).
- `turnos.fecha` is ISO `yyyy-MM-dd` text (`LocalDate.toString()`), so `BETWEEN` and ordering
  work on strings. Enum columns (`turnos.tipo`, `plantillas.tipo`) store `.valor` through
  `Conversores`. Room cannot express the SQL `CHECK` constraints; the enum conversion
  enforces them.
- New rows go to the end in one statement (`MAX(orden) + 1`: lists, tasks per list, templates, and items
  per template). Undo re-inserts the deleted row with its id and `orden`.
- Migrations live in `data/local/Migraciones.kt` (`MIGRATION_1_2` … `MIGRATION_3_4`, `TODAS_LAS_MIGRACIONES`).
  Their SQL must equal the `createSql` Room exports in `app/schemas/.../<n>.json`.
- Settings go in `ajustes` (`AjusteDao`), never DataStore/SharedPreferences.
- Any entity change: bump `AppDatabase.VERSION`, add the `Migration` and append it to
  `TODAS_LAS_MIGRACIONES`, rebuild, commit the new JSON, and extend `MigrationTest`.
- `MigrationTestHelper` needs kotlinx-serialization 1.8.x; the app is constrained to it in
  `app/build.gradle.kts` because test classpaths are pinned to the app's runtime versions.
- JVM tests use `testing/Fakes.kt` (in-memory DAOs, including the cascade, +
  `MainDispatcherRule`); real SQLite is covered by the `androidTest/.../data/local` tests.

## Data preservation, signing and backups

- Upgrades keep data only if (1) every schema change has a non-destructive `Migration` (never
  add `fallbackToDestructiveMigration`), (2) the APK is signed with the same key, and
  (3) `versionCode` always increases.
- Release signing: `app/build.gradle.kts` reads `~/.mis-turnos/keystore.properties` (or
  `MIS_TURNOS_KEYSTORE_PROPERTIES`). The key never goes into the repo (`*.jks`,
  `keystore.properties` are git-ignored). Debug builds use the per-machine debug key, so the
  emulator cannot run `connectedDebugAndroidTest` while a release build is installed
  (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`): uninstall it first.
- Android Auto Backup includes the whole `database` domain (db + -wal/-shm). Keep the rules in
  `res/xml/` in sync if new data stores appear (e.g. files).
- Manual backup: `data/backup/` (`FormatoCopia` JSON, versioned independently from the Room
  schema; `CopiaSeguridadRepositorio` with SAF URIs) + `CopiaSeguridadDao` (`leerTodo` /
  `reemplazarTodo`, both `@Transaction`). When a table or column is added, extend the JSON
  (missing arrays must keep reading as empty, so older backups stay valid) and
  `CopiaSeguridadTest`. Bump `FormatoCopia.VERSION` only for incompatible changes. Version 2
  added `listas`, `tareasPlantilla` and `tareas[].listaId`; version-1 files (0.6.0) still
  import, their tasks going to "Mi checklist" like the database migration.

## Conventions

- Kotlin official code style, trailing commas on (see `.editorconfig`).
- Strings in `res/values/strings.xml`, Spanish, exactly as in `spec/SPEC.md`. In composables
  read strings with `stringResource`/`LocalResources`, not `LocalContext` (lint error).
- Touch targets >= 44 dp and every icon-only button has a `contentDescription`.
- Controls that contain text use `heightIn(min = …)`, never a fixed `height(…)`, so they grow
  with large system fonts instead of clipping.
- Launcher icon is an adaptive vector icon (`mipmap-anydpi`), no PNGs.

## Best-practice rules (from the review against the installed Android skills)

- **Atomic multi-step writes**: a repository method that runs more than one DAO write (or a
  read that a later write depends on) wraps them in `Transacciones.ejecutar { }`
  (`data/local/Transacciones.kt`; `TransaccionesRoom` in the app, `SinTransaccion` in JVM tests).
- **Compose stability**: `app/compose_stability.conf` declares `java.time.LocalDate`,
  `YearMonth` and `kotlin.collections.*` stable. Keep UI state as immutable data classes with
  read-only collections; never put mutable collections in UI state.
- **One-shot sheet actions** that create data guard against double taps (see
  `SelectorTipoPlantilla`): a second tap cancels the hide animation and would rerun the action.
- **"Today"** is a `MutableStateFlow` refreshed from the Route with `LifecycleResumeEffect`
  (`refrescarHoy()`); never cache `LocalDate.now()` for the life of a ViewModel.
- **Shared Room flows** used by several derived flows in one ViewModel go through `shareIn`
  (see `TurnosViewModel.catalogo`) instead of subscribing several times.
- **Undo after delete** waits for the delete (`Deferred.await()`) before restoring.
- **Pure layout logic** goes to `util/` with unit tests (e.g. `PuntosDia`), not private in a
  composable file.
- Fakes in `testing/Fakes.kt` mirror SQLite constraints (duplicate PK, foreign keys) and throw.
- Manual DI stays (lazy singletons in `AppContainer`); Hilt is not warranted at this size.
- `local.properties` is machine-specific and git-ignored.
