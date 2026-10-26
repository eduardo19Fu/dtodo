import { CategoriaCliente, TipoCliente } from '../models/cliente';

/** Fila del listado de clientes. A diferencia del modelo completo, aquí la ruta llega como texto. */
export class ClienteDto {
  idCliente: number;
  nombre: string;
  nit: string;
  direccion: string;
  fechaRegistro: Date;
  telefono: string;
  codigoCliente: string | null;
  nombreNegocio: string | null;
  tipoCliente: TipoCliente;
  categoriaDescuento: CategoriaCliente | null;
  idRuta: number | null;
  ruta: string | null;
  contacto: string | null;
  telefonoContacto: string | null;
  referenciaUbicacion: string | null;
  limiteCredito: number | null;
  diasCredito: number | null;
}
