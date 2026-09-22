-- ============================================================================
-- Script: Crear Rol VISITANTE
-- Fecha: 2026-08-11
-- ============================================================================

INSERT INTO roles (id,name, description) VALUES
    ('VISITANTE','Visitante', 'Rol visitante');

-- ============================================================================
-- Verificación (descomentar para verificar)
-- ============================================================================
-- SELECT id, name, description
-- FROM roles
-- WHERE r.id = 'VISITANTE';
