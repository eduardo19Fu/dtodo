export interface ReporteFiltroDto {
  fechaInicio?: string;
  fechaFin?: string;
  idSucursal?: number;
  idUsuario?: number;
  idProveedor?: number;
  idCategoria?: number;
  idCliente?: number;
  estado?: string;
  formato?: 'PDF' | 'XLSX';
}
