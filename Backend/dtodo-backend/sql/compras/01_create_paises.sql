CREATE TABLE paises (
    id_pais INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    fecha_registro DATETIME NOT NULL,
    id_usuario INT NULL,
    CONSTRAINT fk_pais_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
