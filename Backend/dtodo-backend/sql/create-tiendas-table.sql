CREATE TABLE tiendas(
    id_tienda VARCHAR(150) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(100) NOT NULL,
    telefono VARCHAR(15),
    encargado VARCHAR(150)
);