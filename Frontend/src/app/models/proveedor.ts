import { Estado } from './estado';
import { Pais } from './pais';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

export class Proveedor {
    idProveedor: number;
    nombre: string;
    contacto: string;
    telefonoEntidad: string;
    telefonoContacto: string;
    emailEntidad: string;
    emailContacto: string;
    direccion: string;
    sitioWeb: string;
    fechaRegistro: Date;

    pais: Pais;
    estado: Estado;
    usuario: UsuarioAuxiliar;
}
