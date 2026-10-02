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
  unidadesImportadas: number;
  errores: string[] = [];
}
