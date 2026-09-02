import { Estado } from './estado';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

export class Sucursal {
    idSucursal: number;
    nombre: string;
    direccion: string;
    telefono: string;
    encargado: string;
    codigoEstablecimientoSat: number;
    esPrincipal: boolean;
    fechaRegistro: Date;

    estado: Estado;
    usuario: UsuarioAuxiliar;
}
