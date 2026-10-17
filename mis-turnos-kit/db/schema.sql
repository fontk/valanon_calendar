-- Mis turnos — esquema SQLite (Room, versión 1)
-- Fechas en ISO 8601 (yyyy-MM-dd). Valores de turno en minúsculas y sin tilde.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS turnos (
    fecha TEXT    NOT NULL PRIMARY KEY,                       -- 'yyyy-MM-dd'
    tipo  TEXT    NOT NULL
          CHECK (tipo IN ('libre', 'manana', 'tarde', 'noche', 'vacaciones'))
);

CREATE TABLE IF NOT EXISTS tareas (
    id    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    texto TEXT    NOT NULL,
    hecha INTEGER NOT NULL DEFAULT 0 CHECK (hecha IN (0, 1)), -- booleano
    orden INTEGER NOT NULL                                     -- orden de creación, ascendente
);

CREATE INDEX IF NOT EXISTS idx_tareas_orden ON tareas (orden);

-- ---------------------------------------------------------------
-- Consultas de referencia (equivalentes a los métodos de los DAO)
-- ---------------------------------------------------------------

-- Turnos del mes visible (Flow en Room)
-- SELECT * FROM turnos WHERE fecha BETWEEN :desde AND :hasta;
--   ej. desde = '2026-10-01', hasta = '2026-10-31'

-- Asignar o cambiar el turno de un día
-- INSERT OR REPLACE INTO turnos (fecha, tipo) VALUES (:fecha, :tipo);

-- Quitar el turno de un día (solo si se añade la opción "Sin turno")
-- DELETE FROM turnos WHERE fecha = :fecha;

-- Todas las tareas en orden
-- SELECT * FROM tareas ORDER BY orden ASC;

-- Nueva tarea al final de la lista
-- INSERT INTO tareas (texto, hecha, orden)
--   VALUES (:texto, 0, COALESCE((SELECT MAX(orden) FROM tareas), 0) + 1);

-- Marcar / desmarcar
-- UPDATE tareas SET hecha = :hecha WHERE id = :id;

-- Borrar una tarea
-- DELETE FROM tareas WHERE id = :id;

-- Botón flotante: reiniciar todos los checks (no borra nada)
-- UPDATE tareas SET hecha = 0;
