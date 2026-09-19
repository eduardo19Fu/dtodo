export type CategoriaReporte = 'VENTAS' | 'INVENTARIO' | 'PROFORMAS' | 'NOTAS_CREDITO' | 'COMPRAS';
export type FormatoReporte = 'PDF' | 'XLSX';
export type FiltroReporte = 'FECHAS' | 'SUCURSAL' | 'USUARIO' | 'CATEGORIA' | 'CLIENTE' |
  'PRODUCTO' | 'PROVEEDOR' | 'ESTADO' | 'FECHA_CORTE';

export interface ReporteDefinicion {
  codigo: string;
  categoria: CategoriaReporte;
  titulo: string;
  descripcion: string;
  icono: string;
  formatos: FormatoReporte[];
  filtros: FiltroReporte[];
  roles: string[];
  disponible: boolean;
}
