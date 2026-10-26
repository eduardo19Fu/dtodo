-- Módulo de Clientes Mayoristas: datos del cliente mayorista.
-- Aditivo: los clientes existentes quedan como MINORISTA, sin ruta, sin categoría y sin código.
-- codigo_cliente es único cuando existe (MySQL admite varios NULL en un índice único); por defecto el
-- sistema propone el teléfono, que es con lo que el cliente identifica a sus clientes.
-- El NIT sigue en la misma columna y es opcional hasta que se factura.
ALTER TABLE clientes
    ADD COLUMN codigo_cliente VARCHAR(30) NULL,
    ADD COLUMN nombre_negocio VARCHAR(150) NULL,
    ADD COLUMN tipo_cliente VARCHAR(20) NOT NULL DEFAULT 'MINORISTA',
    ADD COLUMN categoria_descuento VARCHAR(20) NULL,
    ADD COLUMN id_ruta INT NULL,
    ADD COLUMN contacto VARCHAR(150) NULL,
    ADD COLUMN telefono_contacto VARCHAR(30) NULL,
    ADD COLUMN referencia_ubicacion VARCHAR(255) NULL,
    ADD COLUMN limite_credito DECIMAL(12,2) NULL,
    ADD COLUMN dias_credito INT NULL;

ALTER TABLE clientes
    ADD CONSTRAINT uq_cliente_codigo UNIQUE (codigo_cliente),
    ADD CONSTRAINT fk_cliente_ruta FOREIGN KEY (id_ruta) REFERENCES rutas(id_ruta);

CREATE INDEX idx_cliente_ruta ON clientes(id_ruta);
CREATE INDEX idx_cliente_tipo ON clientes(tipo_cliente);
