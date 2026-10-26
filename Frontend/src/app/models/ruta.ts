import { Estado } from './estado';

/** Ruta tal como se envía al guardar: las referencias solo necesitan su id. */
export class Ruta {
  idRuta: number;
  nombre: string;
  zona: string;
  diasVisita: string;
  vendedor: { idUsuario: number } | null = null;
  sucursal: { idSucursal: number } | null = null;
  estado: Estado | null = null;
}
