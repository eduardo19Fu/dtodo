import { resumenImportacion } from './movimiento-bodega-request';

describe('resumenImportacion', () => {
  const resultado = (productosCreados: number): any => ({
    filasLeidas: 4, productosImportados: 4, unidadesImportadas: 30, productosCreados, errores: []
  });

  it('informa los productos y unidades importados', () => {
    expect(resumenImportacion(resultado(0))).toBe('Se importaron 4 productos (30 unidades).');
  });

  it('añade cuántos productos nuevos se crearon en el catálogo', () => {
    expect(resumenImportacion(resultado(1))).toBe(
      'Se importaron 4 productos (30 unidades). Se crearon 1 producto nuevo en el catálogo.');
    expect(resumenImportacion(resultado(3))).toBe(
      'Se importaron 4 productos (30 unidades). Se crearon 3 productos nuevos en el catálogo.');
  });

  it('tolera una respuesta sin el conteo de productos creados', () => {
    expect(resumenImportacion({ ...resultado(0), productosCreados: undefined })).toBe(
      'Se importaron 4 productos (30 unidades).');
  });
});
