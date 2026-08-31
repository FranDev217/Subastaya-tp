-- Datos semilla obligatorios (ver consigna 3.3 y domain.md sección 3).
-- Los horarios de las subastas se calculan relativos a NOW() para que los
-- casos "activa crítica" / "próxima" / "vencida" sigan siendo válidos sin
-- importar cuándo se corra esta migración.

-- Usuarios
INSERT INTO usuario (email, nombre, password_hash) VALUES
    ('vendedor@test.com', 'Vendedor Demo', '$2a$10$demoHashVendedorxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'),
    ('comprador1@test.com', 'Comprador Uno', '$2a$10$demoHashComprador1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'),
    ('comprador2@test.com', 'Comprador Dos', '$2a$10$demoHashComprador2xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'),
    ('sinfondos@test.com', 'Sin Fondos', '$2a$10$demoHashSinFondosxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');

-- Billeteras (saldo_total = saldo_retenido + saldo_disponible en todos los casos)
INSERT INTO billetera (usuario_id, saldo_total, saldo_retenido, saldo_disponible)
SELECT id, 0, 0, 0 FROM usuario WHERE email = 'vendedor@test.com'
UNION ALL
SELECT id, 150000, 45000, 105000 FROM usuario WHERE email = 'comprador1@test.com'
UNION ALL
SELECT id, 200000, 0, 200000 FROM usuario WHERE email = 'comprador2@test.com'
UNION ALL
SELECT id, 500, 0, 500 FROM usuario WHERE email = 'sinfondos@test.com';

-- Categorías
INSERT INTO categoria (nombre, url_icono) VALUES
    ('Tecnología', '/icons/tecnologia.svg'),
    ('Coleccionables', '/icons/coleccionables.svg'),
    ('Indumentaria', '/icons/indumentaria.svg'),
    ('Vehículos', '/icons/vehiculos.svg');

-- Subastas (5 casos de prueba de la consigna)
INSERT INTO subasta (vendedor_id, categoria_id, titulo, descripcion, url_imagen, precio_base,
                      incremento_minimo, fecha_inicio, fecha_fin, estado)
SELECT (SELECT id FROM usuario WHERE email = 'vendedor@test.com'),
       (SELECT id FROM categoria WHERE nombre = 'Tecnología'),
       'Notebook Gamer RTX 4070',
       'Notebook gamer de alta gama, 32GB RAM, SSD 1TB.',
       'https://picsum.photos/seed/notebook/600/400',
       40000, 1000,
       now() - interval '1 hour', now() + interval '25 minutes', 'ACTIVA'
UNION ALL
SELECT (SELECT id FROM usuario WHERE email = 'vendedor@test.com'),
       (SELECT id FROM categoria WHERE nombre = 'Coleccionables'),
       'Figura de colección edición limitada',
       'Figura de colección sellada, edición limitada numerada.',
       'https://picsum.photos/seed/figura/600/400',
       5000, 500,
       now() - interval '10 minutes', now() + interval '90 seconds', 'ACTIVA'
UNION ALL
SELECT (SELECT id FROM usuario WHERE email = 'vendedor@test.com'),
       (SELECT id FROM categoria WHERE nombre = 'Indumentaria'),
       'Campera de cuero vintage',
       'Campera de cuero genuino, talle M, poco uso.',
       'https://picsum.photos/seed/campera/600/400',
       8000, 500,
       now() + interval '24 hours', now() + interval '25 hours', 'PROGRAMADA'
UNION ALL
SELECT (SELECT id FROM usuario WHERE email = 'vendedor@test.com'),
       (SELECT id FROM categoria WHERE nombre = 'Vehículos'),
       'Bicicleta rodado 29',
       'Bicicleta de montaña rodado 29, frenos a disco.',
       'https://picsum.photos/seed/bici/600/400',
       30000, 2000,
       now() - interval '2 days', now() - interval '1 hour', 'ACTIVA'
UNION ALL
SELECT (SELECT id FROM usuario WHERE email = 'vendedor@test.com'),
       (SELECT id FROM categoria WHERE nombre = 'Tecnología'),
       'Teclado mecánico RGB',
       'Teclado mecánico switches rojos, retroiluminado RGB.',
       'https://picsum.photos/seed/teclado/600/400',
       10000, 1000,
       now() - interval '3 days', now() - interval '2 hours', 'ACTIVA';

-- Pujas: 2 ofertas previas en la subasta "activa estándar" (líder $45.000, comprador1)
INSERT INTO puja (subasta_id, comprador_id, monto, fecha_puja)
SELECT (SELECT id FROM subasta WHERE titulo = 'Notebook Gamer RTX 4070'),
       (SELECT id FROM usuario WHERE email = 'comprador2@test.com'),
       41000, now() - interval '40 minutes'
UNION ALL
SELECT (SELECT id FROM subasta WHERE titulo = 'Notebook Gamer RTX 4070'),
       (SELECT id FROM usuario WHERE email = 'comprador1@test.com'),
       45000, now() - interval '20 minutes';

-- Puja ganadora en la subasta "vencida con ganador" (para probar la liquidación del Worker)
INSERT INTO puja (subasta_id, comprador_id, monto, fecha_puja)
SELECT (SELECT id FROM subasta WHERE titulo = 'Bicicleta rodado 29'),
       (SELECT id FROM usuario WHERE email = 'comprador2@test.com'),
       32000, now() - interval '3 hours';

-- Ledger: depósitos iniciales de cada billetera + retención vigente de comprador1
INSERT INTO transaccion_ledger (billetera_id, tipo, monto, fecha, subasta_id)
SELECT (SELECT b.id FROM billetera b JOIN usuario u ON u.id = b.usuario_id WHERE u.email = 'comprador1@test.com'),
       'DEPOSITO', 150000, now() - interval '5 days', NULL
UNION ALL
SELECT (SELECT b.id FROM billetera b JOIN usuario u ON u.id = b.usuario_id WHERE u.email = 'comprador1@test.com'),
       'RETENCION', 45000, now() - interval '20 minutes',
       (SELECT id FROM subasta WHERE titulo = 'Notebook Gamer RTX 4070')
UNION ALL
SELECT (SELECT b.id FROM billetera b JOIN usuario u ON u.id = b.usuario_id WHERE u.email = 'comprador2@test.com'),
       'DEPOSITO', 200000, now() - interval '5 days', NULL
UNION ALL
SELECT (SELECT b.id FROM billetera b JOIN usuario u ON u.id = b.usuario_id WHERE u.email = 'sinfondos@test.com'),
       'DEPOSITO', 500, now() - interval '5 days', NULL;
