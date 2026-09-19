export interface ReporteFiltroDto {
  fechaInicio?: string;
  fechaFin?: string;
  idSucursal?: number;
  idUsuario?: number;
  formato?: 'PDF' | 'XLSX';
}
