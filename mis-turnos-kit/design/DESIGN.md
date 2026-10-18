# Guía visual — Mis turnos

Referencia: `mockups/Main.dc.html`, `mockups/Selector.dc.html`, `mockups/SelectorMes.dc.html`,
`mockups/Checklist.dc.html`, `mockups/Plantillas.dc.html`, `mockups/PlantillasTipo.dc.html`,
`mockups/PlantillaDetalle.dc.html`, `mockups/Turnos.dc.html`, `mockups/TurnosDia.dc.html`,
`mockups/TurnosEditar.dc.html`, `mockups/TurnosSelector.dc.html`, más los estados adicionales
`PlantillasVacio`, `PlantillaEditar`, `PlantillaBorrar`, `TurnosVacio`, `TurnosPlantilla` y `TurnosGrande`
y `PlantillaTareas`, `Checklist`, `ChecklistNueva`, `ChecklistBorrar`, `ChecklistVacio` (390×844). Los
siete de Plantillas, los siete de Turnos y los cuatro de Checklist son, en cada caso, el mismo componente
con la pantalla inicial distinta (prop `pantallaInicial`: Plantillas → lista / vacio / tipos / detalle /
editar / borrar / tareas; Turnos → calendario / vacio / plantilla / grande / dia / editar / selector;
Checklist → lista / nueva / borrar / vacio).
Los valores numéricos están en `tokens/tokens.json`; aquí se explica cómo se usan.

## Principios
- Fondo blanco, una sola tinta de acento (verde azulado #0F6E64) para acciones primarias, pestaña activa y
  el día de hoy. Nada de degradados, sombras salvo en el FAB, ni emojis.
- Los cinco turnos se distinguen por tono **y** por luminosidad (claro: libre, mañana, tarde, vacaciones;
  oscuro: noche), de modo que son legibles también para daltónicos y en impresión en gris.
- Tipografía Manrope en pesos 500–800. Títulos 22 sp/800, cuerpo 16 sp/600, etiquetas 12 sp/700 en
  mayúsculas con tracking amplio.
- Esquinas redondeadas generosas: 12 dp celdas, 14 dp controles, 18 dp FAB, 24 dp hoja inferior.
- Todo elemento tocable ≥ 44 dp.

## Pantalla Calendario (Main)
1. Cabecera (padding 22/20/10): sobretítulo "MIS TURNOS"; fila con botón redondo 44 dp (borde `line`),
   selector de mes y año centrado y botón redondo 44 dp. El selector es una píldora de 44 dp (radio 22,
   fondo `surface`, padding horizontal 14, gap 6) con "Octubre 2026" en 20 sp/800 y un chevron hacia abajo
   de 18 dp (trazo 2.4, color `textMuted`). Es el único elemento de la cabecera con fondo, para que se lea
   como tocable.
2. Leyenda: chips píldora (padding 5×10) con punto 8 dp y texto 12 sp; usan el `bg`/`fg` del turno; gap 6.
3. Cabecera de semana: 7 columnas, 12 sp/700 `textFaint`, padding lateral 16.
4. Rejilla: 7 columnas, gap 6, celdas 60 dp alto, radio 12. Número 15 sp/800 y abreviatura 10 sp/700 en
   columna centrada con gap 2. Hoy: borde 2 dp `accent`.
5. Botón "Exportar a PDF": margen 10/20/12, alto 52, radio 14, fondo `accent`, icono 20 dp + texto 16/700,
   gap 10.
6. Barra inferior 72 dp con borde superior `lineSoft`.

## Hoja de selección (Selector)
- Scrim `rgba(20,20,19,0.45)` sobre toda la pantalla; hoja blanca anclada abajo, radio 24 arriba,
  padding 12/20/28, gap 14.
- Asa 36×4 `handle`, sobretítulo "TURNO DEL DÍA", título fecha 20 sp/800.
- Opciones: filas 52 dp, radio 14, padding horizontal 14, gap 12: cuadrado 28 dp (radio 8) con el
  `swatch` del turno, nombre 16/700, y a la derecha "Actual" 13/700 en `accent` cuando corresponde.
  Borde 2 dp `accent` en la actual, 1 dp `line` en el resto.
- "Cancelar": 48 dp, radio 14, fondo `surface`, texto 15/700.

## Hoja de selección de mes y año (SelectorMes)
- Misma hoja que el selector de turno: scrim, radio 24 arriba, padding 12/20/28, gap 14, asa 36×4.
- Sobretítulo "IR A MES". Fila de año: botones redondos 44 dp (borde `line`) a los lados y el año en
  24 sp/800 centrado.
- Rejilla 3 columnas × 4 filas, gap 8, botones 52 dp radio 14, abreviaturas de tres letras en 15 sp/700.
  Activo: fondo `accent`, texto blanco, borde 2 dp `accent`. Mes de hoy: borde 2 dp #B9CFCB (acento al
  30 % sobre blanco). Resto: borde 1 dp `line`.
- Fila final con "Hoy" (contorno `accent`, texto `accent`) y "Cancelar" (fondo `surface`), 48 dp cada uno,
  mismo ancho.
- En tema oscuro: fondo de la hoja = superficie oscura, botones de mes con borde `line` oscuro, el activo
  mantiene `accent`; "hoy" usa `AccentOnBackground` al 40 %.

## Pestaña Turnos (Turnos, TurnosDia, TurnosEditar)
1. Cabecera igual que Calendario con el sobretítulo "TURNOS DEL EQUIPO". Debajo (padding 6/20/12) el chip
   de plantilla activa: 36 dp, radio 999, borde `line`, padding 0/12/0/8, gap 8, insignia 24 dp radio 7
   (`surfaceAccent` + icono 15 dp en `accent`), nombre 13/700 con elipsis y chevron 16 dp; a la derecha el
   resumen 12/700 `textMuted`.
2. Rejilla idéntica a Calendario pero las celdas no llevan color de turno: número 15/800 y debajo una fila
   (gap 3, alto 8) de puntos de 7 dp con el `dot` del turno de cada trabajador asignado; máximo 5 puntos y
   "+N" en 9/800 `textMuted`. Día con asignaciones: fondo #FAFAF8; hoy: borde 2 dp `accent`.
3. Leyenda de turnos bajo la rejilla (chips 4×10) y barra inferior de cuatro pestañas.
4. Hoja "Equipo del día": misma hoja base; cabecera en fila con sobretítulo, fecha 20/800, subtítulo 13/600
   y el botón de modo a la derecha ("Editar" contorno `accent` 40 dp con lápiz 16 dp; "Listo" relleno
   `accent`). Filas de 56 dp mínimo, radio 14, borde `lineSoft`, padding 6/8/6/10, gap 12: avatar 36 dp
   con iniciales 13/800, nombre 15/700, y chip de turno (6×12, radio 999, colores del turno; "Sin asignar"
   con borde discontinuo #C9C9C4 y texto `textFaint`) o, en edición, botón-chip (40 dp, radio 12, borde
   2 dp `dot` del turno, fondo/texto del turno, 13/700, chevron derecho 16 dp). Botón "Cerrar" 48 dp `surface`.
4b. Selector de turno del trabajador: exactamente la hoja de "Calendario · selector de turno" (opciones
   52 dp, cuadrado 28 dp radio 8, "Actual" en `accent`), con cabecera en fila: botón volver 44 dp fondo
   `surface` + sobretítulo (fecha) + título "Turno de <nombre>" 20/800 con elipsis; fila final "Quitar
   turno" (borde #E6C9C3, texto `danger`, solo si tiene turno) y "Cancelar" (`surface`), 48 dp cada uno.
   Sustituye al contenido de la hoja del día (no se apilan dos hojas).
5. Hoja "¿Qué equipo quieres ver?": filas 56 dp radio 14 con nombre 16/700 y recuento 13/600; la activa con
   borde 2 dp `accent`.

## Pantalla Checklist (Checklist, ChecklistNueva, ChecklistBorrar, ChecklistVacio)
1. Cabecera: sobretítulo, "Checklist" 22/800 y progreso de la lista activa "2 de 5 hechas" 13/700
   `textMuted` alineados en la línea base.
2. Fila de chips de lista con scroll horizontal (padding 2/20/14, gap 8): chips de 36 dp, radio 999,
   padding 0/12, texto 13/700 + píldora de progreso "2/5" (11/800, padding 2×7, radio 999). Activa: fondo
   `accent`, texto blanco, píldora blanca al 22 %; resto: borde `line`, píldora `surface`. Último chip
   "+ Nueva": borde 1 dp discontinuo `accent`, texto `accent` 13/800, icono + 16 dp.
3. Tarjeta de la lista activa: margen 0/20/12, padding 10/6/10/14, radio 16, fondo `surfaceInput`; nombre
   16/800 con lápiz 14 dp, subtítulo de origen 12/600 `textMuted`; a la derecha botón Reiniciar (44 dp,
   icono 22 dp en `accent`) y papelera (44 dp, icono 20 dp en `danger`). En renombrado: campo 40 dp con
   borde 2 dp `accent` + "Guardar".
4. Alta: fila con gap 8; campo 50 dp (radio 14, fondo blanco, borde `line`, padding 16, texto 16/600,
   placeholder "Nueva tarea…") y botón "Agregar" 50 dp (padding 18, fondo `accent`, icono + 18 dp).
5. Lista: gap 8, padding inferior 24 dp (ya no hay FAB). Fila: min 60 dp, radio 14, borde `lineSoft`,
   padding 8/8/8/14; casilla 24 dp en `accent`; texto 16/600; papelera 44 dp con icono 20 dp en `danger`.
   Hecha → fondo `surfaceInput`, texto `textFaint` tachado.
6. Estado vacío de la lista activa a 60 dp del alta: icono 44 dp gris #B9B9B3, "Esta lista está vacía"
   15/700, ayuda 13/500 a 220 dp. Sin listas: estado vacío centrado con insignia 64 dp (radio 20, fondo
   `surface`), título 17/800, ayuda 14/500 y botón relleno `accent` "Nueva lista" 48 dp.
7. Hoja "Nueva lista": misma hoja base; sobretítulo "NUEVA LISTA", título 20/800; tarjetas de plantilla
   64 dp mínimo (radio 16, borde `line`, insignia 40 dp #EDE9FF con icono checklist #4B43B8, nombre 16/800,
   "5 tareas" 13/600); aviso en caja `surfaceInput` 13/600 si no hay plantillas; "Lista en blanco" 56 dp
   con borde discontinuo #C9C9C4 e insignia `surface` con +; "Cancelar" 48 dp.
8. Diálogo de eliminar lista: igual que el de eliminar plantilla.
9. La insignia del tipo Tareas (Plantillas y Checklist) usa #EDE9FF / #4B43B8 (`tokens.templateTasks`),
   un violeta de la misma familia que el acento para distinguir el tipo sin competir con él.

## Pestaña Plantillas (Plantillas, PlantillasTipo, PlantillaDetalle)
1. Lista: cabecera igual que Checklist ("Plantillas" + contador). Tarjetas de 72 dp mínimo, radio 16,
   borde `lineSoft`, padding 12/10/12/14, gap 14 entre insignia, textos y chevron. Insignia 44 dp radio 12
   con fondo `surfaceAccent` e icono del tipo en `accent`. Nombre 16/800, subtítulo 13/600 `textMuted`.
   Gap entre tarjetas 10. FAB + igual que el de Checklist (icono + 28 dp, trazo 2.4).
2. Hoja de tipo: misma hoja que las demás (radio 24, padding 12/20/28, gap 14). Sobretítulo "NUEVA
   PLANTILLA", título 20/800, una tarjeta por tipo (72 dp mínimo, radio 16, borde `line`) con insignia,
   nombre 16/800, descripción 13/600 y chevron; "Cancelar" 48 dp fondo `surface`.
3. Detalle: barra de 14/12/10/8 con botón volver 44 dp sin fondo, sobretítulo y nombre 20/800 con lápiz
   16 dp en `textMuted`. En renombrado: campo 40 dp radio 12 borde 2 dp `accent` + botón "Guardar"
   (40 dp, fondo `accent`, 14/800). Alta idéntica a Checklist. Etiqueta de recuento en estilo sobretítulo.
   Filas de 60 dp mínimo, radio 14, padding 8/8/8/12, gap 12: avatar 36 dp fondo `surface` con iniciales
   13/800, nombre 16/600, lápiz 44 dp `textMuted`, papelera 44 dp `danger`. Fila en edición: borde 2 dp
   `accent`, campo 40 dp fondo `surfaceInput` sin borde, "Guardar" y X (40 dp, fondo `surface`).
   Botón "Eliminar plantilla": 48 dp, radio 14, borde #E6C9C3, texto e icono `danger`, 12 dp bajo la lista.
   Tipo Tareas (mockup `PlantillaTareas`): mismo detalle con avatar cuadrado (radio 10) que muestra el
   número de orden en lugar de las iniciales, sobretítulo "PLANTILLA · TAREAS" y textos propios.
   Diálogo de confirmación (`AlertDialog`, mockup `PlantillaBorrar`): tarjeta radio 24, padding 24, título
   20/800, cuerpo 15/500 `textMuted`, acciones alineadas a la derecha: "Cancelar" texto en `accent` y
   "Eliminar" relleno `danger` con texto blanco, 44 dp.
4. La barra inferior pasa a tres pestañas de igual ancho; la activa sigue siendo la única con píldora.

## Iconos (trazo 2 dp, esquinas y remates redondeados, 24×24 viewBox)
- Calendario: `rect 3,5 18×16 r2` + línea y=10 + dos anillas x=8 y x=16 de y=3 a 7.
- Checklist: `rect 3,4 18×16 r2` + marca `M9 11l2 2 4-4`.
- Chevrons: `M15 6l-6 6 6 6` / `M9 6l6 6-6 6`; chevron abajo (selector de mes): `M6 9l6 6 6-6`, trazo 2.4.
- Documento-descarga: hoja doblada + flecha hacia abajo en el centro.
- Más: `M12 5v14M5 12h14` (trazo 2.4).
- Papelera: tapa, cuerpo y dos líneas verticales.
- Reiniciar: dos arcos con flechas (`M3 12a9 9 0 0 1 15.5-6.3L21 8` … `M21 12a9 9 0 0 1-15.5 6.3L3 16`).
- Plantillas (capas): `M12 3l9 5-9 5-9-5 9-5z`, `M3 13l9 5 9-5`, `M3 17l9 5 9-5`.
- Trabajadores (dos personas): `circle 9,8 r3.5`, `M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6`,
  `circle 17,9 r2.5`, `M16 14.5c3 0 5.5 2 5.5 5`.
- Lápiz: `M12 20h9`, `M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z`.
- Flecha atrás: `M19 12H5`, `M11 6l-6 6 6 6`. Cerrar (X): `M6 6l12 12M18 6L6 18` (trazo 2.2).
- Turnos (reloj): `circle 12,12 r9`, `M12 7v5l3 2`.
Los paths exactos están en los mockups; conviértelos a `ImageVector` con `path { moveTo… }` o impórtalos
como Vector Drawable.

## Tema oscuro (opcional, v1.1)
Fondo #141413, superficies #1F1F1D, texto #F3F3EF, líneas #2E2E2B. Mantener los colores de turno; en
"noche" usar borde #6E68C9 para que la celda no se funda con el fondo.
