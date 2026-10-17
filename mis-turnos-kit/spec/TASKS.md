# Plan de trabajo

Marca cada tarea al completarla. Cada fase debe terminar con la app compilando y ejecutable.

## Fase 0 — Proyecto base
- [x] Crear proyecto Android (Kotlin, Compose, Material 3), `minSdk 26`, paquete `com.alejandro.misturnos`.
  - Nota: se reutiliza el proyecto existente, con paquete `com.valanon.calendar`.
- [x] Version catalog con Compose BOM, Navigation Compose, Room (runtime, ktx, compiler via KSP), Lifecycle ViewModel Compose.
- [x] Tema: `Color.kt` y `Theme.kt` a partir de `design/tokens/tokens.json`; tipografía Manrope (`Type.kt`).
- [x] `Scaffold` con `NavigationBar` de dos destinos y `NavHost` (`calendario` inicial, `checklist`).
- [x] Iconos de trazo como `ImageVector` propios (calendario, checklist, chevrons, documento-descarga, +, papelera, reiniciar).

## Fase 1 — Datos
- [x] Entidad `Turno(fecha: String PK, tipo: String)` y `enum class TipoTurno { LIBRE, MANANA, TARDE, NOCHE, VACACIONES }` con conversor a los valores de `db/schema.sql`.
- [x] Entidad `Tarea(id: Long autogen, texto: String, hecha: Boolean, orden: Int)`.
- [x] `TurnoDao`: `observarRango(desde, hasta): Flow<List<Turno>>`, `upsert(turno)`, `borrar(fecha)`.
- [x] `TareaDao`: `observarTodas(): Flow<List<Tarea>>`, `insertar`, `actualizarHecha(id, hecha)`, `borrar(id)`, `reiniciarTodas()`, `maxOrden()`.
- [x] `AppDatabase` (versión 1, `exportSchema = true`) y `Repositorio`.
- [x] Tests instrumentados de ambos DAOs con base de datos en memoria.

## Fase 2 — Calendario
- [x] `CalendarioViewModel`: estado `{ mes: YearMonth, turnos: Map<LocalDate, TipoTurno>, diaSeleccionado: LocalDate? }`.
- [x] Cálculo de celdas: huecos iniciales = `(primerDia.dayOfWeek.value - 1)`; total días del mes.
- [x] Composables: `CabeceraMes`, `LeyendaTurnos`, `CabeceraDiasSemana`, `RejillaMes`, `CeldaDia`.
- [x] `ModalBottomSheet` de selección con las cinco opciones y "Cancelar"; marca "Actual".
- [x] Botón "Exportar a PDF" (acción cableada en Fase 4).
- [x] Verificar C1–C4 de `USER_STORIES.md`.

## Fase 3 — Checklist
- [x] `ChecklistViewModel`: estado `{ borrador: String, tareas: List<Tarea> }`.
- [x] Campo + botón "Agregar", acción IME Done, validación de texto vacío.
- [x] `FilaTarea` con `Checkbox`, texto tachado si hecha, botón papelera.
- [x] FAB "Reiniciar todas las tareas" colocado 20 dp sobre la barra inferior.
- [x] Estado vacío y contador "X de Y hechas".
- [x] Verificar L1–L6.

## Fase 4 — PDF
- [x] `ExportadorPdf`: A4 (595×842 pt), título, rejilla 7 columnas con colores de turno, leyenda al pie.
- [x] Guardar en `cacheDir/pdf/turnos-AAAA-MM.pdf`, `FileProvider` y `ACTION_SEND`.
- [x] Snackbar "PDF exportado". Verificar C5.

## Fase 5 — Pulido
- [x] `contentDescription` en todos los iconos y celdas; targets ≥ 44 dp.
- [x] Comprobar con tamaño de fuente del sistema al 130 %.
- [x] Previews de Compose con datos de muestra (los de las maquetas) solo en código de preview.
- [x] Tema oscuro (opcional): mantener los colores de turno, oscurecer fondos y texto.
  - Sigue el modo oscuro del sistema. El PDF se genera siempre en claro para imprimir.
- [x] README del proyecto con cómo compilar e instalar.

## Fase 6 — Selector de mes y año (spec 3.1.1, historia C2b)
- [x] `strings.xml`: "Elegir mes y año", "Ir a mes", "Año anterior", "Año siguiente", "Hoy", abreviaturas
      de mes (array `meses_abreviados`) y plantilla "%1$s de %2$d" para el `contentDescription` de cada mes.
- [x] `CalendarioViewModel`: estado `selectorMesAbierto: Boolean` y `anioSelector: Int` en
      `SavedStateHandle`; métodos `abrirSelectorMes()` (fija `anioSelector` al año del mes visible),
      `cerrarSelectorMes()`, `anioSelectorAnterior()`, `anioSelectorSiguiente()`, `irAMes(anio, mes)` e
      `irAHoy()`. Exponerlos en `CalendarioUiState`.
- [x] `CabeceraMes`: sustituir el `Text` del título por un botón píldora (44 dp, fondo `surface`, texto
      20 sp/800 + icono chevron-abajo 18 dp) con `contentDescription` "Elegir mes y año". Añadir
      `MisTurnosIcons.ChevronAbajo` (path `M6 9l6 6 6-6`).
- [x] Nuevo composable `SelectorMes.kt` (mismo patrón que `SelectorTurno.kt`): `ModalBottomSheet` con
      sobretítulo, fila de año, `LazyVerticalGrid`/`Column` de 4 filas × 3 meses y fila "Hoy" / "Cancelar".
      Estados visuales de cada mes según spec (activo / hoy / resto). Debe funcionar en `PaletaOscura`.
- [x] Conectar en `CalendarioRoute`/`CalendarioScreen`; `irAMes` debe reutilizar la misma clave `KEY_MES`
      que `cambiarMes` para que `turnosDelMes` recargue solo.
- [x] Tests JVM de `CalendarioViewModel`: abrir fija el año, las flechas cambian solo `anioSelector`,
      `irAMes` cambia el mes visible y cierra, `irAHoy` vuelve a `YearMonth.now()`.
- [x] Preview de `SelectorMes` con datos de muestra y verificación manual de C2b en emulador.
- [x] Actualizar `CLAUDE.md` (sección Architecture) si cambia la forma de exponer el estado.

## Fase 7 — Pestaña Plantillas (spec 4b, historias P1–P9)

### 7.1 Datos
- [x] `db/schema.sql` ya incluye `plantillas` y `trabajadores`; crear entidades Room `Plantilla(id, tipo,
      nombre, orden)` y `Trabajador(id, plantillaId, nombre, orden)` con `@ForeignKey(onDelete = CASCADE)`
      e índice en `plantilla_id`. `enum class TipoPlantilla(val valor: String) { TRABAJADORES("trabajadores") }`
      + conversor.
- [x] `PlantillaDao`: `observarTodas(): Flow<List<PlantillaConRecuento>>` (JOIN con COUNT de
      trabajadores), `observar(id)`, `insertarAlFinal(tipo, nombre)` (MAX(orden)+1), `renombrar(id, nombre)`,
      `borrar(id)`, `contar()`.
- [x] `TrabajadorDao`: `observarDePlantilla(plantillaId): Flow<List<Trabajador>>`, `agregarAlFinal`,
      `renombrar(id, nombre)`, `borrar(id)`, `insertar(trabajador)` (para deshacer con id y orden).
- [x] `AppDatabase`: `VERSION = 2`, `entities += Plantilla, Trabajador`, `MIGRATION_1_2` que crea ambas tablas
      e índices exactamente como `schema.sql`; registrar en `build()`; exportar `app/schemas/.../2.json`.
- [x] `PlantillasRepositorio` en `di/AppContainer`.
- [x] Tests: `MigrationTest` (1 → 2 con `MigrationTestHelper`, datos de turnos/tareas intactos),
      `PlantillaDaoTest`, `TrabajadorDaoTest` (cascade al borrar plantilla). Fakes JVM en `testing/Fakes.kt`.

### 7.2 Navegación
- [x] `Destino.PLANTILLAS("plantillas", R.string.tab_plantillas, MisTurnosIcons.Plantillas)` como tercer
      elemento; `NavHost` con `composable("plantillas")` → `PlantillasRoute()`.
- [x] El detalle es navegación interna de la pestaña (ruta `plantillas/{id}` dentro de un grafo anidado
      `plantillas_graph`, o estado del ViewModel con `BackHandler`); en ambos casos el Atrás del sistema
      vuelve a la lista y el cambio de pestaña conserva la plantilla abierta.
- [x] Icono `MisTurnosIcons.Plantillas` (capas), `Trabajadores` (dos personas), `Lapiz`, `FlechaAtras`,
      `Cerrar` a partir de los paths de `design/mockups/Plantillas.dc.html`.

### 7.3 Lista y alta
- [x] `PlantillasViewModel`: `plantillas: StateFlow<List<PlantillaConRecuento>>`, `hojaTiposAbierta`
      (SavedStateHandle), `crear(tipo)` que devuelve el id creado para navegar al detalle con
      `renombrando = true`.
- [x] `PlantillasScreen`: cabecera con contador, tarjetas, estado vacío, FAB "Añadir plantilla".
- [x] `SelectorTipoPlantilla` (`ModalBottomSheet`): una opción por `TipoPlantilla.entries`, "Cancelar".

### 7.4 Detalle de trabajadores
- [x] `PlantillaTrabajadoresViewModel(plantillaId)`: `plantilla`, `trabajadores`, `borrador`,
      `editandoId`, `borradorEdicion`, `renombrando`, `borradorNombre`, `confirmandoBorrado`; acciones
      `agregar`, `empezarEdicion`, `guardarEdicion`, `cancelarEdicion`, `eliminar` (+ `deshacer`),
      `empezarRenombrar`, `guardarNombre`, `pedirBorrarPlantilla`, `borrarPlantilla`.
- [x] `PlantillaTrabajadoresScreen`: barra con volver + nombre editable, alta, etiqueta de recuento,
      `FilaTrabajador` (lectura / edición), estado vacío, botón "Eliminar plantilla" + `AlertDialog`,
      Snackbar con "Deshacer".
- [x] Iniciales: primera letra del primer y del último token del nombre, en mayúsculas (`util/Iniciales`).
- [x] Tests JVM del ViewModel: alta ignora vacíos, solo una fila en edición, guardar vacío conserva el
      nombre, eliminar + deshacer restaura orden, borrar plantilla navega atrás.

### 7.5 Cierre
- [x] `strings.xml` con todos los textos de spec 4b (incluidos plurales de plantillas y trabajadores).
- [x] Previews con `DatosMuestra` (dos plantillas, nombres de la maqueta) solo en preview.
- [x] Modo oscuro: insignias con `surfaceAccent` oscuro, avatares con `surface` oscuro.
- [x] Verificar P1–P9 en emulador y actualizar `CLAUDE.md` (Architecture y Database rules: versión 2).

