export interface FacturaListadoDto {
  idFactura: number;
  noFactura: number;
  serie: string;
  fecha: Date;
  total: number;
  idEstado: number;
  estado: string;
  usuario: string;
  vendedor: string;
  cliente: string;
  nitCliente: string;
  certificacionSat: string;
}
