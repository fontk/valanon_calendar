-- Mis turnos — esquema SQLite (Room, versión 1)
-- Fechas en ISO 8601 (yyyy-MM-dd). Valores de turno en minúsculas y sin tilde.

PRAGMA foreign_keys = ON;

-- Versión 5: los tipos de turno son datos configurables (spec 3c). Hasta la v4 `turnos.tipo` y
-- `asignaciones.tipo` eran texto ('libre', …); Migration(4, 5) crea `tipos_turno` con las cinco filas
-- por defecto y recrea ambas tablas con `tipo_id`, mapeando el texto antiguo por `clave`.
CREATE TABLE IF NOT EXISTS tipos_turno (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    clave       TEXT    NULL UNIQUE,         -- 'libre' | 'manana' | 'tarde' | 'noche' | 'vacaciones'; NULL = personalizado
    nombre      TEXT    NOT NULL,            -- máx. 20 caracteres, único sin distinguir mayúsculas (lo valida la app)
    abreviatura TEXT    NOT NULL,            -- 1–5 caracteres en mayúsculas
    color       TEXT    NOT NULL,            -- clave de PaletaTurnos (16 entradas, en código)
    orden       INTEGER NOT NULL
);

-- Filas por defecto (las inserta la migración; ids fijos 1–5)
-- INSERT INTO tipos_turno (id, clave, nombre, abreviatura, color, orden) VALUES
--   (1, 'libre',      'Libre',      'LIBRE', 'gris',     1),
--   (2, 'manana',     'Mañana',     'MAÑ',   'amarillo', 2),
--   (3, 'tarde',      'Tarde',      'TARDE', 'azul',     3),
--   (4, 'noche',      'Noche',      'NOCHE', 'indigo',   4),
--   (5, 'vacaciones', 'Vacaciones', 'VAC',   'verde',    5);

CREATE TABLE IF NOT EXISTS turnos (
    fecha   TEXT    NOT NULL PRIMARY KEY,                     -- 'yyyy-MM-dd'
    tipo_id INTEGER NOT NULL REFERENCES tipos_turno (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_turnos_tipo ON turnos (tipo_id);

-- Versión 4: las tareas pertenecen a una lista (spec 4). En v1–v3 `tareas` no tenía `lista_id`;
-- Migration(3, 4) crea `listas`, inserta "Mi checklist" si había tareas y recrea `tareas` con la columna.
CREATE TABLE IF NOT EXISTS listas (
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    nombre       TEXT    NOT NULL,
    plantilla_id INTEGER NULL REFERENCES plantillas (id) ON DELETE SET NULL, -- plantilla de tareas de origen (informativo)
    orden        INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS tareas (
    id       INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    lista_id INTEGER NOT NULL REFERENCES listas (id) ON DELETE CASCADE,
    texto    TEXT    NOT NULL,
    hecha    INTEGER NOT NULL DEFAULT 0 CHECK (hecha IN (0, 1)), -- booleano
    orden    INTEGER NOT NULL                                     -- orden dentro de la lista
);

CREATE INDEX IF NOT EXISTS idx_tareas_lista ON tareas (lista_id, orden);

-- Versión 2 del esquema: plantillas reutilizables (spec 4b). Migration(1, 2) crea solo lo siguiente.
CREATE TABLE IF NOT EXISTS plantillas (
    id     INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tipo   TEXT    NOT NULL CHECK (tipo IN ('trabajadores', 'tareas')),  -- punto de extensión para nuevos tipos
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

-- Versión 4: elementos de una plantilla de tipo 'tareas' (preset)
CREATE TABLE IF NOT EXISTS tareas_plantilla (
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    plantilla_id INTEGER NOT NULL REFERENCES plantillas (id) ON DELETE CASCADE,
    texto        TEXT    NOT NULL,
    orden        INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tareas_plantilla ON tareas_plantilla (plantilla_id, orden);

-- Versión 3 del esquema: turnos del equipo (spec 3b). Migration(2, 3) crea solo lo siguiente.
CREATE TABLE IF NOT EXISTS asignaciones (
    id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    fecha         TEXT    NOT NULL,                              -- 'yyyy-MM-dd'
    trabajador_id INTEGER NOT NULL REFERENCES trabajadores (id) ON DELETE CASCADE,
    tipo_id       INTEGER NOT NULL REFERENCES tipos_turno (id) ON DELETE CASCADE,
    UNIQUE (fecha, trabajador_id)                                -- un turno por trabajador y día
);

CREATE INDEX IF NOT EXISTS idx_asignaciones_tipo ON asignaciones (tipo_id);

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
-- INSERT OR REPLACE INTO turnos (fecha, tipo_id) VALUES (:fecha, :tipoId);

-- Quitar el turno de un día (solo si se añade la opción "Sin turno")
-- DELETE FROM turnos WHERE fecha = :fecha;

-- Listas con progreso (chips de Checklist)
-- SELECT l.*, COUNT(t.id) AS total, SUM(t.hecha) AS hechas FROM listas l
--   LEFT JOIN tareas t ON t.lista_id = l.id GROUP BY l.id ORDER BY l.orden ASC;

-- Tareas de la lista activa en orden
-- SELECT * FROM tareas WHERE lista_id = :listaId ORDER BY orden ASC;

-- Nueva tarea al final de la lista activa
-- INSERT INTO tareas (lista_id, texto, hecha, orden)
--   VALUES (:listaId, :texto, 0, COALESCE((SELECT MAX(orden) FROM tareas WHERE lista_id = :listaId), 0) + 1);

-- Nueva lista desde una plantilla de tareas (en una transacción)
-- INSERT INTO listas (nombre, plantilla_id, orden)
--   VALUES (:nombre, :plantillaId, COALESCE((SELECT MAX(orden) FROM listas), 0) + 1);
-- INSERT INTO tareas (lista_id, texto, hecha, orden)
--   SELECT last_insert_rowid(), texto, 0, orden FROM tareas_plantilla WHERE plantilla_id = :plantillaId ORDER BY orden;

-- Renombrar / eliminar lista (borra sus tareas en cascada)
-- UPDATE listas SET nombre = :nombre WHERE id = :id;
-- DELETE FROM listas WHERE id = :id;

-- Marcar / desmarcar
-- UPDATE tareas SET hecha = :hecha WHERE id = :id;

-- Borrar una tarea
-- DELETE FROM tareas WHERE id = :id;

-- Botón Reiniciar de la tarjeta de la lista activa (no borra nada)
-- UPDATE tareas SET hecha = 0 WHERE lista_id = :listaId;

-- Lista activa de Checklist
-- SELECT valor FROM ajustes WHERE clave = 'checklist.lista_id';
-- INSERT OR REPLACE INTO ajustes (clave, valor) VALUES ('checklist.lista_id', :listaId);

-- Migración 3 → 4 (esquema de referencia; Room la ejecuta en MIGRATION_3_4)
-- CREATE TABLE listas (...); CREATE TABLE tareas_plantilla (...);
-- INSERT INTO listas (nombre, plantilla_id, orden) SELECT 'Mi checklist', NULL, 1 WHERE EXISTS (SELECT 1 FROM tareas);
-- CREATE TABLE tareas_nueva (... lista_id ...);
-- INSERT INTO tareas_nueva (id, lista_id, texto, hecha, orden)
--   SELECT id, (SELECT id FROM listas LIMIT 1), texto, hecha, orden FROM tareas;
-- DROP TABLE tareas; ALTER TABLE tareas_nueva RENAME TO tareas; CREATE INDEX idx_tareas_lista ...;
-- INSERT OR REPLACE INTO ajustes (clave, valor) SELECT 'checklist.lista_id', id FROM listas LIMIT 1;

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
-- INSERT OR REPLACE INTO asignaciones (fecha, trabajador_id, tipo_id) VALUES (:fecha, :trabajadorId, :tipoId);

-- "Sin asignar"
-- DELETE FROM asignaciones WHERE fecha = :fecha AND trabajador_id = :trabajadorId;

-- Número de turnos asignados de una plantilla (para el diálogo de eliminar plantilla)
-- SELECT COUNT(*) FROM asignaciones a JOIN trabajadores t ON t.id = a.trabajador_id
--   WHERE t.plantilla_id = :plantillaId;

-- Plantilla activa de la pestaña Turnos
-- SELECT valor FROM ajustes WHERE clave = 'turnos.plantilla_id';
-- INSERT OR REPLACE INTO ajustes (clave, valor) VALUES ('turnos.plantilla_id', :plantillaId);

-- Tipos de turno (Ajustes)
-- SELECT * FROM tipos_turno ORDER BY orden ASC;
-- INSERT INTO tipos_turno (clave, nombre, abreviatura, color, orden)
--   VALUES (NULL, :nombre, :abreviatura, :color, COALESCE((SELECT MAX(orden) FROM tipos_turno), 0) + 1);
-- UPDATE tipos_turno SET nombre = :nombre, abreviatura = :abreviatura, color = :color WHERE id = :id;
-- Usos de un tipo (para el diálogo de eliminar)
-- SELECT (SELECT COUNT(*) FROM turnos WHERE tipo_id = :id), (SELECT COUNT(*) FROM asignaciones WHERE tipo_id = :id);
-- Eliminar (borra en cascada sus días en turnos y asignaciones)
-- DELETE FROM tipos_turno WHERE id = :id;
-- Restablecer los cinco por defecto: por cada clave, UPDATE si existe, INSERT al final si no.

-- Migración 4 → 5 (esquema de referencia; Room la ejecuta en MIGRATION_4_5)
-- CREATE TABLE tipos_turno (...); INSERT las 5 filas por defecto;
-- CREATE TABLE turnos_nueva (fecha TEXT PK, tipo_id INTEGER NOT NULL REFERENCES tipos_turno(id) ON DELETE CASCADE);
-- INSERT INTO turnos_nueva (fecha, tipo_id) SELECT t.fecha, tt.id FROM turnos t JOIN tipos_turno tt ON tt.clave = t.tipo;
-- DROP TABLE turnos; ALTER TABLE turnos_nueva RENAME TO turnos; CREATE INDEX idx_turnos_tipo ...;
-- (igual para asignaciones, conservando id, fecha y trabajador_id; recrear idx_asignaciones_fecha_trabajador,
--  idx_asignaciones_trabajador e idx_asignaciones_tipo)

