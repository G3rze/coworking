-- Seed data for Coworking Space Management API
-- Passwords are BCrypt encoded:
-- admin123 -> $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQb9OX.O7oHx7u6kUqN3jPVEOhWG
-- user123  -> $2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6

INSERT INTO coworking.users (id, username, email, password, role)
VALUES
  ('550e8400-e29b-41d4-a716-446655440001', 'admin', 'admin@coworking.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQb9OX.O7oHx7u6kUqN3jPVEOhWG', 'ADMIN'),
  ('550e8400-e29b-41d4-a716-446655440002', 'user', 'user@coworking.com', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6', 'USER');

INSERT INTO coworking.spaces (id, name, description, capacity, location, price_per_hour, status)
VALUES
  ('660e8400-e29b-41d4-a716-446655440001', 'Sala de Juntas A', 'Sala pequeña para 4 personas con pizarrón', 4, 'Piso 1', 150.00, 'AVAILABLE'),
  ('660e8400-e29b-41d4-a716-446655440002', 'Sala de Juntas B', 'Sala mediana para 8 personas con pantalla', 8, 'Piso 1', 250.00, 'AVAILABLE'),
  ('660e8400-e29b-41d4-a716-446655440003', 'Oficina Privada 1', 'Oficina individual con escritorio y silla ergonómica', 1, 'Piso 2', 100.00, 'AVAILABLE'),
  ('660e8400-e29b-41d4-a716-446655440004', 'Sala de Conferencias', 'Sala grande para 20 personas con proyector', 20, 'Piso 3', 500.00, 'AVAILABLE'),
  ('660e8400-e29b-41d4-a716-446655440005', 'Espacio Coworking', 'Espacio abierto compartido con internet de alta velocidad', 50, 'Piso 1', 50.00, 'AVAILABLE');
