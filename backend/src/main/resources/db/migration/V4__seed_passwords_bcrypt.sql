-- Los password_hash del seed (V2) son strings inventados, no hashes BCrypt validos:
-- el login nunca podria autenticar contra ellos. Los reemplazamos por un hash real
-- de la contrasena de prueba "Password123!" (documentada en DOMAIN.md/README) para
-- los 4 usuarios semilla.

UPDATE usuario
   SET password_hash = '$2a$10$RuaFPLYBWd5nl9bXRoo4Xe2kkBh7CEFUxBCTak4mj4ugGjuIIeEgy'
 WHERE email IN (
     'vendedor@test.com',
     'comprador1@test.com',
     'comprador2@test.com',
     'sinfondos@test.com'
 );
