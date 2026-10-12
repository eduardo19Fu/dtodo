/** Datos para agregar producto a una bodega o reducir sus existencias. */
export class MovimientoBodegaRequest {
  idProducto: number;
  cantidad: number;
  stockMinimo?: number;
  motivo?: string;
}

export type OrigenInventarioBodega = 'SUCURSAL' | 'BODEGA';

/** Resultado de importar inventario desde Excel; si hay errores no se aplicó ninguna fila. */
export class ImportacionInventarioDto {
  filasLeidas: number;
  productosImportados: number;
  /** Productos que no existían en el catálogo y se crearon durante la importación. */
  productosCreados: number;
  unidadesImportadas: number;
  errores: string[] = [];
}

/** Texto con el resultado de una importación exitosa, incluidos los productos nuevos creados en el catálogo. */
export function resumenImportacion(resultado: ImportacionInventarioDto): string {
  const creados = resultado.productosCreados || 0;
  const nuevos = creados > 0 ? ` Se crearon ${creados} ${creados === 1 ? 'producto nuevo' : 'productos nuevos'} en el catálogo.` : '';
  return `Se importaron ${resultado.productosImportados} productos (${resultado.unidadesImportadas} unidades).${nuevos}`;
}
