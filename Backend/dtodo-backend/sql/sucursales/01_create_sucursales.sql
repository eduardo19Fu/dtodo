CREATE TABLE sucursales (
    id_sucursal INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(15),
    encargado VARCHAR(150),
    codigo_establecimiento_sat INT NOT NULL DEFAULT 1,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_registro DATETIME NOT NULL,
    id_estado INT NOT NULL,
    id_usuario INT,
    CONSTRAINT fk_sucursal_estado FOREIGN KEY (id_estado) REFERENCES estados(id_estado),
    CONSTRAINT fk_sucursal_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
