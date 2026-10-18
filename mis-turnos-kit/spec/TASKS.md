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

## Fase 8 — Pestaña Turnos del equipo (spec 3b, historias T1–T7). Requiere Fase 6 y Fase 7.

### 8.1 Datos
- [x] Entidades Room `Asignacion(id, fecha: String, trabajadorId, tipo: TipoTurno)` con
      `@ForeignKey(Trabajador, onDelete = CASCADE)`, `@Index(value = ["fecha", "trabajador_id"], unique = true)`
      e índice en `trabajador_id`; y `Ajuste(clave: String PK, valor: String)`. Reutilizar `Conversores`
      de `TipoTurno`.
- [x] `AsignacionDao`: `observarMes(plantillaId, desde, hasta): Flow<List<Asignacion>>` (JOIN con
      `trabajadores` por `plantilla_id`), `upsert(asignacion)`, `borrar(fecha, trabajadorId)`,
      `contarDePlantilla(plantillaId)`, `obtenerDeTrabajador(trabajadorId)` (para deshacer).
- [x] `AjusteDao`: `observar(clave): Flow<String?>`, `guardar(Ajuste)`.
- [x] `AppDatabase`: `VERSION = 3`, entidades nuevas, `MIGRATION_2_3` exactamente como `schema.sql`,
      exportar `app/schemas/.../3.json`. Si la versión 2 aún no está en ningún dispositivo, fusionar en
      una sola migración 1 → 2 y documentarlo en `CLAUDE.md`.
  - Nota: no se fusionan; la versión 2 ya se distribuyó en la APK 0.4.0.
- [x] `AsignacionesRepositorio` (incluye `plantillaActiva(): Flow<Plantilla?>` resolviendo
      `turnos.plantilla_id` o la primera disponible) en `di/AppContainer`.
- [x] `TrabajadoresRepositorio.eliminar` devuelve también las asignaciones borradas para que "Deshacer"
      las reinserte; el borrado de plantilla informa del número de asignaciones para el diálogo.
- [x] Tests: `MigrationTest` 2 → 3, `AsignacionDaoTest` (unique por fecha+trabajador, cascade desde
      trabajador y desde plantilla, filtro por plantilla), fakes JVM.

### 8.2 Navegación e iconos
- [x] `Destino.TURNOS("turnos", R.string.tab_turnos, MisTurnosIcons.Turnos)` en segunda posición;
      `composable("turnos") { TurnosRoute() }`. La barra pasa a cuatro ítems de igual ancho; comprobar que
      las etiquetas caben a 1.3× de fuente (si no, reducir a 11 sp solo en la barra).
- [x] Iconos `Turnos` (reloj: `circle 12,12 r9` + `M12 7v5l3 2`) y reutilizar `Trabajadores`, `Lapiz`,
      `ChevronAbajo`.

### 8.3 Calendario del equipo
- [x] `TurnosViewModel`: `mes` (propia clave `KEY_MES_TURNOS`), `diaAbierto`, `editando`,
      `selectorMesAbierto`/`anioSelector` (reutilizar la lógica de Fase 6, idealmente extraída a un
      `SelectorMesState` compartido con `CalendarioViewModel`), `hojaPlantillasAbierta`; `uiState` combina
      plantilla activa, trabajadores y asignaciones del mes en `Map<LocalDate, Map<Long, TipoTurno>>`.
- [x] `TurnosScreen`: cabecera con `CabeceraMes` reutilizada (extraerla de `CalendarioScreen` a
      `ui/componentes`), `ChipPlantillaActiva` + resumen, `RejillaMes` reutilizada con un slot de
      contenido de celda (`CeldaDiaPuntos`: número + hasta 5 puntos + "+N"), leyenda, estado vacío con
      "Ir a Plantillas".
- [x] `SelectorPlantillaActiva` (`ModalBottomSheet`).

### 8.4 Hoja "Equipo del día"
- [x] Refactor previo: generalizar `SelectorTurno.kt` (Calendario) para aceptar `titulo`, `sobretitulo`,
      `turnoActual: TipoTurno?`, `onElegir`, `onCancelar`, `accionSecundaria: (texto, onClick)?` y un
      `onVolver` opcional (flecha). Calendario debe seguir viéndose igual (actualizar su preview).
- [x] `EquipoDelDiaSheet`: una sola `ModalBottomSheet` cuyo contenido alterna entre `EquipoDelDia`
      (cabecera con fecha, subtítulo de recuento, botón Editar/Listo, lista de `FilaTrabajadorTurno` en
      modo chip o botón, botón Cerrar) y el `SelectorTurno` reutilizado para el trabajador seleccionado
      (`trabajadorSeleccionadoId` en SavedStateHandle), con `AnimatedContent` entre ambos.
- [x] Guardado inmediato al elegir una opción (`upsert`) o "Quitar turno" (`borrar`), sin botón Guardar;
      ambas acciones vuelven a `EquipoDelDia` en modo edición.
- [x] Cerrar la hoja (botón, deslizar, scrim, Atrás) resetea `editando = false` y
      `trabajadorSeleccionadoId = null`; Atrás con el selector abierto solo vuelve a `EquipoDelDia`.
- [x] Tests JVM del ViewModel: abrir selector fija el trabajador; elegir turno persiste, actualiza el mapa
      y cierra el selector; "Quitar turno" borra; cancelar no cambia nada; cambiar de plantilla recarga;
      sin plantillas → estado vacío; borrar la plantilla activa → siguiente disponible.

### 8.5 Cierre
- [x] `strings.xml`: "Turnos", "Turnos del equipo", "Elegir plantilla de trabajadores", plural
      "%1$d de %2$d días con turnos", "Equipo del día", plural "%1$d de %2$d asignados", "Editar", "Listo",
      "Cerrar", "Sin asignar", "Turno de %1$s", "Cambiar turno de %1$s, %2$s", "Volver al equipo del día",
      "Quitar turno", "Plantilla de trabajadores", "¿Qué equipo quieres ver?",
      textos del estado vacío e "Ir a Plantillas", y el añadido "y sus %1$d turnos asignados" del diálogo
      de eliminar plantilla.
- [x] Previews con `DatosMuestra` (plantilla "Equipo de planta" y asignaciones de octubre de la maqueta).
- [x] Modo oscuro: puntos con los mismos `dot`; fondo de día con datos = superficie oscura +1 nivel.
- [x] Verificar T1–T7 en emulador y actualizar `CLAUDE.md` (Architecture, Database rules: versión 3).

## Fase 9 — Plantillas de tareas y Checklist con varias listas (spec 4, 4b, historias P2, P10, L0–L8). Requiere Fase 7.

### 9.1 Datos
- [ ] `TipoPlantilla.TAREAS("tareas")`. Entidad `TareaPlantilla(id, plantillaId FK CASCADE, texto, orden)` con
      índice en `plantilla_id`; `TareaPlantillaDao` con las mismas operaciones que `TrabajadorDao`.
- [ ] Entidad `Lista(id, nombre, plantillaId: Long? FK SET NULL, orden)`; `Tarea` gana `listaId` FK CASCADE
      (índice en `lista_id`). `ListaDao`: `observarTodas(): Flow<List<ListaConProgreso>>` (COUNT total y
      COUNT hechas), `insertarAlFinal`, `renombrar`, `borrar`. `TareaDao`: todas las consultas filtran por
      `lista_id` (`observarDeLista`, `agregarAlFinal(listaId, texto)`, `reiniciarLista(listaId)`).
- [ ] `AppDatabase.VERSION = 4` y `MIGRATION_3_4`: crear `tareas_plantilla` y `listas`; si `tareas` tiene
      filas, insertar la lista "Mi checklist" y escribir `checklist.lista_id` en `ajustes`; recrear `tareas`
      con `lista_id` (tabla temporal, `INSERT … SELECT`, `DROP`, `ALTER … RENAME`), copiar los datos y
      recrear el índice `idx_tareas_orden` como `(lista_id, orden)`. Exportar `app/schemas/.../4.json`.
- [ ] `ListasRepositorio.crearDesdePlantilla(plantillaId)` en una transacción: inserta la lista y copia
      `tareas_plantilla` → `tareas` con `hecha = 0` y el mismo `orden`; `crearEnBlanco()`.
      `listaActiva(): Flow<Lista?>` resolviendo `checklist.lista_id` o la primera por `orden`.
- [ ] Tests: `MigrationTest` 3 → 4 con y sin tareas previas (ids, textos, `hecha` y orden intactos),
      `ListaDaoTest` (progreso agregado, cascade de tareas, SET NULL al borrar la plantilla),
      `TareaPlantillaDaoTest`, test del repositorio de copia desde plantilla.

### 9.2 Plantillas: tipo Tareas
- [ ] `SelectorTipoPlantilla` itera `TipoPlantilla.entries` con icono, colores y descripción por tipo
      (insignia #EDE9FF / #4B43B8 para Tareas).
- [ ] Tarjeta de plantilla: insignia y subtítulo según tipo ("Tareas · 5 tareas").
- [ ] `PlantillaDetalleScreen` generalizado con un `TextosTipoPlantilla` (sobretítulo, placeholder,
      plural, aria, estado vacío) y un slot de avatar (iniciales en círculo / número con radio 10 dp);
      `PlantillaDetalleViewModel` recibe el tipo y usa el DAO correspondiente (trabajadores o
      tareas_plantilla). Cuerpo del diálogo de borrado con la frase extra para Tareas.
- [ ] Tests JVM del ViewModel con ambos tipos.

### 9.3 Checklist con listas
- [ ] `ChecklistViewModel`: `listas`, `listaActiva`, `tareas` de la activa, `borrador`, `renombrando`,
      `borradorNombre`, `hojaNuevaAbierta`, `confirmandoBorrado` (SavedStateHandle); acciones `activar(id)`
      (guarda `checklist.lista_id`), `crearDesdePlantilla`, `crearEnBlanco`, `renombrar`, `reiniciar`
      (solo la activa), `pedirBorrar`, `borrarLista` (activa la vecina), y las de tareas ya existentes
      restringidas a la activa.
- [ ] `ChecklistScreen`: `FilaChipsListas` (`LazyRow`, chip activo relleno, píldora n/N, chip "+ Nueva"),
      `TarjetaListaActiva` (nombre editable, origen, botones Reiniciar y Eliminar de 44 dp), alta, lista de
      tareas, estados vacíos (lista vacía / sin listas). **Eliminar el FAB** y su string.
- [ ] `SelectorPlantillaTareas` (`ModalBottomSheet`): plantillas de tipo Tareas + "Lista en blanco" + aviso
      cuando no hay plantillas.
- [ ] `AlertDialog` de eliminar lista con nombre y recuento.
- [ ] Tests JVM: activar persiste, crear desde plantilla copia N tareas sin marcar y activa la nueva,
      reiniciar no toca otras listas, borrar activa la vecina, borrar la última deja estado vacío.

### 9.4 Cierre
- [ ] `strings.xml`: "Nueva", "Nueva lista", "¿Desde qué plantilla de tareas?", "Lista en blanco",
      "Lista %1$d", "Desde la plantilla «%1$s»", "Cambiar nombre de la lista", "Reiniciar las tareas de esta
      lista", "Eliminar esta lista", "¿Eliminar la lista «%1$s»?", plural "Se borrarán sus %1$d tareas y su
      progreso. La plantilla de origen no cambia.", "La lista está vacía.", "Esta lista está vacía",
      "Sin listas todavía", "Crea una lista desde una plantilla de tareas o empieza con una en blanco.",
      "No tienes plantillas de tareas. Puedes crearlas en la pestaña Plantillas.", "Mi checklist",
      "%1$d/%2$d", "Tareas", "Preset de tareas para crear listas en Checklist", "Nueva tarea del preset…",
      plural "%1$d tareas", "Editar tarea", "Eliminar tarea", "Las listas ya creadas en Checklist no se ven
      afectadas." Eliminar "Reiniciar todas las tareas".
- [ ] Previews con `DatosMuestra` (listas "Rutina de apertura" y "Mi checklist", plantillas de tareas
      "Rutina de apertura" y "Cierre de turno").
- [ ] Verificar P2, P10 y L0–L8 en emulador; actualizar `CLAUDE.md` (Architecture, Database rules: versión 4).

