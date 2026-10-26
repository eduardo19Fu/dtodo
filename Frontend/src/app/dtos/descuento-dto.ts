import { CategoriaCliente } from '../models/cliente';

/** De dónde sale el descuento de una línea de proforma; lo elige el usuario y el servidor lo verifica. */
export type TipoDescuento = 'CATEGORIA' | 'CIENTO' | 'FARDO' | 'PRECIO_MAYORISTA' | 'MANUAL';

export const ETIQUETAS_TIPO_DESCUENTO: Record<TipoDescuento, string> = {
  CATEGORIA: 'Categoría',
  CIENTO: 'Por ciento',
  FARDO: 'Fardo cerrado',
  PRECIO_MAYORISTA: 'Precio mayorista',
  MANUAL: 'Manual'
};

/** Una forma de descuento disponible para un producto, un cliente y una cantidad. */
export class OpcionDescuento {
  tipo: TipoDescuento;
  etiqueta: string;
  /** Nulo cuando hay varios niveles que elegir o cuando el descuento es libre. */
  porcentaje: number | null;
  niveles: number[];
  cantidadMinima: number | null;
  /** Falso cuando la cantidad no llega al mínimo: se avisa, pero no se bloquea. */
  alcanzaMinimo: boolean;
  precio: number | null;
}

export class OpcionesDescuento {
  idProducto: number;
  precioVenta: number;
  precioMayorista: number | null;
  categoria: CategoriaCliente | null;
  cantidad: number;
  opciones: OpcionDescuento[];
  sugerida: OpcionDescuento | null;
}

export class CategoriaNiveles {
  categoria: CategoriaCliente;
  porcentajes: number[];
}

/** Descuento especial de un producto por ciento o por fardo cerrado. */
export class DescuentoProducto {
  tipo: 'CIENTO' | 'FARDO';
  cantidadMinima: number;
  porcentaje: number;
  activo = true;
}

export class ProductoDescuentos {
  idProducto: number;
  codProducto: string;
  nombre: string;
  precioVenta: number;
  precioMayorista: number | null;
  descuentos: DescuentoProducto[];
}

export class GuardarDescuentosProducto {
  precioMayorista: number | null;
  descuentos: DescuentoProducto[];
}
