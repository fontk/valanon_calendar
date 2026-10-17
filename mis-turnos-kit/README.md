# Mis turnos — kit de arranque para un agente

Este paquete contiene todo lo necesario para que un agente de programación implemente la app Android
"Mis turnos" a partir del diseño ya realizado. Empieza leyendo `AGENTS.md` y después `spec/SPEC.md`.

## Contenido

| Ruta | Qué es |
|---|---|
| `AGENTS.md` | Instrucciones para el agente: objetivo, stack recomendado, orden de trabajo, criterios de aceptación. |
| `spec/SPEC.md` | Especificación funcional completa de las dos pestañas (Calendario y Checklist). |
| `spec/USER_STORIES.md` | Historias de usuario con criterios de aceptación verificables. |
| `spec/TASKS.md` | Plan de trabajo por fases, listo para ir marcando. |
| `db/schema.sql` | Esquema SQLite (tablas `turnos`, `tareas`, `plantillas`, `trabajadores`, `asignaciones`, `ajustes`) y consultas de referencia. |
| `design/DESIGN.md` | Guía visual: colores, tipografía, espaciados, componentes y estados. |
| `design/tokens/tokens.json` | Tokens de diseño en JSON (colores, radios, tamaños) para traducir a `Color.kt`/`Theme.kt`. |
| `design/mockups/*.dc.html` | Los diez artboards del diseño (Calendario, selector de turno, selector de mes y año, Checklist, Plantillas en tres estados y Turnos en cuatro estados). Son HTML autocontenido: el marcado y los estilos inline son la referencia exacta de layout; el bloque `<script>` final es la lógica de la maqueta y sirve como pseudocódigo del comportamiento. |

## Decisiones ya tomadas (no reabrir sin motivo)

- Plataforma: Android nativo, Kotlin + Jetpack Compose, Material 3 con tema propio.
- Cuatro pestañas: Calendario, Turnos, Checklist, Plantillas (en ese orden).
- Persistencia: SQLite a través de Room.
- Idioma de la interfaz: español (textos literales en `spec/SPEC.md`).
- Semana empieza en lunes. Cabeceras de día: L M X J V S D.
- Turnos posibles, exactamente estos cinco: `libre`, `manana`, `tarde`, `noche`, `vacaciones`.
- Exportación a PDF del mes visible con `android.graphics.pdf.PdfDocument` (sin dependencias externas).
