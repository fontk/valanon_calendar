# SPEC — Mis turnos (Android)

## 1. Resumen

App personal para registrar el turno de trabajo de cada día en un calendario y llevar una checklist de
tareas reutilizable. Cuatro pestañas en una barra inferior, en este orden: **Calendario** (destino
inicial, los turnos del propio usuario), **Turnos** (los turnos de los compañeros de una plantilla de
trabajadores, día a día), **Checklist** (varias listas de tareas, creadas desde plantillas de tareas o en
blanco) y **Plantillas** (datos reutilizables por otras pestañas: trabajadores y presets de tareas). Todos los datos se guardan en SQLite (Room) y persisten entre sesiones.

## 2. Navegación

- Barra inferior de 72 dp con cuatro pestañas de igual ancho, en este orden: Calendario, Turnos,
  Checklist, Plantillas. La pestaña activa muestra el icono dentro de una píldora
  (fondo `surfaceAccent` #DDF0EC) y texto en `accent` con peso 800; la inactiva en `textMuted` #5F5F5A,
  peso 700. Etiquetas: "Calendario" y "Checklist".
- Iconos de trazo (stroke 2 dp): calendario (rectángulo con dos anillas y línea de cabecera), turnos
  (reloj: círculo con manecillas), checklist (rectángulo con marca de verificación) y plantillas (tres
  capas apiladas: rombo arriba y dos arcos debajo). Nunca emoji.
- Cambiar de pestaña conserva el estado de cada una (mes visible de Calendario y de Turnos, texto escrito
  en el campo de nueva tarea, plantilla abierta en Plantillas). Calendario y Turnos tienen cada uno su
  propio mes visible.

## 3. Pestaña Calendario

### 3.1 Cabecera
- Sobretítulo "MIS TURNOS" (12 sp, peso 700, mayúsculas, tracking 0.08em, color `textMuted`).
- Fila con botón circular "Mes anterior" (44 dp, borde `line` #E6E6E1), el **selector de mes y año**
  centrado y botón "Mes siguiente".
- El selector de mes y año es un botón píldora (44 dp de alto, padding horizontal 14, radio 22, fondo
  `surface` #F2F2EF) con el texto "Octubre 2026" (mes con inicial mayúscula + año, 20 sp, peso 800) y un
  chevron hacia abajo de 18 dp en `textMuted`. `contentDescription`: "Elegir mes y año". Al tocarlo se
  abre la hoja descrita en 3.1.1.
- La navegación de mes es ilimitada hacia delante y hacia atrás, tanto con las flechas como con el selector.

### 3.1.1 Selector de mes y año (hoja inferior)
- `ModalBottomSheet` con asa, sobretítulo "IR A MES".
- Fila de año: botón circular "Año anterior" (44 dp, borde `line`), año centrado (24 sp, peso 800) y botón
  "Año siguiente". El año mostrado al abrir es el del mes visible; las flechas cambian solo el año
  mostrado en la hoja, no el mes activo.
- Rejilla de 12 meses en 3 columnas (gap 8): botones de 52 dp, radio 14, texto con las tres primeras letras
  del mes en mayúsculas (ENE, FEB, MAR, ABR, MAY, JUN, JUL, AGO, SEP, OCT, NOV, DIC), 15 sp, peso 700.
  `contentDescription` de cada uno: "octubre de 2026".
  - Mes activo (el visible en el calendario, solo si el año mostrado coincide): fondo `accent`, texto
    blanco, borde 2 dp `accent`.
  - Mes de hoy (si el año mostrado es el actual y no es el activo): borde 2 dp #B9CFCB, fondo blanco.
  - Resto: fondo blanco, borde 1 dp `line`.
- Tocar un mes activa `YearMonth(añoMostrado, mes)` como mes visible y cierra la hoja. Los turnos del mes
  elegido se cargan al instante (misma consulta por rango que la navegación con flechas).
- Fila inferior con dos botones de 48 dp, radio 14, a partes iguales (gap 8): "Hoy" (borde 1 dp `accent`,
  texto `accent`, fondo blanco) que activa el mes actual del dispositivo y cierra la hoja, y "Cancelar"
  (fondo `surface`) que cierra sin cambios.
- Cerrar deslizando o tocando el scrim equivale a "Cancelar".
- El estado "hoja abierta" y el año mostrado sobreviven a la rotación (SavedStateHandle).

### 3.2 Leyenda
Cinco chips en fila con salto de línea, cada uno con punto de color de 8 dp + etiqueta (12 sp, 700):
Libre, Mañana, Tarde, Noche, Vacaciones. Colores en `design/tokens/tokens.json` → `shift`.

### 3.3 Rejilla del mes
- Cabecera de días: L M X J V S D (12 sp, 700, `textFaint` #7A7A75). Semana empieza en lunes.
- Celdas de 60 dp de alto, radio 12 dp, gap 6 dp, 7 columnas, margen lateral 16 dp. Las celdas vacías
  previas al día 1 se dibujan invisibles pero ocupan su hueco.
- Cada celda muestra el número del día (15 sp, 800) y debajo la abreviatura del turno (10 sp, 700,
  tracking 0.04em): LIBRE, MAÑ, TARDE, NOCHE, VAC. Sin turno: fondo blanco, borde `lineSoft` #E8E8E4,
  sin abreviatura.
- Colores por turno (fondo / texto / borde):
  - libre: #F2F2EF / #5B5B57 / #E4E4E0
  - manana: #FFE9A8 / #6B4A00 / #F3D77A
  - tarde: #CFE4FF / #12407A / #A9CBF5
  - noche: #2E2A6B / #FFFFFF / #2E2A6B
  - vacaciones: #CDEFD5 / #145229 / #A6DDB4
- El día de hoy lleva borde de 2 dp en `accent`, por encima del borde de su turno.
- `contentDescription` de cada celda: "17 de octubre, Noche" o "17 de octubre, sin turno".

### 3.4 Selección de turno (hoja inferior)
- Al tocar un día se abre un `ModalBottomSheet` con asa, sobretítulo "TURNO DEL DÍA" y título con la fecha
  en formato "Sábado, 17 de octubre" (día de la semana con inicial mayúscula).
- Lista de cinco opciones (52 dp de alto, radio 14 dp): cuadrado de color de 28 dp (radio 8) + nombre del
  turno. La opción actualmente asignada lleva borde de 2 dp en `accent` y la palabra "Actual" a la
  derecha; el resto borde 1 dp `line`.
- Tocar una opción guarda el turno en la base de datos y cierra la hoja. Botón "Cancelar" (48 dp, fondo
  `surface` #F2F2EF) cierra sin cambios. También se puede quitar un turno asignado: añadir una sexta
  opción "Sin turno" solo si el usuario lo pide; por defecto no existe.
- Guardado: `INSERT OR REPLACE INTO turnos(fecha, tipo)`. Fecha en ISO `yyyy-MM-dd`.

### 3.5 Exportar a PDF
- Botón de ancho completo encima de la barra inferior (52 dp, radio 14, fondo `accent`, texto blanco
  16 sp 700, icono de documento con flecha de descarga): "Exportar a PDF".
- Genera un PDF A4 vertical del mes visible: título "Mis turnos — Octubre 2026", rejilla con el mismo
  esquema de colores y una leyenda al pie. Un mes por página.
- Nombre de archivo: `turnos-2026-10.pdf`. Tras generarlo, abrir la hoja de compartir del sistema
  (`ACTION_SEND`, tipo `application/pdf`) o guardarlo en Descargas; mostrar un Snackbar "PDF exportado".

## 3b. Pestaña Turnos (turnos del equipo)

Calendario mensual que, en vez del turno del propio usuario, muestra qué turno tiene cada compañero de
una **plantilla de trabajadores** (pestaña Plantillas, tipo Trabajadores) en cada día. Reutiliza los
mismos cinco valores de turno, colores y abreviaturas de la pestaña Calendario.

### 3b.1 Cabecera y plantilla activa
- Sobretítulo "TURNOS DEL EQUIPO". Fila de mes idéntica a Calendario: flechas "Mes anterior"/"Mes
  siguiente" y el selector de mes y año (3.1.1). El mes visible de Turnos es independiente del de
  Calendario.
- Debajo, a la izquierda, un chip de **plantilla activa** (36 dp, radio 999, borde `line`): insignia de
  24 dp con el icono de dos personas en `accent` sobre `surfaceAccent`, nombre de la plantilla (13 sp,
  700, una línea con elipsis, ancho máx. 240 dp) y chevron abajo. `contentDescription`: "Elegir
  plantilla de trabajadores". Al tocarlo se abre la hoja 3b.4. A la derecha, el resumen "12 de 31 días
  con turnos" (12 sp, 700, `textMuted`).
- La plantilla activa se guarda en la tabla `ajustes` (clave `turnos.plantilla_id`). Al abrir la pestaña
  por primera vez se usa la primera plantilla de tipo Trabajadores por `orden`. Si la plantilla activa se
  borra, se vuelve a la primera disponible.
- Sin ninguna plantilla de trabajadores: la fila del chip, la rejilla y la leyenda se sustituyen por el
  estado vacío centrado: insignia de 64 dp (radio 20, fondo `surface`, icono de dos personas 32 dp en
  #9A9A95), "Aún no hay ninguna plantilla de trabajadores" (17 sp, 800), la ayuda "Crea una en la pestaña
  Plantillas para ver aquí los turnos de tu equipo." (14 sp, 500, `textFaint`) y un botón "Ir a Plantillas"
  (48 dp, radio 14, contorno `accent`, chevron derecho) que navega a esa pestaña. La cabecera de mes sigue
  visible.

### 3b.2 Rejilla del mes
- Misma estructura que 3.3 (semana en lunes, cabecera L–D, celdas de 60 dp, radio 12, gap 6, hoy con
  borde 2 dp `accent`). Las celdas de Turnos no tienen color de fondo por turno: muestran el número del
  día (15 sp, 800) y debajo una fila de **puntos** de 7 dp (gap 3), uno por trabajador de la plantilla
  activa que tenga turno asignado ese día, en el orden de la plantilla y con el color `dot` de su turno
  (`shift.*.dot` en tokens). Se dibujan como máximo 5 puntos; si hay más, un texto "+N" (9 sp, 800,
  `textMuted`) tras el quinto. Un día con al menos una asignación tiene fondo #FAFAF8; sin asignaciones,
  blanco con borde `lineSoft`.
- `contentDescription` de la celda: "17 de octubre, 3 de 4 asignados".
- Leyenda de los cinco turnos (chips como en 3.2) debajo de la rejilla, encima de la barra inferior.
- Tocar un día abre la hoja 3b.3.

### 3b.3 Equipo del día (hoja inferior)
- `ModalBottomSheet` (alto máximo ≈ 85 % de la pantalla, la lista hace scroll). Cabecera: sobretítulo
  "EQUIPO DEL DÍA", título con la fecha "Sábado, 17 de octubre" (20 sp, 800) y subtítulo "Equipo de
  planta · 3 de 4 asignados" (13 sp, 600, `textMuted`). A la derecha, el botón de modo:
  - En **modo lectura**: botón "Editar" (40 dp, radio 12, contorno 1 dp `accent`, texto `accent`
    14 sp/800, icono lápiz 16 dp).
  - En **modo edición**: botón "Listo" (40 dp, radio 12, fondo `accent`, texto blanco).
- Lista de los trabajadores de la plantilla activa, en su orden (gap 8). Cada fila: min 56 dp, radio 14,
  borde `lineSoft`, padding 6/8/6/10: avatar de iniciales 36 dp (fondo `surface`), nombre (15 sp, 700,
  una línea con elipsis) y, a la derecha:
  - En modo lectura: un **chip** con el turno (12 sp, 700, padding 6×12, radio 999) con los colores
    `bg`/`fg`/`border` del turno, o "Sin asignar" (texto `textFaint`, borde 1 dp discontinuo #C9C9C4,
    fondo blanco) cuando no tiene turno ese día.
  - En modo edición: el chip pasa a ser un **botón** (40 dp, radio 12, mismos colores del turno, borde
    2 dp con el color `dot` del turno o `line` si está sin asignar, chevron derecho 16 dp).
    `contentDescription`: "Cambiar turno de Lucía Martínez, Noche". Tocarlo abre el **selector de turno
    del trabajador** (3b.3.1).

#### 3b.3.1 Selector de turno del trabajador (hoja inferior)
- Es el **mismo componente** que el selector de turno de Calendario (3.4): misma hoja, mismas cinco
  opciones de 52 dp con cuadrado de color de 28 dp, nombre del turno y "Actual" en la asignada, mismo
  orden (Libre, Mañana, Tarde, Noche, Vacaciones). En Compose debe reutilizarse `SelectorTurno`
  parametrizándolo (título, subtítulo, turno actual, acción secundaria), no copiarlo.
- Diferencias respecto a Calendario: la cabecera lleva un botón circular "Volver al equipo del día" (44 dp,
  fondo `surface`, flecha izquierda), el sobretítulo es la fecha ("Sábado, 17 de octubre") y el título
  "Turno de Lucía Martínez" (20 sp, 800, una línea con elipsis). La fila inferior tiene dos botones de
  48 dp a partes iguales: "Quitar turno" (borde #E6C9C3, texto `danger`; solo visible si el trabajador
  tiene turno ese día) y "Cancelar" (fondo `surface`).
- Se presenta **sustituyendo** a la hoja "Equipo del día" (una sola `ModalBottomSheet` cuyo contenido
  cambia con animación, o navegación interna de la hoja), nunca dos hojas apiladas. Elegir una opción,
  "Quitar turno", "Cancelar", la flecha de volver, deslizar o el scrim devuelven a "Equipo del día" en
  modo edición; solo "Cancelar"/volver/deslizar/scrim lo hacen sin cambios.
- **Guardado inmediato**: elegir un turno escribe `INSERT OR REPLACE` en `asignaciones`; "Quitar turno"
  hace `DELETE`. No hay botón Guardar. Al volver a "Equipo del día" el chip, el recuento del subtítulo y
  los puntos de la celda del día ya reflejan el cambio. "Listo" solo vuelve al modo lectura.
- Botón "Cerrar" (48 dp, fondo `surface`) al pie. Cerrar deslizando o por el scrim equivale a Cerrar y
  sale también del modo edición.
- El día abierto, el modo edición, el trabajador cuyo selector está abierto y la plantilla activa
  sobreviven a la rotación (SavedStateHandle).

### 3b.4 Elegir plantilla activa (hoja inferior)
- Sobretítulo "PLANTILLA DE TRABAJADORES", título "¿Qué equipo quieres ver?". Una fila por plantilla de
  tipo Trabajadores (56 dp, radio 14): nombre (16 sp, 700) y "4 trabajadores" (13 sp, 600, `textMuted`);
  la activa con borde 2 dp `accent`, el resto 1 dp `line`. Tocar una la activa, la guarda en `ajustes` y
  cierra la hoja. Botón "Cancelar".

### 3b.5 Reglas de datos
- Una asignación es (fecha, trabajador) → turno; única por fecha y trabajador
  (`UNIQUE(fecha, trabajador_id)`). Un trabajador puede estar sin asignar un día (no existe fila).
- La pestaña Turnos no escribe en la tabla `turnos` del propio usuario ni la lee: son datos distintos.
- Borrar un trabajador borra sus asignaciones en cascada; borrar una plantilla borra trabajadores y
  asignaciones. El diálogo de "Eliminar plantilla" (4b.3) debe añadir al cuerpo "y sus N turnos
  asignados" cuando N > 0. El diálogo de eliminar trabajador no cambia (sigue siendo inmediato con
  Deshacer; deshacer reinserta también sus asignaciones).
- Un trabajador añadido a la plantilla aparece de inmediato en la hoja del día como "Sin asignar" para
  todos los días.

## 4. Pestaña Checklist

La pestaña gestiona **varias listas** de tareas. Cada lista se crea desde una plantilla de tareas (4b,
tipo Tareas), que actúa como preset y copia sus tareas sin marcar, o en blanco. Solo hay una lista
**activa** a la vez y todas las acciones (agregar, marcar, borrar, reiniciar, eliminar) actúan sobre ella.

### 4.0 Listas
- Bajo la cabecera, una fila horizontal con scroll (gap 8, padding 2/20/14) de **chips de lista**
  (36 dp, radio 999, texto 13 sp/700) con el nombre y una píldora de progreso "2/5" (11 sp, 800). La
  activa: fondo `accent`, texto blanco, píldora blanca al 22 %; el resto: fondo blanco, borde `line`,
  píldora `surface`. Tocar un chip la activa (y descarta el texto escrito en el campo de nueva tarea).
  `contentDescription`: "Rutina de apertura, 2 de 5 hechas, lista activa". Orden: por `orden`.
- Al final de la fila, el chip "+ Nueva" (borde 1 dp discontinuo `accent`, texto `accent`) abre la hoja 4.5.
- Debajo, la **tarjeta de la lista activa** (margen 0/20/12, padding 10/6/10/14, radio 16, fondo
  `surfaceInput`): nombre (16 sp, 800) con lápiz de 14 dp — tocarlo lo convierte en un campo de 40 dp con
  "Guardar", igual que el nombre de una plantilla —, subtítulo "Desde la plantilla «Rutina de apertura»"
  o "Lista en blanco" (12 sp, 600, `textMuted`), y a la derecha dos botones de 44 dp: **Reiniciar**
  (icono de flechas circulares en `accent`, `contentDescription` "Reiniciar las tareas de esta lista";
  sustituye al antiguo FAB, que desaparece) y **Eliminar lista** (papelera en `danger`,
  `contentDescription` "Eliminar esta lista"), que abre el diálogo 4.6.
- La lista activa se recuerda en `ajustes` (clave `checklist.lista_id`). Si se elimina, pasa a ser activa
  la lista vecina (la siguiente, o la anterior si era la última).
- Sin ninguna lista: estado vacío centrado (insignia 64 dp con el icono de checklist, "Sin listas todavía",
  "Crea una lista desde una plantilla de tareas o empieza con una en blanco." y botón relleno "Nueva lista"
  que abre 4.5). La fila de chips, la tarjeta y el alta se ocultan.

### 4.1 Cabecera
- Sobretítulo "MIS TURNOS", título "Checklist" (22 sp, 800) y a la derecha el progreso de la lista activa
  "3 de 6 hechas" (13 sp, 700, `textMuted`). Si la lista activa no tiene tareas, el progreso se oculta.

### 4.2 Alta de tarea (en la lista activa)
- Campo de texto (50 dp, radio 14, fondo #F7F7F4, borde `line`) con placeholder "Nueva tarea…" y botón
  "Agregar" (50 dp, fondo `accent`, icono +). Pulsar el botón o la acción IME Done añade la tarea.
- Se ignoran textos vacíos o solo espacios; se recorta el texto. Tras añadir, el campo se vacía.
- La tarea nueva se inserta al final de la lista activa con `hecha = 0` y `lista_id` = lista activa.

### 4.3 Lista
- Cada fila: altura mínima 60 dp, radio 14, borde `lineSoft`. Contiene: casilla de 24 dp (color `accent`),
  texto (16 sp, 600) y botón papelera (44 dp, icono en #8A2E1F, `contentDescription` "Borrar tarea").
- Marcar/desmarcar la casilla actualiza `hecha` en la base de datos. Tarea hecha: fondo #F7F7F4, texto en
  `textFaint` y tachado.
- La papelera borra la tarea de inmediato (sin confirmación), opcionalmente con Snackbar "Tarea borrada"
  y acción "Deshacer".
- Orden: por `orden` ascendente (campo entero), que coincide con el orden de creación.
- Estado vacío de la lista activa: icono de checklist grande en gris, "Esta lista está vacía" y
  "Escribe una tarea arriba y pulsa Agregar."

### 4.4 Reiniciar la lista activa
- Botón de 44 dp en la tarjeta de la lista activa (ver 4.0). Ya no existe el FAB.
- Acción: `UPDATE tareas SET hecha = 0 WHERE lista_id = :listaId`. No borra tareas; solo desmarca las de
  la lista activa. Las demás listas no cambian.

### 4.5 Nueva lista (hoja inferior)
- Sobretítulo "NUEVA LISTA", título "¿Desde qué plantilla de tareas?". Una tarjeta por plantilla de tipo
  Tareas (64 dp mínimo, radio 16, borde `line`, insignia 40 dp #EDE9FF con icono de checklist en #4B43B8,
  nombre 16 sp/800, "5 tareas" 13 sp/600) y, al final, la opción "Lista en blanco" (56 dp, borde
  discontinuo #C9C9C4, insignia `surface` con +). Si no hay plantillas de tareas se muestra el aviso "No
  tienes plantillas de tareas. Puedes crearlas en la pestaña Plantillas." encima de "Lista en blanco".
  Botón "Cancelar".
- Elegir una plantilla crea la lista con su nombre, `plantilla_id` = la plantilla y una tarea por cada
  elemento del preset (mismo texto y orden, `hecha = 0`); "Lista en blanco" crea "Lista N" sin tareas y
  `plantilla_id` NULL. En ambos casos la nueva lista pasa a ser la activa y la hoja se cierra. Las tareas
  son **copias**: cambiar la plantilla después no modifica listas ya creadas, y viceversa.

### 4.6 Eliminar lista
- `AlertDialog` "¿Eliminar la lista «Rutina de apertura»?" con cuerpo "Se borrarán sus 5 tareas y su
  progreso. La plantilla de origen no cambia." (o "La lista está vacía." si no tiene tareas) y botones
  "Eliminar" (relleno `danger`) / "Cancelar". Confirmar borra la lista y sus tareas (ON DELETE CASCADE) y
  activa la lista vecina.

## 4b. Pestaña Plantillas

Las plantillas son conjuntos de datos reutilizables que otras pestañas consumen. Hay dos tipos:
**Trabajadores** (una lista con N trabajadores cuya única propiedad es el nombre; la usa la pestaña
Turnos) y **Tareas** (un preset con N tareas cuya única propiedad es el texto; la usa Checklist para
crear listas). Ambos comparten la misma pantalla de detalle (4b.3) cambiando solo textos e icono. El
modelo (`TipoPlantilla`) está preparado para añadir más tipos sin rediseñar la pestaña.

### 4b.1 Lista de plantillas
- Cabecera: sobretítulo "MIS TURNOS", título "Plantillas" (22 sp, 800) y a la derecha el contador
  "2 plantillas" / "1 plantilla" (13 sp, 700, `textMuted`); oculto si no hay ninguna.
- Lista con gap 10 y padding inferior 100 dp (deja sitio al FAB). Cada plantilla es una tarjeta tocable
  de 72 dp mínimo, radio 16, borde `lineSoft`, padding 12/10/12/14: insignia de 44 dp (radio 12, fondo
  `surfaceAccent`, icono del tipo en `accent`, para Trabajadores el icono de dos personas), nombre
  (16 sp, 800, una línea con elipsis), subtítulo "Trabajadores · 4 trabajadores" (13 sp, 600,
  `textMuted`) y chevron derecho en `textFaint`. Tocar la tarjeta abre el detalle (4b.3).
- Orden: por `orden` ascendente (orden de creación).
- Estado vacío: icono de capas 44 dp gris, "Sin plantillas todavía" y "Pulsa + para crear tu primera
  plantilla."
- FAB de 60 dp, radio 18, fondo `accent`, icono + de 28 dp (trazo 2.4), a 20 dp del borde derecho y
  20 dp sobre la barra inferior. `contentDescription`: "Añadir plantilla". Abre la hoja 4b.2.

### 4b.2 Elegir tipo de plantilla (hoja inferior)
- `ModalBottomSheet` con asa, sobretítulo "NUEVA PLANTILLA" y título "¿Qué tipo de plantilla?" (20 sp, 800).
- Una opción por tipo disponible, como tarjeta de 72 dp mínimo, radio 16, borde `line`: insignia 44 dp
  con el icono del tipo, nombre del tipo (16 sp, 800), descripción (13 sp, 600, `textMuted`) y chevron.
  En este orden: "Trabajadores" — "Lista de nombres para usar en otras pestañas" (insignia
  `surfaceAccent`/`accent`, icono de dos personas) y "Tareas" — "Preset de tareas para crear listas en
  Checklist" (insignia #EDE9FF/#4B43B8, icono de checklist).
- Botón "Cancelar" (48 dp, fondo `surface`).
- Tocar un tipo crea la plantilla en la base de datos con nombre por defecto "Trabajadores N" o
  "Tareas N" (N = número de plantillas + 1), cierra la hoja y abre su detalle con el campo de nombre ya en
  modo edición para que el usuario lo cambie si quiere.
- En la lista de plantillas, la insignia de cada tarjeta usa el icono y los colores de su tipo y el
  subtítulo dice "Trabajadores · 4 trabajadores" o "Tareas · 5 tareas".

### 4b.3 Detalle de una plantilla (trabajadores o tareas)

El detalle es el mismo componente para ambos tipos. Para el tipo Tareas cambian solo estos textos:
sobretítulo "PLANTILLA · TAREAS", placeholder "Nueva tarea del preset…", etiqueta "N TAREAS" / "1 TAREA",
estado vacío "Sin tareas todavía" / "Escribe una tarea arriba y pulsa Agregar.", `contentDescription`
"Editar tarea" / "Eliminar tarea", y el avatar muestra el número de orden (1, 2, 3…) con radio 10 dp en
lugar de las iniciales en círculo. El resto de reglas (alta, edición en línea, eliminar con Deshacer,
renombrar, eliminar plantilla) son idénticas. El cuerpo del diálogo de eliminar una plantilla de tareas
añade "Las listas ya creadas en Checklist no se ven afectadas."

Descripción detallada tomando como ejemplo el tipo Trabajadores:
- Barra superior (padding 14/12/10/8): botón "Volver a plantillas" (44 dp, flecha izquierda), sobretítulo
  "PLANTILLA · TRABAJADORES" y el nombre de la plantilla (20 sp, 800) con un icono de lápiz de 16 dp al
  lado; tocar el nombre (`contentDescription` "Cambiar nombre de la plantilla") lo convierte en un campo
  de texto de 40 dp con borde 2 dp `accent` y un botón "Guardar". Enter o "Guardar" confirman; se ignora
  un nombre vacío (se conserva el anterior).
- Alta de trabajador: campo de 50 dp con placeholder "Nombre del trabajador…" y botón "Agregar" (igual
  que en Checklist). Enter/IME Done o el botón añaden al final; se ignoran textos vacíos; el campo se
  vacía al añadir. No se impide repetir nombres.
- Etiqueta de sección "4 TRABAJADORES" / "1 TRABAJADOR" (12 sp, 700, mayúsculas, `textMuted`).
- Lista (gap 8). Cada fila: min 60 dp, radio 14, borde `lineSoft`, padding 8/8/8/12: avatar circular de
  36 dp (fondo `surface`) con las iniciales (primera letra del primer y del último nombre, 13 sp, 800),
  nombre (16 sp, 600, una línea con elipsis), botón lápiz "Editar trabajador" (44 dp, `textMuted`) y
  botón papelera "Eliminar trabajador" (44 dp, `danger`).
- Edición en línea: al pulsar el lápiz la fila pasa a borde 2 dp `accent` y muestra un campo de 40 dp con
  el nombre actual, un botón "Guardar" y un botón X "Cancelar edición" (40 dp, fondo `surface`). Enter o
  "Guardar" confirman (texto vacío → se conserva el nombre anterior); Escape o X cancelan. Solo puede
  haber una fila en edición a la vez.
- Eliminar trabajador: inmediato, con Snackbar "Trabajador eliminado" y acción "Deshacer" (reinserta con
  su id y `orden`).
- Estado vacío de la lista: "Sin trabajadores todavía" y "Escribe un nombre arriba y pulsa Agregar."
- Al final de la lista, botón "Eliminar plantilla" (48 dp, radio 14, borde 1 dp #E6C9C3, texto e icono
  en `danger`). Muestra un `AlertDialog` "¿Eliminar la plantilla «Equipo de planta»?" con cuerpo "Se
  borrarán también sus 4 trabajadores." y botones "Eliminar" / "Cancelar". Confirmar borra la plantilla y
  sus trabajadores (ON DELETE CASCADE) y vuelve a la lista.
- El botón Atrás del sistema en el detalle vuelve a la lista de plantillas, no sale de la app.

### 4b.4 Reglas de datos
- `plantillas.tipo` admite `trabajadores` y `tareas`; el enum `TipoPlantilla` es el punto de extensión.
  Los elementos de cada tipo viven en su propia tabla (`trabajadores`, `tareas_plantilla`).
- Una plantilla de tareas es un preset: las listas de Checklist creadas desde ella guardan copias de sus
  tareas y `listas.plantilla_id` como referencia informativa (`ON DELETE SET NULL`), de modo que borrar
  la plantilla no toca las listas.

## 5. Persistencia

Ver `db/schema.sql`. Ocho tablas:

- `turnos(fecha TEXT PRIMARY KEY, tipo TEXT NOT NULL CHECK(tipo IN (...)))`
- `listas(id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, plantilla_id INTEGER NULL REFERENCES plantillas(id) ON DELETE SET NULL, orden INTEGER NOT NULL)`
- `tareas(id INTEGER PRIMARY KEY AUTOINCREMENT, lista_id INTEGER NOT NULL REFERENCES listas(id) ON DELETE CASCADE, texto TEXT NOT NULL, hecha INTEGER NOT NULL DEFAULT 0, orden INTEGER NOT NULL)`
- `tareas_plantilla(id INTEGER PRIMARY KEY AUTOINCREMENT, plantilla_id INTEGER NOT NULL REFERENCES plantillas(id) ON DELETE CASCADE, texto TEXT NOT NULL, orden INTEGER NOT NULL)`
- `plantillas(id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL CHECK(tipo IN ('trabajadores')), nombre TEXT NOT NULL, orden INTEGER NOT NULL)`
- `trabajadores(id INTEGER PRIMARY KEY AUTOINCREMENT, plantilla_id INTEGER NOT NULL REFERENCES plantillas(id) ON DELETE CASCADE, nombre TEXT NOT NULL, orden INTEGER NOT NULL)`
- `asignaciones(id INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT NOT NULL, trabajador_id INTEGER NOT NULL REFERENCES trabajadores(id) ON DELETE CASCADE, tipo TEXT NOT NULL CHECK(tipo IN (...)), UNIQUE(fecha, trabajador_id))`
- `ajustes(clave TEXT PRIMARY KEY, valor TEXT NOT NULL)` — pares clave/valor de la app
  (`turnos.plantilla_id`, `checklist.lista_id`). Sustituye a DataStore/SharedPreferences, que siguen
  prohibidos.

La app lee los turnos del mes visible con un rango `fecha BETWEEN ? AND ?` y observa cambios con `Flow`.
`plantillas` y `trabajadores` llegan en la versión 2 del esquema de Room con una `Migration(1, 2)`;
`asignaciones` y `ajustes` en la versión 3 con una `Migration(2, 3)`. Esas migraciones solo crean tablas e
índices. La versión 4 (`Migration(3, 4)`) añade `tareas_plantilla` y `listas`, y **mueve** las tareas
existentes: crea la lista "Mi checklist" (`orden` 1, `plantilla_id` NULL), recrea `tareas` con la columna
`lista_id` apuntando a esa lista (SQLite no permite añadir una columna NOT NULL con FK sin valor por
defecto: se hace con tabla temporal + copia + renombrado) y guarda `checklist.lista_id` en `ajustes`. Si una
instalación no tiene tareas, la lista "Mi checklist" no se crea. Si ninguna versión intermedia se ha
publicado, las migraciones pueden fusionarse. Turnos lee las asignaciones del mes visible con `fecha BETWEEN ? AND ?` filtrando por los
trabajadores de la plantilla activa (JOIN con `trabajadores`).

## 6. Accesibilidad y calidad

- Contraste de texto ≥ 4.5:1 (los pares de color anteriores ya lo cumplen).
- Targets ≥ 44 dp. Soporte de tamaño de fuente del sistema hasta 1.3× sin recortes (las celdas pueden
  crecer).
- Rotación: el estado se conserva (ViewModel). En apaisado la rejilla puede ocupar menos alto.

## 7. Fuera de alcance (v1)

Cuentas, sincronización, notificaciones, widgets, varios calendarios, turnos partidos, estadísticas.
Otros tipos de plantilla, el uso de plantillas desde Calendario, exportar a PDF el calendario del equipo,
copiar los turnos de un día a otro, ver varias plantillas a la vez, reordenar listas o tareas arrastrando y
"volver a sincronizar" una lista con su plantilla quedan para versiones posteriores; el modelo de datos ya
lo permite.
