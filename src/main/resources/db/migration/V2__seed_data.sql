INSERT INTO coworking.users (id, username, email, password, role, created_at)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'admin', 'admin@coworking.com', '$2a$10$Z7quqKeiQCrzVuiyDMgD2uiDxtAVohkmgg6bFrFWd8062JsndQvO2', 'ADMIN', NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 'user', 'user@coworking.com', '$2a$10$WV.rC0MoIfB.T6SYNiHIeu.0Pw4PYtAN6cdTTL/e3KXvDtJkDsHAK', 'USER', NOW());

INSERT INTO coworking.spaces (id, name, description, capacity, location, price_per_hour, status, created_at)
VALUES
    ('660e8400-e29b-41d4-a716-446655440001', 'Sala de Juntas A', 'Sala moderna para 8 personas con proyector y pizarra digital', 8, 'Piso 1, Torre A', 25.00, 'AVAILABLE', NOW()),
    ('660e8400-e29b-41d4-a716-446655440002', 'Puesto de Trabajo 1', 'Escritorio individual en espacio abierto', 1, 'Piso 2, Zona Comun', 10.00, 'AVAILABLE', NOW()),
    ('660e8400-e29b-41d4-a716-446655440003', 'Sala de Conferencias B', 'Sala grande para 20 personas con sistema de videoconferencia', 20, 'Piso 3, Torre B', 50.00, 'AVAILABLE', NOW());
