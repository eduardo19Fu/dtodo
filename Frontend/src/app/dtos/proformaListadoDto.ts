export interface ProformaListadoDto {
  idProforma: number;
  noProforma: string;
  fechaEmision: Date;
  total: number;
  idEstado: number;
  estado: string;
  usuario: string;
  vendedor: string;
  cliente: string;
  nitCliente: string;
}
