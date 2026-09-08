-- Corrección del seed para el Worker de liquidación (2.3).
--
-- La subasta "Bicicleta rodado 29" (vencida con ganador) tiene una puja de
-- $32.000 de comprador2, pero la billetera de comprador2 no reflejaba la
-- retención de escrow asociada. Sin saldo retenido, el Worker no puede debitar
-- al comprador ganador y la liquidación rompe el CHECK (saldo_retenido >= 0).
--
-- Se corrige con una migración nueva en lugar de editar V2 porque las
-- migraciones ya aplicadas son inmutables: modificar V2 dispara
-- "checksum mismatch" en las bases que ya corrieron el seed.
--
-- Idempotente: la guarda NOT EXISTS permite ejecutarla sobre bases nuevas
-- (donde V2 deja saldo_retenido = 0) y sobre bases ya migradas, sin duplicar
-- la retención ni el asiento del Ledger.

UPDATE billetera b
   SET saldo_retenido = b.saldo_retenido + 32000,
       saldo_disponible = b.saldo_disponible - 32000
 WHERE b.usuario_id = (SELECT id FROM usuario WHERE email = 'comprador2@test.com')
   AND NOT EXISTS (
       SELECT 1
         FROM transaccion_ledger l
        WHERE l.billetera_id = b.id
          AND l.tipo = 'RETENCION'
          AND l.subasta_id = (SELECT id FROM subasta WHERE titulo = 'Bicicleta rodado 29')
   );

INSERT INTO transaccion_ledger (billetera_id, tipo, monto, fecha, subasta_id)
SELECT b.id, 'RETENCION', 32000, now() - interval '3 hours',
       (SELECT id FROM subasta WHERE titulo = 'Bicicleta rodado 29')
  FROM billetera b
  JOIN usuario u ON u.id = b.usuario_id
 WHERE u.email = 'comprador2@test.com'
   AND NOT EXISTS (
       SELECT 1
         FROM transaccion_ledger l
        WHERE l.billetera_id = b.id
          AND l.tipo = 'RETENCION'
          AND l.subasta_id = (SELECT id FROM subasta WHERE titulo = 'Bicicleta rodado 29')
   );
