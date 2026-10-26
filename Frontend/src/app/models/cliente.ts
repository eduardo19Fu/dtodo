export type TipoCliente = 'MINORISTA' | 'MAYORISTA';
export type CategoriaCliente = 'A' | 'B' | 'VOLUMEN';

export class Cliente {

    idCliente: number;
    nombre: string;
    nit: string;
    direccion: string;
    fechaRegistro: Date;
    telefono: string;

    /** Código con el que el negocio identifica al cliente (por defecto su teléfono). */
    codigoCliente?: string;
    nombreNegocio?: string;
    tipoCliente: TipoCliente = 'MINORISTA';
    categoriaDescuento?: CategoriaCliente | null;
    /** Ruta del cliente mayorista; al guardar basta con el id. */
    ruta?: { idRuta: number; nombre?: string } | null;
    contacto?: string;
    telefonoContacto?: string;
    referenciaUbicacion?: string;
    limiteCredito?: number | null;
    diasCredito?: number | null;
}
