# AGENTS.md — instrucciones para el agente implementador

## Objetivo

Implementar la app Android **Mis turnos** descrita en `spec/SPEC.md`, reproduciendo con fidelidad el diseño
de `design/mockups/` y `design/DESIGN.md`. El resultado debe compilar, instalarse y funcionar en un
dispositivo Android real.

## Stack (fijado)

- Kotlin, Jetpack Compose, Material 3, Navigation Compose (dos destinos: `calendario`, `checklist`).
- Room (SQLite) para persistencia. Esquema en `db/schema.sql`; respétalo nombre a nombre.
- ViewModel + StateFlow. Sin librerías de inyección obligatorias (Hilt opcional).
- `minSdk 26` (para `java.time`), `targetSdk` el último estable. Gradle con Kotlin DSL y version catalog.
- PDF con `android.graphics.pdf.PdfDocument`; compartir con `FileProvider` + `Intent.ACTION_SEND` (o
  guardar en Descargas con `MediaStore`). Sin librerías de terceros para el PDF.
- Fuente: Manrope (descargar de Google Fonts y añadir a `res/font/`, o usar Downloadable Fonts). Si no es
  posible, `FontFamily.SansSerif`, nunca Roboto por defecto sin ajustar pesos.

## Orden de trabajo

Sigue `spec/TASKS.md` fase a fase. Resumen:

1. Proyecto vacío que compila, con tema (colores de `design/tokens/tokens.json`) y barra inferior con las
   cinco pestañas navegando (Calendario, Turnos, Checklist, Plantillas, Ajustes).
2. Capa de datos Room: entidades `Turno` y `Tarea`, DAOs, base de datos, repositorio. Tests de DAO.
3. Pestaña Calendario completa (grid, leyenda, navegación de mes, hoja de selección de turno, persistencia).
4. Pestaña Checklist completa (alta, marcar/desmarcar, borrar, FAB reiniciar, estado vacío).
5. Exportar a PDF.
6. Pulido: accesibilidad, rotación, tema oscuro opcional, pruebas de UI básicas.

Haz commits pequeños por tarea. No pases a la siguiente fase sin que la actual compile y se pueda probar.

## Cómo leer las maquetas

- `design/mockups/Main.dc.html`: pestaña Calendario. El `<div>` raíz es la pantalla a 390×844 px.
  Cada estilo inline es la medida de referencia (paddings, radios, tamaños de fuente en px; conviértelos a
  dp 1:1 y sp 1:1).
- `design/mockups/Selector.dc.html`: mismo calendario con la hoja inferior de selección de turno abierta.
- `design/mockups/SelectorMes.dc.html`: mismo calendario con la hoja de selección de mes y año abierta
  (se abre tocando el título del mes, que es un botón).
- `design/mockups/Checklist.dc.html`: pestaña Checklist.
- `design/mockups/Plantillas.dc.html`, `PlantillasTipo.dc.html`, `PlantillaDetalle.dc.html`: pestaña
  Plantillas en sus tres estados (lista, hoja de tipo, detalle de trabajadores). Es un único componente
  con la prop `pantallaInicial`; el script del final contiene el flujo completo (crear, renombrar, añadir,
  editar en línea, eliminar) como pseudocódigo.
- `design/mockups/Turnos.dc.html`, `TurnosVacio`, `TurnosPlantilla`, `TurnosGrande`, `TurnosDia`,
  `TurnosEditar`, `TurnosSelector` (`.dc.html`): pestaña Turnos en todos sus estados (calendario del
  equipo, sin plantillas, hoja de elección de plantilla, equipo de 7 con "+N", hoja "Equipo del día" en
  lectura y en edición, y el selector de turno de un trabajador, que es el mismo selector de Calendario
  reutilizado). Mismo patrón de un componente con `pantallaInicial`.
- `PlantillasVacio`, `PlantillaEditar`, `PlantillaBorrar` y `PlantillaTareas` (`.dc.html`) completan
  Plantillas: lista vacía, edición en línea de un trabajador, diálogo de confirmación al eliminar la
  plantilla y el detalle de una plantilla de tipo Tareas (preset).
- `design/mockups/Checklist.dc.html`, `ChecklistNueva`, `ChecklistBorrar`, `ChecklistVacio`: la pestaña
  Checklist con varias listas (chips de lista, tarjeta de la lista activa con Reiniciar y Eliminar, hoja
  "Nueva lista" desde una plantilla de tareas, diálogo de eliminar lista, estado sin listas). Sustituye al
  diseño anterior de una sola lista con FAB.
- `design/mockups/Ajustes.dc.html`, `AjustesTipos`, `AjustesTipoEditar`, `AjustesTipoNuevo`,
  `AjustesTipoBorrar`: pestaña Ajustes y el ajuste "Tipos de turno" (lista, editor con paleta de 16
  colores y vista previa, diálogo de eliminar con usos). El script del `Ajustes.dc.html` contiene la
  paleta y las cinco filas por defecto exactamente como deben quedar en código y en la migración.
- El bloque `<script type="text/x-dc">` del final de cada fichero contiene la lógica de la maqueta en
  JavaScript (cálculo de celdas del mes, mapa de turnos, alta/borrado de tareas). Úsalo como pseudocódigo
  del comportamiento esperado, no lo portes literalmente.
- Las etiquetas `<sc-for>`, `<sc-if>` y los `{{huecos}}` son del formato de maqueta: equivalen a bucles,
  condicionales y bindings.

## Criterios de aceptación globales

- Al cerrar y reabrir la app, los turnos marcados y las tareas (con su estado) se conservan.
- Todos los targets táctiles miden al menos 44×44 dp; todos los botones de solo icono tienen
  `contentDescription`.
- Textos en español exactamente como figuran en `spec/SPEC.md`.
- Sin datos de ejemplo precargados en producción (los turnos de octubre y las cinco tareas de la maqueta
  son relleno de muestra; pueden usarse solo en previews de Compose o en un flavor de debug).

## Qué no hacer

- Los tipos de turno son datos (`tipos_turno`), no un enum: nunca hardcodear nombres, abreviaturas ni
  colores de turno fuera de las cinco filas por defecto de la migración y de `PaletaTurnos`. No cambiar
  esos valores por defecto ni la paleta sin pedir confirmación.
- No sustituir SQLite/Room por DataStore, SharedPreferences ni archivos JSON.
- No añadir inicio de sesión, sincronización en la nube ni notificaciones: fuera de alcance.
- No añadir tipos de plantilla distintos de Trabajadores y Tareas. Turnos consume plantillas de
  trabajadores (asignaciones por día) y Checklist consume plantillas de tareas (crea listas copiando el
  preset); no conectar plantillas a Calendario sin que se pida.
- Las tareas de una lista son copias del preset: nunca enlazar `tareas` con `tareas_plantilla`; la única
  referencia es `listas.plantilla_id` (informativa, `ON DELETE SET NULL`).
- Sin datos de ejemplo precargados: la lista "Mi checklist" solo se crea en la migración cuando la
  instalación ya tenía tareas.
- La pestaña Turnos nunca escribe en la tabla `turnos` (los turnos del propio usuario); usa `asignaciones`.
- La preferencia de plantilla activa va en la tabla `ajustes` de Room, nunca en DataStore/SharedPreferences.
