/** Fila del listado de rutas: la ruta con su vendedor, sucursal, estado y cantidad de clientes. */
export class RutaDto {
  idRuta: number;
  nombre: string;
  zona: string;
  diasVisita: string;
  idVendedor: number | null;
  vendedor: string;
  idSucursal: number;
  sucursal: string;
  idEstado: number;
  estado: string;
  totalClientes: number;
}
