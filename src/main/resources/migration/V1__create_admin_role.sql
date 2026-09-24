-- ============================================================================
-- Script: Crear Rol ADMIN
-- Descripción: Inserta el rol ADMINISTRADOR
-- Fecha: 2026-08-11
-- ============================================================================

-- 2. Crear el rol ADMIN
INSERT INTO roles (id,name, description) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Rol administrador - SuperAdmin');


-- ============================================================================
-- Verificación (descomentar para verificar)
-- ============================================================================
-- SELECT id, name, description
-- FROM roles
-- WHERE r.id = 'ADMIN';
