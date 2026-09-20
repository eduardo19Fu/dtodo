export interface ReporteFiltroDto {
  fechaInicio?: string;
  fechaFin?: string;
  idSucursal?: number;
  idUsuario?: number;
  estado?: string;
  formato?: 'PDF' | 'XLSX';
}
