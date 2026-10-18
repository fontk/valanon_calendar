# Historias de usuario y criterios de aceptación

Formato: **Como** usuario **quiero** … **para** …. Cada historia lista criterios verificables (Dado / Cuando / Entonces).

## Calendario

### C1 — Ver el mes actual
Como usuario quiero abrir la app y ver el mes en curso para situarme rápidamente.
- Dado que abro la app, cuando carga, entonces se muestra la pestaña Calendario con el mes y año actuales
  y el día de hoy resaltado con borde en el color de acento.
- Las semanas empiezan en lunes y la cabecera muestra L M X J V S D.

### C2 — Navegar entre meses
Como usuario quiero pasar al mes anterior o siguiente para planificar o consultar.
- Dado el mes visible, cuando pulso "Mes siguiente", entonces el título y la rejilla pasan al mes siguiente
  (incluido el cambio de año en diciembre → enero).
- Los turnos ya guardados del mes destino se muestran al instante.

### C2b — Ir directamente a un mes y año
Como usuario quiero tocar el título del mes y elegir cualquier mes y año para no tener que pulsar las
flechas muchas veces.
- Dado el calendario, cuando toco el título "Octubre 2026", entonces se abre una hoja inferior con el año
  2026, una rejilla de 12 meses con OCT resaltado como activo y los botones "Hoy" y "Cancelar".
- Cuando pulso "Año siguiente" dos veces, entonces la hoja muestra 2028 y ningún mes aparece como activo
  (el mes visible sigue siendo octubre de 2026 hasta que elija uno).
- Cuando toco "MAR" con 2028 mostrado, entonces la hoja se cierra, el título pasa a "Marzo 2028" y la
  rejilla muestra los turnos guardados de ese mes.
- Dado que estoy viendo otro mes, cuando abro la hoja y pulso "Hoy", entonces el calendario vuelve al mes
  actual y la hoja se cierra.
- Cuando pulso "Cancelar", deslizo la hoja hacia abajo o toco fuera, entonces se cierra sin cambiar el mes
  visible.
- Cuando roto el dispositivo con la hoja abierta, entonces sigue abierta y con el mismo año mostrado.

### C3 — Marcar el turno de un día
Como usuario quiero tocar un día y elegir Libre, Mañana, Tarde, Noche o Vacaciones para registrar mi turno.
- Dado un día cualquiera, cuando lo toco, entonces se abre la hoja inferior con la fecha completa y las
  cinco opciones; la asignada actualmente aparece marcada como "Actual".
- Cuando elijo una opción, entonces la hoja se cierra y la celda cambia de color y abreviatura.
- Cuando pulso "Cancelar", entonces la hoja se cierra sin cambios.

### C4 — Persistencia de turnos
Como usuario quiero que mis turnos se guarden para no perderlos al cerrar la app.
- Dado que he marcado turnos, cuando cierro la app por completo (forzar detención) y la reabro, entonces
  todos los turnos siguen marcados igual.
- Existe un test de DAO que inserta, reemplaza y consulta por rango de fechas.

### C5 — Exportar a PDF
Como usuario quiero exportar el calendario del mes a PDF para imprimirlo o enviarlo.
- Dado un mes visible, cuando pulso "Exportar a PDF", entonces se genera `turnos-AAAA-MM.pdf` en A4 con
  la rejilla coloreada y la leyenda, y se abre la hoja de compartir del sistema.
- El PDF se abre correctamente en un visor estándar y los colores coinciden con la app.

## Turnos (equipo)

### T1 — Ver el calendario del equipo
Como usuario quiero una pestaña Turnos con el mes en curso y, en cada día, un punto por compañero con el
color de su turno, para ver de un vistazo cómo está el equipo.
- Dado que existe una plantilla de trabajadores con asignaciones, cuando abro Turnos, entonces veo el mes
  actual, el chip con el nombre de la plantilla activa, el resumen "N de M días con turnos", las celdas
  con sus puntos de color y la leyenda.
- Dado un día con 7 asignados, entonces la celda muestra 5 puntos y "+2".
- Las flechas y el selector de mes y año funcionan igual que en Calendario y no cambian el mes de Calendario.

### T2 — Sin plantillas
- Dado que no existe ninguna plantilla de trabajadores, cuando abro Turnos, entonces veo el estado vacío
  con el botón "Ir a Plantillas", que me lleva a esa pestaña.

### T3 — Consultar el equipo de un día
- Cuando toco un día, entonces se abre "Equipo del día" con la fecha, "Plantilla · X de Y asignados" y
  una fila por trabajador con su chip de turno o "Sin asignar".
- Cuando pulso "Cerrar", deslizo o toco fuera, entonces la hoja se cierra.

### T4 — Editar los turnos de un día
- Dado la hoja abierta en lectura, cuando pulso "Editar", entonces cada chip pasa a ser un botón con
  chevron y el botón de cabecera pasa a "Listo".
- Cuando toco el chip de Javier, entonces la hoja pasa a mostrar el selector de turno, idéntico al de
  Calendario: fecha como sobretítulo, "Turno de Javier Ortega", las cinco opciones con su cuadrado de
  color y "Actual" en la que tiene, y abajo "Quitar turno" y "Cancelar".
- Cuando elijo "Noche", entonces vuelvo a "Equipo del día" en modo edición, el chip de Javier tiene los
  colores de Noche, la asignación queda guardada al instante, el subtítulo actualiza el recuento y el
  punto de Javier en la celda del día cambia de color.
- Cuando pulso "Quitar turno", entonces se borra su asignación de ese día, su chip pasa a "Sin asignar" y
  su punto desaparece.
- Cuando pulso "Cancelar" o la flecha de volver en el selector, entonces regreso a "Equipo del día" sin
  cambios.
- Cuando pulso "Listo", entonces vuelvo al modo lectura con los chips actualizados.
- Cuando cierro la hoja estando en edición, entonces los cambios ya hechos se conservan (se guardaron al
  elegir) y al reabrir el día la hoja está en modo lectura.

### T5 — Cambiar de plantilla activa
- Cuando toco el chip de la plantilla, entonces se abre "¿Qué equipo quieres ver?" con todas las plantillas
  de trabajadores y la activa resaltada.
- Cuando elijo otra, entonces la rejilla y la hoja pasan a mostrar sus trabajadores y asignaciones, y la
  elección se recuerda al cerrar y reabrir la app.

### T6 — Coherencia con Plantillas
- Cuando añado un trabajador a la plantilla activa, entonces aparece en la hoja del día como "Sin asignar".
- Cuando elimino un trabajador con asignaciones, entonces desaparecen sus puntos; "Deshacer" los restaura.
- Cuando elimino la plantilla activa, entonces Turnos pasa a la primera plantilla disponible o al estado
  vacío si no queda ninguna.

### T7 — Persistencia y migración
- Dado asignaciones guardadas, cuando fuerzo el cierre y reabro la app, entonces siguen iguales.
- Dado una base de datos en versión 2, cuando actualizo, entonces turnos, tareas, plantillas y trabajadores
  se conservan y la pestaña Turnos funciona (migración 2 → 3).

## Checklist

### L0 — Varias listas, una activa
Como usuario quiero tener varias listas de tareas y trabajar con una cada vez.
- Dado dos listas, cuando abro Checklist, entonces veo sus chips con el progreso "2/5" y "1/3", la última
  activa resaltada en acento, su tarjeta con nombre y origen, y debajo solo sus tareas.
- Cuando toco otro chip, entonces esa lista pasa a ser la activa, la tarjeta y las tareas cambian, y el
  progreso de la cabecera también.
- Cuando cierro y reabro la app, entonces sigue activa la misma lista.
- Cuando toco el nombre de la lista y escribo otro, entonces el chip y la tarjeta muestran el nuevo nombre.

### L7 — Crear una lista desde una plantilla de tareas
- Cuando pulso "+ Nueva", entonces se abre la hoja "¿Desde qué plantilla de tareas?" con una tarjeta por
  plantilla de tareas (nombre y número de tareas), la opción "Lista en blanco" y "Cancelar".
- Cuando toco "Rutina de apertura" (5 tareas), entonces aparece una lista nueva con ese nombre, "Desde la
  plantilla «Rutina de apertura»", sus 5 tareas sin marcar, y queda activa.
- Cuando toco "Lista en blanco", entonces aparece "Lista N" vacía, activa, con el estado "Esta lista está
  vacía".
- Dado que no tengo plantillas de tareas, cuando abro la hoja, entonces veo el aviso que me remite a
  Plantillas y solo puedo crear una lista en blanco.
- Cuando después edito la plantilla de origen, entonces la lista ya creada no cambia.

### L8 — Eliminar una lista
- Cuando pulso la papelera de la tarjeta de la lista activa, entonces aparece el diálogo con el nombre y el
  número de tareas; "Cancelar" no hace nada; "Eliminar" borra la lista con sus tareas y activa la vecina.
- Dado que elimino la única lista, entonces veo el estado vacío "Sin listas todavía" con el botón "Nueva
  lista".

### L1 — Añadir tarea
Como usuario quiero escribir una tarea y agregarla a la lista activa para no olvidarla.
- Dado texto en el campo, cuando pulso "Agregar" o la acción Done del teclado, entonces la tarea aparece
  al final de la lista activa sin marcar y el campo queda vacío; las demás listas no cambian.
- Dado el campo vacío o solo espacios, cuando pulso "Agregar", entonces no ocurre nada.

### L2 — Marcar y desmarcar
Como usuario quiero marcar una tarea como hecha y poder desmarcarla.
- Cuando toco la casilla, entonces la tarea alterna su estado, se tacha y el contador "X de Y hechas"
  se actualiza.

### L3 — Borrar tarea
Como usuario quiero borrar una tarea con el icono de papelera.
- Cuando pulso la papelera de una tarea, entonces desaparece de la lista y de la base de datos.

### L4 — Reiniciar checks de la lista activa
Como usuario quiero un botón que desmarque todas las tareas de la lista activa para reutilizarla cada día.
- Dado varias tareas marcadas en la lista activa y otras marcadas en otra lista, cuando pulso el botón
  Reiniciar de la tarjeta de la lista activa, entonces solo las de la lista activa quedan desmarcadas,
  ninguna se borra y la otra lista conserva sus marcas.
- Ya no existe el botón flotante.

### L5 — Persistencia de tareas y migración
- Dado listas y tareas creadas y algunas marcadas, cuando cierro y reabro la app, entonces las listas, su
  orden y los estados son idénticos.
- Dado una instalación anterior con tareas en la checklist única, cuando actualizo, entonces aparecen en
  una lista llamada "Mi checklist", activa, con sus estados intactos (migración a la versión 4).

### L6 — Estados vacíos
- Dado una lista activa sin tareas, entonces se muestra "Esta lista está vacía" con la ayuda "Escribe una
  tarea arriba y pulsa Agregar." y no se muestra el contador de progreso.
- Dado que no hay ninguna lista, entonces se muestra "Sin listas todavía" con el botón "Nueva lista" y se
  ocultan los chips, la tarjeta y el campo de alta.

## Plantillas

### P1 — Ver la lista de plantillas
Como usuario quiero una pestaña Plantillas donde ver todas mis plantillas.
- Dado que abro la pestaña Plantillas sin plantillas, entonces veo "Sin plantillas todavía", la ayuda
  "Pulsa + para crear tu primera plantilla." y el FAB +; no se muestra el contador.
- Dado que tengo dos plantillas, entonces veo dos tarjetas con su nombre, "Trabajadores · N trabajadores"
  y el contador "2 plantillas".

### P2 — Crear una plantilla eligiendo su tipo
Como usuario quiero pulsar + y elegir qué tipo de plantilla crear.
- Cuando pulso el FAB, entonces se abre la hoja "¿Qué tipo de plantilla?" con las opciones "Trabajadores"
  y "Tareas", y "Cancelar".
- Cuando toco "Tareas", entonces se crea una plantilla llamada "Tareas N", se abre su detalle con el nombre
  en modo edición, el sobretítulo "PLANTILLA · TAREAS" y el placeholder "Nueva tarea del preset…".
- Cuando toco "Trabajadores", entonces se crea una plantilla llamada "Trabajadores N", se abre su
  detalle con el nombre en modo edición y la lista de trabajadores vacía.
- Cuando pulso "Cancelar", entonces la hoja se cierra y no se crea nada.

### P3 — Consultar una plantilla y sus trabajadores
- Cuando toco una tarjeta de la lista, entonces se abre el detalle con el nombre de la plantilla, la
  etiqueta "N TRABAJADORES" y una fila por trabajador con avatar de iniciales, nombre, lápiz y papelera.
- Cuando pulso la flecha de volver o el botón Atrás del sistema, entonces regreso a la lista.

### P4 — Renombrar una plantilla
- Cuando toco el nombre de la plantilla, escribo "Equipo de planta" y pulso "Guardar" (o Enter), entonces
  el nombre cambia en el detalle y en la tarjeta de la lista.
- Cuando dejo el campo vacío y pulso "Guardar", entonces se conserva el nombre anterior.

### P5 — Añadir trabajadores
- Dado texto en "Nombre del trabajador…", cuando pulso "Agregar" o Done, entonces el trabajador aparece al
  final de la lista, el contador sube y el campo se vacía.
- Dado el campo vacío o solo espacios, cuando pulso "Agregar", entonces no ocurre nada.
- Puedo añadir tantos trabajadores como quiera (no hay límite); la lista hace scroll.

### P6 — Editar un trabajador
- Cuando pulso el lápiz de una fila, entonces la fila muestra un campo con el nombre actual, "Guardar" y X.
- Cuando cambio el texto y pulso "Guardar" (o Enter), entonces el nombre se actualiza y la fila vuelve al
  modo lectura; con texto vacío se conserva el anterior.
- Cuando pulso X (o Escape), entonces se descartan los cambios.
- Cuando pulso el lápiz de otra fila mientras edito una, entonces la primera vuelve a modo lectura sin
  guardar y la segunda entra en edición.

### P7 — Eliminar un trabajador
- Cuando pulso la papelera de una fila, entonces desaparece, el contador baja y aparece el Snackbar
  "Trabajador eliminado" con "Deshacer"; al deshacer vuelve a su posición.

### P8 — Eliminar una plantilla
- Cuando pulso "Eliminar plantilla", entonces aparece el diálogo de confirmación con el nombre y el número
  de trabajadores; "Cancelar" no hace nada y "Eliminar" borra la plantilla con sus trabajadores y vuelve
  a la lista.

### P10 — Plantilla de tareas como preset
Como usuario quiero definir una plantilla con las tareas habituales para crear listas a partir de ella.
- Dado una plantilla de tareas con 5 tareas, cuando la abro, entonces veo "5 TAREAS" y cada fila con su
  número de orden, texto, lápiz y papelera; puedo añadir, editar y eliminar igual que con trabajadores.
- Cuando elimino la plantilla, entonces el diálogo indica que las listas ya creadas en Checklist no se ven
  afectadas, y tras confirmar esas listas siguen intactas.

### P9 — Persistencia de plantillas
- Dado plantillas con trabajadores, cuando fuerzo el cierre de la app y la reabro, entonces todo sigue
  igual (nombres, orden y trabajadores).
- Dado una instalación con la base de datos en versión 1, cuando actualizo a la versión con Plantillas,
  entonces mis turnos y tareas se conservan y la pestaña Plantillas aparece vacía (migración 1 → 2).

