-- Módulo de Bodegas: catálogo de bodegas.
-- id_sucursal es NULL a propósito: una bodega puede existir sin sucursal asignada; cuando la tiene,
-- es la sucursal destino por defecto de sus despachos.
CREATE TABLE bodegas (
    id_bodega INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(200) NOT NULL,
    descripcion VARCHAR(300),
    encargado VARCHAR(150),
    telefono VARCHAR(15),
    fecha_registro DATETIME NOT NULL,
    id_sucursal INT NULL,
    id_estado INT NOT NULL,
    id_usuario INT NULL,
    CONSTRAINT uq_bodega_nombre UNIQUE (nombre),
    CONSTRAINT fk_bodega_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_bodega_estado FOREIGN KEY (id_estado) REFERENCES estados(id_estado),
    CONSTRAINT fk_bodega_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_bodega_sucursal ON bodegas(id_sucursal);
