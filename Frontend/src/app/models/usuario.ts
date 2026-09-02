import { Correlativo } from './correlativo';
import { Sucursal } from './sucursal';
export class Usuario {

    idUsuario: number;
    usuario: string;
    password: string;
    primerNombre: string;
    segundoNombre: string;
    apellido: string;
    enabled: boolean;
    fechaRegistro: Date;
    roles: string[] = [];

    correlativos: Correlativo[] = [];
    sucursal: Sucursal;
}
