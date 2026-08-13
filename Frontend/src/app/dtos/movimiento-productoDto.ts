export class MovimientoProductoDto {
    idMovimiento: number;
    fechaMovimiento: Date;
    stockInicial: number;
    tipoMovimiento: string;
    cantidad: number;
    productoNombre: string;
    usuarioNombre: string;
}