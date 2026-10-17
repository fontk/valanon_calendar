# SPEC — Mis turnos (Android)

## 1. Resumen

App personal para registrar el turno de trabajo de cada día en un calendario y llevar una checklist de
tareas reutilizable. Tres pestañas en una barra inferior: **Calendario** (izquierda, destino inicial),
**Checklist** (centro) y **Plantillas** (derecha), esta última para editar datos que otras pestañas
reutilizarán. Todos los datos se guardan en SQLite (Room) y persisten entre sesiones.

## 2. Navegación

- Barra inferior de 72 dp con tres pestañas, en este orden: Calendario, Checklist, Plantillas. La pestaña activa muestra el icono dentro de una píldora
  (fondo `surfaceAccent` #DDF0EC) y texto en `accent` con peso 800; la inactiva en `textMuted` #5F5F5A,
  peso 700. Etiquetas: "Calendario" y "Checklist".
- Iconos de trazo (stroke 2 dp): calendario (rectángulo con dos anillas y línea de cabecera), checklist
  (rectángulo con marca de verificación) y plantillas (tres capas apiladas: rombo arriba y dos arcos
  debajo). Nunca emoji.
- Cambiar de pestaña conserva el estado de cada una (mes visible, texto escrito en el campo de nueva tarea,
  plantilla abierta en Plantillas).

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

## 4. Pestaña Checklist

### 4.1 Cabecera
- Sobretítulo "MIS TURNOS", título "Checklist" (22 sp, 800) y a la derecha el progreso "3 de 6 hechas"
  (13 sp, 700, `textMuted`). Si no hay tareas, el progreso se oculta.

### 4.2 Alta de tarea
- Campo de texto (50 dp, radio 14, fondo #F7F7F4, borde `line`) con placeholder "Nueva tarea…" y botón
  "Agregar" (50 dp, fondo `accent`, icono +). Pulsar el botón o la acción IME Done añade la tarea.
- Se ignoran textos vacíos o solo espacios; se recorta el texto. Tras añadir, el campo se vacía.
- La tarea nueva se inserta al final de la lista con `hecha = 0`.

### 4.3 Lista
- Cada fila: altura mínima 60 dp, radio 14, borde `lineSoft`. Contiene: casilla de 24 dp (color `accent`),
  texto (16 sp, 600) y botón papelera (44 dp, icono en #8A2E1F, `contentDescription` "Borrar tarea").
- Marcar/desmarcar la casilla actualiza `hecha` en la base de datos. Tarea hecha: fondo #F7F7F4, texto en
  `textFaint` y tachado.
- La papelera borra la tarea de inmediato (sin confirmación), opcionalmente con Snackbar "Tarea borrada"
  y acción "Deshacer".
- Orden: por `orden` ascendente (campo entero), que coincide con el orden de creación.
- Estado vacío: icono de checklist grande en gris, "Sin tareas todavía" y "Escribe una tarea arriba y
  pulsa Agregar."

### 4.4 Botón flotante
- FAB de 60 dp, radio 18, fondo `accent`, icono de dos flechas circulares, situado a 20 dp del borde
  derecho y 20 dp por encima de la barra inferior. `contentDescription`: "Reiniciar todas las tareas".
- Acción: `UPDATE tareas SET hecha = 0`. No borra tareas. Las tareas se mantienen; solo se desmarcan.

## 4b. Pestaña Plantillas

Las plantillas son conjuntos de datos reutilizables que otras pestañas consumirán en el futuro (por
ejemplo, asignar trabajadores a los turnos del calendario). En esta versión existe un único tipo de
plantilla, **Trabajadores**: una lista con N trabajadores cuya única propiedad es el nombre. El modelo
y la interfaz están preparados para añadir más tipos sin rediseñar la pestaña.

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
  En v1 solo "Trabajadores" — "Lista de nombres para usar en otras pestañas".
- Botón "Cancelar" (48 dp, fondo `surface`).
- Tocar un tipo crea la plantilla en la base de datos con nombre por defecto "Trabajadores N" (N =
  número de plantillas + 1), cierra la hoja y abre su detalle con el campo de nombre ya en modo edición
  para que el usuario lo cambie si quiere.

### 4b.3 Detalle de una plantilla de trabajadores
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
- `plantillas.tipo` solo admite `trabajadores` en v1; el enum `TipoPlantilla` es el punto de extensión.
- Las plantillas no se referencian todavía desde Calendario ni Checklist; cuando se haga, el borrado
  deberá avisar de las referencias afectadas (fuera de alcance ahora).

## 5. Persistencia

Ver `db/schema.sql`. Cuatro tablas:

- `turnos(fecha TEXT PRIMARY KEY, tipo TEXT NOT NULL CHECK(tipo IN (...)))`
- `tareas(id INTEGER PRIMARY KEY AUTOINCREMENT, texto TEXT NOT NULL, hecha INTEGER NOT NULL DEFAULT 0, orden INTEGER NOT NULL)`
- `plantillas(id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL CHECK(tipo IN ('trabajadores')), nombre TEXT NOT NULL, orden INTEGER NOT NULL)`
- `trabajadores(id INTEGER PRIMARY KEY AUTOINCREMENT, plantilla_id INTEGER NOT NULL REFERENCES plantillas(id) ON DELETE CASCADE, nombre TEXT NOT NULL, orden INTEGER NOT NULL)`

La app lee los turnos del mes visible con un rango `fecha BETWEEN ? AND ?` y observa cambios con `Flow`.
Las dos tablas nuevas llegan en la versión 2 del esquema de Room con una `Migration(1, 2)` que solo crea
tablas e índices (no toca datos existentes).

## 6. Accesibilidad y calidad

- Contraste de texto ≥ 4.5:1 (los pares de color anteriores ya lo cumplen).
- Targets ≥ 44 dp. Soporte de tamaño de fuente del sistema hasta 1.3× sin recortes (las celdas pueden
  crecer).
- Rotación: el estado se conserva (ViewModel). En apaisado la rejilla puede ocupar menos alto.

## 7. Fuera de alcance (v1)

Cuentas, sincronización, notificaciones, widgets, varios calendarios, turnos partidos, estadísticas.
Otros tipos de plantilla y el uso de las plantillas desde Calendario o Checklist (asignar trabajadores a
turnos) quedan para versiones posteriores; el modelo de datos ya lo permite.
