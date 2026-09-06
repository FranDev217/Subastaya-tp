UPDATE billetera b
   SET saldo_total = 0,
       saldo_retenido = 0,
       saldo_disponible = 0
 WHERE b.usuario_id = (SELECT id FROM usuario WHERE email = 'vendedor@test.com');

UPDATE billetera b
   SET saldo_total = 150000,
       saldo_retenido = 45000,
       saldo_disponible = 105000
 WHERE b.usuario_id = (SELECT id FROM usuario WHERE email = 'comprador1@test.com');

UPDATE billetera b
   SET saldo_total = 200000,
       saldo_retenido = 32000,
       saldo_disponible = 168000
 WHERE b.usuario_id = (SELECT id FROM usuario WHERE email = 'comprador2@test.com');

UPDATE billetera b
   SET saldo_total = 500,
       saldo_retenido = 0,
       saldo_disponible = 500
 WHERE b.usuario_id = (SELECT id FROM usuario WHERE email = 'sinfondos@test.com');

UPDATE subasta SET estado = 'ACTIVA' WHERE id IN (1, 2, 4, 5);
UPDATE subasta SET estado = 'PROGRAMADA' WHERE id = 3;

DELETE FROM auditoria_log;
DELETE FROM transaccion_ledger WHERE tipo IN ('PAGO', 'COBRO');
