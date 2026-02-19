export interface ProductoDto {
    idProducto: number;
    coProducto: string;
    nombre: string;
    precioCompra: number;
    precioVenta: number;
    porcentajeGanancia: number;
    descripcion: number;
    fechaVencimiento: Date;
    fechaRegistro: Date;
    fechaIngreso: Date;
    stock: number;
    marcaProducto: string;
    tipoProducto: string;
    imagen: string;
    estado: string;
}