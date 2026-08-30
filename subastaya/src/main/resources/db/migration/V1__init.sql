-- Esquema inicial de SubastaYa (ver domain.md para el detalle de cada entidad)

CREATE TABLE categoria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL UNIQUE,
    url_icono  VARCHAR(255)
);

CREATE TABLE usuario (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    nombre          VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    fecha_registro  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE billetera (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id        BIGINT NOT NULL UNIQUE REFERENCES usuario (id),
    saldo_total       NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (saldo_total >= 0),
    saldo_retenido    NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (saldo_retenido >= 0),
    saldo_disponible  NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (saldo_disponible >= 0),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE subasta (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vendedor_id         BIGINT NOT NULL REFERENCES usuario (id),
    categoria_id        BIGINT NOT NULL REFERENCES categoria (id),
    titulo              VARCHAR(200) NOT NULL,
    descripcion         TEXT NOT NULL,
    url_imagen          VARCHAR(500),
    precio_base         NUMERIC(14, 2) NOT NULL CHECK (precio_base > 0),
    incremento_minimo   NUMERIC(14, 2) NOT NULL CHECK (incremento_minimo > 0),
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP NOT NULL,
    estado              VARCHAR(20) NOT NULL
        CHECK (estado IN ('PROGRAMADA', 'ACTIVA', 'FINALIZADA', 'DESIERTA')),
    version             BIGINT NOT NULL DEFAULT 0,
    CHECK (fecha_fin > fecha_inicio)
);

CREATE INDEX idx_subasta_estado ON subasta (estado);
CREATE INDEX idx_subasta_fecha_fin ON subasta (fecha_fin);
CREATE INDEX idx_subasta_categoria ON subasta (categoria_id);

CREATE TABLE puja (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subasta_id    BIGINT NOT NULL REFERENCES subasta (id),
    comprador_id  BIGINT NOT NULL REFERENCES usuario (id),
    monto         NUMERIC(14, 2) NOT NULL CHECK (monto > 0),
    fecha_puja    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_puja_subasta ON puja (subasta_id);
CREATE INDEX idx_puja_comprador ON puja (comprador_id);

CREATE TABLE transaccion_ledger (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    billetera_id  BIGINT NOT NULL REFERENCES billetera (id),
    tipo          VARCHAR(20) NOT NULL
        CHECK (tipo IN ('DEPOSITO', 'RETENCION', 'LIBERACION', 'PAGO', 'COBRO')),
    monto         NUMERIC(14, 2) NOT NULL CHECK (monto > 0),
    fecha         TIMESTAMP NOT NULL DEFAULT now(),
    subasta_id    BIGINT REFERENCES subasta (id)
);

CREATE INDEX idx_ledger_billetera ON transaccion_ledger (billetera_id);

CREATE TABLE auditoria_log (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entidad        VARCHAR(20) NOT NULL
        CHECK (entidad IN ('SUBASTA', 'BILLETERA', 'SISTEMA')),
    entidad_id     BIGINT NOT NULL,
    accion         VARCHAR(50) NOT NULL,
    usuario_id     BIGINT REFERENCES usuario (id),
    detalle_json   TEXT,
    fecha          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_entidad ON auditoria_log (entidad, entidad_id);
