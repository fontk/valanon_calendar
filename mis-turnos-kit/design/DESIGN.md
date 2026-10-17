# Guía visual — Mis turnos

Referencia: `mockups/Main.dc.html`, `mockups/Selector.dc.html`, `mockups/Checklist.dc.html` (390×844).
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
   título de mes centrado, botón redondo 44 dp.
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

## Pantalla Checklist
1. Cabecera: sobretítulo, "Checklist" 22/800 y progreso "3 de 6 hechas" 13/700 `textMuted` alineados en
   la línea base.
2. Alta: fila con gap 8; campo 50 dp (radio 14, fondo `surfaceInput`, borde `line`, padding 16, texto
   16/600, placeholder "Nueva tarea…") y botón "Agregar" 50 dp (padding 18, fondo `accent`, icono + 18 dp).
3. Lista: gap 8, padding inferior 100 dp para que el FAB no tape la última fila. Fila: min 60 dp, radio 14,
   borde `lineSoft`, padding 8/8/8/14; casilla 24 dp en `accent`; texto 16/600; papelera 44 dp con icono
   20 dp en `danger`. Hecha → fondo `surfaceInput`, texto `textFaint` tachado.
4. Estado vacío centrado a 80 dp del alta: icono 44 dp gris #B9B9B3, "Sin tareas todavía" 15/700, ayuda
   13/500 a 220 dp de ancho máximo.
5. FAB 60 dp, radio 18, fondo `accent`, icono reiniciar 26 dp, a 20 dp del borde derecho y 20 dp sobre la
   barra inferior, sombra `elevation.fab`.

## Iconos (trazo 2 dp, esquinas y remates redondeados, 24×24 viewBox)
- Calendario: `rect 3,5 18×16 r2` + línea y=10 + dos anillas x=8 y x=16 de y=3 a 7.
- Checklist: `rect 3,4 18×16 r2` + marca `M9 11l2 2 4-4`.
- Chevrons: `M15 6l-6 6 6 6` / `M9 6l6 6-6 6`.
- Documento-descarga: hoja doblada + flecha hacia abajo en el centro.
- Más: `M12 5v14M5 12h14` (trazo 2.4).
- Papelera: tapa, cuerpo y dos líneas verticales.
- Reiniciar: dos arcos con flechas (`M3 12a9 9 0 0 1 15.5-6.3L21 8` … `M21 12a9 9 0 0 1-15.5 6.3L3 16`).
Los paths exactos están en los mockups; conviértelos a `ImageVector` con `path { moveTo… }` o impórtalos
como Vector Drawable.

## Tema oscuro (opcional, v1.1)
Fondo #141413, superficies #1F1F1D, texto #F3F3EF, líneas #2E2E2B. Mantener los colores de turno; en
"noche" usar borde #6E68C9 para que la celda no se funda con el fondo.
