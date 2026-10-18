# Mis turnos (Valanon Calendar)

Aplicación Android nativa para registrar el turno de trabajo de cada día y llevar una
checklist reutilizable, además de plantillas de datos como listas de trabajadores. Escrita en **Kotlin** con **Jetpack Compose** y persistencia local
en **SQLite** a través de **Room**. Todo se guarda en el dispositivo y se conserva al
cerrar la app.

La especificación y el diseño de partida están en `mis-turnos-kit/` (generado con Claude
Design): `spec/SPEC.md`, `spec/USER_STORIES.md`, `design/DESIGN.md` y las maquetas.

## Funcionalidades

- **Calendario**: mes visible con semana en lunes, leyenda, navegación ilimitada entre meses
  y el día de hoy resaltado. Al tocar un día se abre una hoja para elegir Libre, Mañana,
  Tarde, Noche o Vacaciones.
- **Ir a un mes concreto**: al tocar el título del mes se abre una hoja con el año y los 12
  meses para saltar directamente a cualquier mes y año, con un botón "Hoy" para volver al
  mes actual.
- **Exportar a PDF**: genera `turnos-AAAA-MM.pdf` en A4 con la rejilla coloreada y la
  leyenda, y abre la hoja de compartir del sistema.
- **Turnos del equipo**: calendario con los turnos de los compañeros de una plantilla de
  trabajadores. Cada día muestra un punto por persona con el color de su turno. Al tocar un día
  se ve el equipo, y en modo edición se cambia el turno de cada uno con el mismo selector del
  calendario. Tiene su propio mes visible y permite elegir qué plantilla ver.
- **Checklist**: añadir tareas, marcar y desmarcar, borrar con opción de deshacer, contador
  "X de Y hechas" y un botón flotante que desmarca todas sin borrarlas.
- **Plantillas**: listas reutilizables que otras pestañas podrán usar más adelante. De momento
  hay un tipo, **Trabajadores**: una lista de nombres. Se crean con +, se renombran tocando el
  nombre, y los trabajadores se añaden, editan en línea o eliminan con opción de deshacer.
- **Modo oscuro**: sigue el ajuste del sistema y cambia al momento, sin reiniciar la app.
  Usa la paleta oscura de `design/DESIGN.md` y mantiene los colores de turno. El PDF
  exportado sale siempre en claro, pensado para imprimir.

## Inicio rápido

Compilar y ejecutar los tests desde la terminal (macOS, con Android Studio instalado):

```bash
cd /Users/alejandrofuentes/Proyects/valanon_calendar
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un
dispositivo o emulador conectado: `./gradlew installDebug`.

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

Conviene añadir la línea `export JAVA_HOME=...` a `~/.zshrc` para no repetirla. Si no se
quiere tocar la configuración de la shell, se puede pasar en la misma línea:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

Secuencia completa de verificación (es la que debe pasar antes de un commit):

```bash
./gradlew clean assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
```

Si Gradle falla con `Unable to locate a Java Runtime`, falta el `export JAVA_HOME`.

## Estructura

```
app/src/main/kotlin/com/valanon/calendar/
├── CalendarApp.kt                 Application: crea el AppContainer
├── di/AppContainer.kt             Contenedor manual de dependencias
├── data/
│   ├── model/TipoTurno.kt         Los cinco turnos y su valor en BBDD
│   ├── local/                     Room: AppDatabase, migraciones, Conversores, DAOs y entidades
│   └── repository/                TurnosRepositorio y TareasRepositorio
├── pdf/                           ExportadorPdf (PdfDocument) y compartir con FileProvider
├── ui/
│   ├── MainActivity.kt            Única Activity
│   ├── MisTurnosApp.kt            Barra inferior y NavHost (calendario, checklist)
│   ├── calendario/                Pantalla, hoja de selección y ViewModel
│   ├── turnos/                    Calendario del equipo, hoja del día y ViewModel
│   ├── checklist/                 Pantalla y ViewModel
│   ├── plantillas/                Lista, hoja de tipos, detalle de trabajadores y ViewModels
│   ├── componentes/               Piezas compartidas (mes, selectores, campo de alta, avatar…)
│   ├── icons/MisTurnosIcons.kt    Iconos de trazo copiados de las maquetas
│   ├── preview/DatosMuestra.kt    Datos de las maquetas, solo para @Preview
│   └── theme/                     Tokens de color, colores de turno, Manrope y tema
└── util/                          Celdas del mes y textos de fecha en español

app/schemas/                       Esquemas JSON exportados por Room (versionar siempre)
app/src/test/                      Tests unitarios (JVM)
app/src/androidTest/               Tests instrumentados: DAOs, PDF y flujos de interfaz
licenses/Manrope-OFL.txt           Licencia de la fuente incluida
```

## Base de datos (SQLite / Room)

- Archivo `mis_turnos.db` en el almacenamiento privado de la app.
- Esquema idéntico a `mis-turnos-kit/db/schema.sql`, versión 3:
  - `turnos(fecha TEXT PK, tipo TEXT)`, con la fecha en ISO `yyyy-MM-dd` y el tipo en
    minúsculas sin tilde (`libre`, `manana`, `tarde`, `noche`, `vacaciones`).
  - `tareas(id INTEGER PK AUTOINCREMENT, texto TEXT, hecha INTEGER DEFAULT 0, orden INTEGER)`
    con el índice `idx_tareas_orden`.
  - `plantillas(id, tipo, nombre, orden)` y `trabajadores(id, plantilla_id, nombre, orden)`.
    Borrar una plantilla borra sus trabajadores en cascada.
  - `asignaciones(id, fecha, trabajador_id, tipo)`, un turno por trabajador y día, y
    `ajustes(clave, valor)`, que guarda la plantilla activa de Turnos.
- Las migraciones 1→2 y 2→3 solo crean tablas nuevas. Quien actualiza la app conserva sus
  turnos, tareas, plantillas y trabajadores.
- Room no admite restricciones `CHECK`. Los valores de `tipo` los valida el conversor de
  `TipoTurno`, y `hecha` es un `Boolean` que Room guarda como 0 o 1.
- Para cambiar el esquema:
  1. Modificar la entidad.
  2. Subir `AppDatabase.VERSION`.
  3. Añadir una `Migration` y registrarla en `AppDatabase.build()` con `.addMigrations(...)`.
  4. Compilar: Room exporta el nuevo esquema a `app/schemas/`. Hay que incluirlo en el commit.
  5. Ampliar `MigrationTest` con la nueva migración.

## Tests

| Tipo | Comando | Qué cubre |
|---|---|---|
| Unitarios (JVM) | `./gradlew testDebugUnitTest` | Celdas del mes, fechas en español, iniciales, contraste de colores y todos los ViewModels |
| Instrumentados | `./gradlew connectedDebugAndroidTest` | DAOs sobre SQLite real, migraciones 1→2 y 2→3, PDF generado y las historias de Calendario, Turnos, Checklist y Plantillas en la app |

Los instrumentados necesitan un emulador o un móvil conectado. La prueba de interfaz
borra las tablas de la app antes de cada test.

## Stack

| Componente | Versión |
|---|---|
| Android Gradle Plugin | 9.3.1 (Kotlin integrado, sin plugin `kotlin-android`) |
| Kotlin | 2.2.10 (la que incluye AGP) |
| KSP | 2.3.12 |
| Room | 2.8.5 |
| Jetpack Compose BOM | 2026.06.01 (Compose UI 1.11.4, Material 3 1.4.0) |
| Navigation Compose | 2.9.8 |
| AndroidX Core / Lifecycle / Activity | 1.18.0 / 2.10.0 / 1.13.0 |
| Coroutines | 1.11.0 |
| Fuente | Manrope variable (SIL OFL 1.1), en `res/font/manrope.ttf` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36.1 |

### Pasar a API 37

Las versiones más recientes de `core-ktx` (1.19+), `lifecycle` (2.11+), el BOM de Compose
(2026.08+) y Navigation Compose (2.10+) exigen `compileSdk 37`. Para actualizarlas: instalar el platform 37 desde el SDK
Manager, poner `compileSdk = 37` (y quitar `compileSdkMinor`) en `app/build.gradle.kts`
y subir las versiones en `gradle/libs.versions.toml`.
