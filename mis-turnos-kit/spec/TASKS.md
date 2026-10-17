# Plan de trabajo

Marca cada tarea al completarla. Cada fase debe terminar con la app compilando y ejecutable.

## Fase 0 — Proyecto base
- [ ] Crear proyecto Android (Kotlin, Compose, Material 3), `minSdk 26`, paquete `com.alejandro.misturnos`.
- [ ] Version catalog con Compose BOM, Navigation Compose, Room (runtime, ktx, compiler via KSP), Lifecycle ViewModel Compose.
- [ ] Tema: `Color.kt` y `Theme.kt` a partir de `design/tokens/tokens.json`; tipografía Manrope (`Type.kt`).
- [ ] `Scaffold` con `NavigationBar` de dos destinos y `NavHost` (`calendario` inicial, `checklist`).
- [ ] Iconos de trazo como `ImageVector` propios (calendario, checklist, chevrons, documento-descarga, +, papelera, reiniciar).

## Fase 1 — Datos
- [ ] Entidad `Turno(fecha: String PK, tipo: String)` y `enum class TipoTurno { LIBRE, MANANA, TARDE, NOCHE, VACACIONES }` con conversor a los valores de `db/schema.sql`.
- [ ] Entidad `Tarea(id: Long autogen, texto: String, hecha: Boolean, orden: Int)`.
- [ ] `TurnoDao`: `observarRango(desde, hasta): Flow<List<Turno>>`, `upsert(turno)`, `borrar(fecha)`.
- [ ] `TareaDao`: `observarTodas(): Flow<List<Tarea>>`, `insertar`, `actualizarHecha(id, hecha)`, `borrar(id)`, `reiniciarTodas()`, `maxOrden()`.
- [ ] `AppDatabase` (versión 1, `exportSchema = true`) y `Repositorio`.
- [ ] Tests instrumentados de ambos DAOs con base de datos en memoria.

## Fase 2 — Calendario
- [ ] `CalendarioViewModel`: estado `{ mes: YearMonth, turnos: Map<LocalDate, TipoTurno>, diaSeleccionado: LocalDate? }`.
- [ ] Cálculo de celdas: huecos iniciales = `(primerDia.dayOfWeek.value - 1)`; total días del mes.
- [ ] Composables: `CabeceraMes`, `LeyendaTurnos`, `CabeceraDiasSemana`, `RejillaMes`, `CeldaDia`.
- [ ] `ModalBottomSheet` de selección con las cinco opciones y "Cancelar"; marca "Actual".
- [ ] Botón "Exportar a PDF" (acción cableada en Fase 4).
- [ ] Verificar C1–C4 de `USER_STORIES.md`.

## Fase 3 — Checklist
- [ ] `ChecklistViewModel`: estado `{ borrador: String, tareas: List<Tarea> }`.
- [ ] Campo + botón "Agregar", acción IME Done, validación de texto vacío.
- [ ] `FilaTarea` con `Checkbox`, texto tachado si hecha, botón papelera.
- [ ] FAB "Reiniciar todas las tareas" colocado 20 dp sobre la barra inferior.
- [ ] Estado vacío y contador "X de Y hechas".
- [ ] Verificar L1–L6.

## Fase 4 — PDF
- [ ] `ExportadorPdf`: A4 (595×842 pt), título, rejilla 7 columnas con colores de turno, leyenda al pie.
- [ ] Guardar en `cacheDir/pdf/turnos-AAAA-MM.pdf`, `FileProvider` y `ACTION_SEND`.
- [ ] Snackbar "PDF exportado". Verificar C5.

## Fase 5 — Pulido
- [ ] `contentDescription` en todos los iconos y celdas; targets ≥ 44 dp.
- [ ] Comprobar con tamaño de fuente del sistema al 130 %.
- [ ] Previews de Compose con datos de muestra (los de las maquetas) solo en código de preview.
- [ ] Tema oscuro (opcional): mantener los colores de turno, oscurecer fondos y texto.
- [ ] README del proyecto con cómo compilar e instalar.
