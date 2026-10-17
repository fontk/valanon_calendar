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
   dos pestañas navegando.
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

- No cambiar los cinco tipos de turno ni sus colores sin pedir confirmación.
- No sustituir SQLite/Room por DataStore, SharedPreferences ni archivos JSON.
- No añadir inicio de sesión, sincronización en la nube ni notificaciones: fuera de alcance.
