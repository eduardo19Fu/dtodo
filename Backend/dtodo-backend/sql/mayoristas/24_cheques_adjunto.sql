-- Módulo de Clientes Mayoristas: foto o PDF del cheque.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   cheques_recibo.adjunto_nombre   Nombre original del archivo que se adjuntó (para mostrarlo y descargarlo).
--   cheques_recibo.adjunto_archivo  Nombre con que se guardó en el servidor (uploads/cheques/); nunca viene del usuario.
--   cheques_recibo.adjunto_tipo     Tipo del archivo: application/pdf, image/png o image/jpeg.
ALTER TABLE cheques_recibo
    ADD COLUMN adjunto_nombre VARCHAR(200) NULL,
    ADD COLUMN adjunto_archivo VARCHAR(80) NULL,
    ADD COLUMN adjunto_tipo VARCHAR(60) NULL;
