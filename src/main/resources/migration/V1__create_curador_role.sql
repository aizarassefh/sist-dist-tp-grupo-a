-- ============================================================================
-- Script: Crear Rol CURADOR
-- Fecha: 2026-08-11
-- ============================================================================


INSERT INTO roles (id,name, description) VALUES
    ('CURADOR','Curador', 'Rol curador');

-- ============================================================================
-- Verificación (descomentar para verificar)
-- ============================================================================
-- SELECT id, name, description
-- FROM roles
-- WHERE r.id = 'CURADOR';
