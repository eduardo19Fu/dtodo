export interface CorrelativoDto {
  idCorrelativo: number;
  correlativoInicial: number;
  correlativoFinal: number;
  correlativoActual: number;
  serie: string;
  fechaCreacion: Date;
  usuario: string;
  idEstado: number;
  estado: string;
}
