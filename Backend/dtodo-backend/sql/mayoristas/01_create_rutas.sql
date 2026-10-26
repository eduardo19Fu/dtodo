-- Módulo de Clientes Mayoristas: catálogo de rutas de venta.
-- id_vendedor es NULL a propósito: una ruta puede existir sin vendedor asignado (Ruta 3 mientras el cliente
-- define quién la atiende). Un vendedor puede tener varias rutas; cada ruta tiene un solo vendedor.
-- dias_visita es informativo (texto libre, ej. 'Lunes cada 15 días').
CREATE TABLE rutas (
    id_ruta INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    zona VARCHAR(255),
    dias_visita VARCHAR(100),
    fecha_registro DATETIME NOT NULL,
    id_vendedor INT NULL,
    id_sucursal INT NOT NULL,
    id_estado INT NOT NULL,
    CONSTRAINT uq_ruta_nombre_sucursal UNIQUE (nombre, id_sucursal),
    CONSTRAINT fk_ruta_vendedor FOREIGN KEY (id_vendedor) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_ruta_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_ruta_estado FOREIGN KEY (id_estado) REFERENCES estados(id_estado)
);

CREATE INDEX idx_ruta_vendedor ON rutas(id_vendedor);
