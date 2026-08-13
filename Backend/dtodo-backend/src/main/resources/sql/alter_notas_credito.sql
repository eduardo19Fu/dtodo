-- ALTER TABLE para que notas_credito coincida con la entidad NotaCredito.java
-- La entidad usa @Enumerated(EnumType.STRING) para estado, por lo que
-- se debe reemplazar la FK id_estado por una columna VARCHAR estado.

-- Paso 1: Eliminar la FK de id_estado (si existe)
ALTER TABLE notas_credito DROP FOREIGN KEY IF EXISTS fk_nota_credito_estado;

-- Paso 2: Eliminar la columna id_estado
ALTER TABLE notas_credito DROP COLUMN IF EXISTS id_estado;

-- Paso 3: Agregar la columna estado como VARCHAR para almacenar el enum
ALTER TABLE notas_credito ADD COLUMN estado VARCHAR(50) NOT NULL DEFAULT 'ENTREGA_PENDIENTE';
