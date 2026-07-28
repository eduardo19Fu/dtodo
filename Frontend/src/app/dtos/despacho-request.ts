import { DespachoNota } from '../models/despacho-nota';

/**
 * Payload del registro de un despacho de Nota de Crédito.
 *
 * Encapsula las líneas a despachar junto con la contraseña del usuario en
 * sesión, que el backend valida contra el hash almacenado antes de persistir.
 * La contraseña sólo existe dentro de este objeto durante la petición: no se
 * guarda en el componente ni en sessionStorage.
 */
export class DespachoRequest {
    password: string;
    despachos: DespachoNota[];

    constructor(password: string, despachos: DespachoNota[]) {
        this.password = password;
        this.despachos = despachos;
    }
}
