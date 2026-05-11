import { Producto } from './producto';
import { Usuario } from './usuario';

export class DespachoNota {
    idDespacho: number;
    idEvento: string;
    fechaDespacho: Date;
    codProducto: string;
    cantidad: number;
    totalDespacho: number;

    producto: Producto;
    usuario: Usuario;
}
