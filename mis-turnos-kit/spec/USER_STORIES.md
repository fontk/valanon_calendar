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

## Checklist

### L1 — Añadir tarea
Como usuario quiero escribir una tarea y agregarla para no olvidarla.
- Dado texto en el campo, cuando pulso "Agregar" o la acción Done del teclado, entonces la tarea aparece
  al final de la lista sin marcar y el campo queda vacío.
- Dado el campo vacío o solo espacios, cuando pulso "Agregar", entonces no ocurre nada.

### L2 — Marcar y desmarcar
Como usuario quiero marcar una tarea como hecha y poder desmarcarla.
- Cuando toco la casilla, entonces la tarea alterna su estado, se tacha y el contador "X de Y hechas"
  se actualiza.

### L3 — Borrar tarea
Como usuario quiero borrar una tarea con el icono de papelera.
- Cuando pulso la papelera de una tarea, entonces desaparece de la lista y de la base de datos.

### L4 — Reiniciar checks
Como usuario quiero un botón flotante que desmarque todas las tareas para reutilizar la lista cada día.
- Dado varias tareas marcadas, cuando pulso el FAB, entonces todas quedan desmarcadas y ninguna se borra.

### L5 — Persistencia de tareas
- Dado tareas creadas y algunas marcadas, cuando cierro y reabro la app, entonces la lista y los estados
  son idénticos.

### L6 — Estado vacío
- Dado que no hay tareas, entonces se muestra "Sin tareas todavía" con la ayuda "Escribe una tarea arriba
  y pulsa Agregar." y no se muestra el contador de progreso.
