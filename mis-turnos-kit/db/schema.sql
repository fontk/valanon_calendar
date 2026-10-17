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

-- Versión 2 del esquema: plantillas reutilizables (spec 4b). Migration(1, 2) crea solo lo siguiente.
CREATE TABLE IF NOT EXISTS plantillas (
    id     INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tipo   TEXT    NOT NULL CHECK (tipo IN ('trabajadores')),  -- punto de extensión para nuevos tipos
    nombre TEXT    NOT NULL,
    orden  INTEGER NOT NULL                                     -- orden de creación, ascendente
);

CREATE TABLE IF NOT EXISTS trabajadores (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    plantilla_id INTEGER NOT NULL REFERENCES plantillas (id) ON DELETE CASCADE,
    nombre      TEXT    NOT NULL,
    orden       INTEGER NOT NULL                                -- orden dentro de la plantilla
);

CREATE INDEX IF NOT EXISTS idx_trabajadores_plantilla ON trabajadores (plantilla_id, orden);

-- Versión 3 del esquema: turnos del equipo (spec 3b). Migration(2, 3) crea solo lo siguiente.
CREATE TABLE IF NOT EXISTS asignaciones (
    id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    fecha         TEXT    NOT NULL,                              -- 'yyyy-MM-dd'
    trabajador_id INTEGER NOT NULL REFERENCES trabajadores (id) ON DELETE CASCADE,
    tipo          TEXT    NOT NULL
                  CHECK (tipo IN ('libre', 'manana', 'tarde', 'noche', 'vacaciones')),
    UNIQUE (fecha, trabajador_id)                                -- un turno por trabajador y día
);

CREATE INDEX IF NOT EXISTS idx_asignaciones_trabajador ON asignaciones (trabajador_id);

CREATE TABLE IF NOT EXISTS ajustes (
    clave TEXT NOT NULL PRIMARY KEY,                             -- p. ej. 'turnos.plantilla_id'
    valor TEXT NOT NULL
);

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

-- Plantillas: lista con recuento de trabajadores
-- SELECT p.*, COUNT(t.id) AS trabajadores FROM plantillas p
--   LEFT JOIN trabajadores t ON t.plantilla_id = p.id
--   GROUP BY p.id ORDER BY p.orden ASC;

-- Nueva plantilla al final
-- INSERT INTO plantillas (tipo, nombre, orden)
--   VALUES (:tipo, :nombre, COALESCE((SELECT MAX(orden) FROM plantillas), 0) + 1);

-- Renombrar plantilla / borrar plantilla (borra sus trabajadores en cascada)
-- UPDATE plantillas SET nombre = :nombre WHERE id = :id;
-- DELETE FROM plantillas WHERE id = :id;

-- Trabajadores de una plantilla, en orden
-- SELECT * FROM trabajadores WHERE plantilla_id = :plantillaId ORDER BY orden ASC;

-- Nuevo trabajador al final de su plantilla
-- INSERT INTO trabajadores (plantilla_id, nombre, orden)
--   VALUES (:plantillaId, :nombre,
--           COALESCE((SELECT MAX(orden) FROM trabajadores WHERE plantilla_id = :plantillaId), 0) + 1);

-- Editar / eliminar trabajador
-- UPDATE trabajadores SET nombre = :nombre WHERE id = :id;
-- DELETE FROM trabajadores WHERE id = :id;

-- Turnos del equipo: asignaciones del mes visible para la plantilla activa
-- SELECT a.* FROM asignaciones a
--   JOIN trabajadores t ON t.id = a.trabajador_id
--   WHERE t.plantilla_id = :plantillaId AND a.fecha BETWEEN :desde AND :hasta;

-- Asignar / cambiar el turno de un trabajador un día (guardado inmediato desde el desplegable)
-- INSERT OR REPLACE INTO asignaciones (fecha, trabajador_id, tipo) VALUES (:fecha, :trabajadorId, :tipo);

-- "Sin asignar"
-- DELETE FROM asignaciones WHERE fecha = :fecha AND trabajador_id = :trabajadorId;

-- Número de turnos asignados de una plantilla (para el diálogo de eliminar plantilla)
-- SELECT COUNT(*) FROM asignaciones a JOIN trabajadores t ON t.id = a.trabajador_id
--   WHERE t.plantilla_id = :plantillaId;

-- Plantilla activa de la pestaña Turnos
-- SELECT valor FROM ajustes WHERE clave = 'turnos.plantilla_id';
-- INSERT OR REPLACE INTO ajustes (clave, valor) VALUES ('turnos.plantilla_id', :plantillaId);

