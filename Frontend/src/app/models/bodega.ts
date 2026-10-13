import { Estado } from './estado';
import { Sucursal } from './sucursal';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

export class Bodega {
    idBodega: number;
    nombre: string;
    ubicacion: string;
    descripcion: string;
    encargado: string;
    telefono: string;
    fechaRegistro: Date;

    sucursal: Sucursal;
    estado: Estado;
    usuario: UsuarioAuxiliar;
}
