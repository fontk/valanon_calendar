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
