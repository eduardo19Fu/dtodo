export type TipoMovimientoBodega =
  'INGRESO' | 'IMPORTACION' | 'ANULACION_DESPACHO' | 'REDUCCION' | 'ELIMINACION' | 'DESPACHO';

export interface TipoMovimientoBodegaInfo {
  codigo: TipoMovimientoBodega;
  etiqueta: string;
  icono: string;
  /** true si el movimiento suma existencias a la bodega. */
  entrada: boolean;
}

export const TIPOS_MOVIMIENTO_BODEGA: TipoMovimientoBodegaInfo[] = [
  { codigo: 'INGRESO', etiqueta: 'Ingreso', icono: 'fa-plus-circle', entrada: true },
  { codigo: 'IMPORTACION', etiqueta: 'Importación', icono: 'fa-file-import', entrada: true },
  { codigo: 'ANULACION_DESPACHO', etiqueta: 'Cancelación de despacho', icono: 'fa-undo', entrada: true },
  { codigo: 'REDUCCION', etiqueta: 'Reducción', icono: 'fa-minus-circle', entrada: false },
  { codigo: 'ELIMINACION', etiqueta: 'Eliminación', icono: 'fa-trash-alt', entrada: false },
  { codigo: 'DESPACHO', etiqueta: 'Despacho', icono: 'fa-truck-loading', entrada: false }
];

export class MovimientoBodegaDto {
  idMovimiento: number;
  fechaMovimiento: Date;
  tipoMovimiento: TipoMovimientoBodega;
  cantidad: number;
  stockInicial: number;
  stockFinal: number;
  motivo: string;
  tipoDocumentoOrigen: string;
  idDocumentoOrigen: number;
  codProducto: string;
  producto: string;
  usuario: string;
}
