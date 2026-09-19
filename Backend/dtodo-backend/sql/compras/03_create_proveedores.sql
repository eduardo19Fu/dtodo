CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(150),
    telefono_entidad VARCHAR(15),
    telefono_contacto VARCHAR(15),
    email_entidad VARCHAR(150),
    email_contacto VARCHAR(150),
    direccion VARCHAR(200),
    sitio_web VARCHAR(200),
    id_pais INT NULL,
    id_estado INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    id_usuario INT NULL,
    CONSTRAINT fk_proveedor_pais FOREIGN KEY (id_pais) REFERENCES paises(id_pais),
    CONSTRAINT fk_proveedor_estado FOREIGN KEY (id_estado) REFERENCES estados(id_estado),
    CONSTRAINT fk_proveedor_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_proveedor_nombre ON proveedores(nombre);
