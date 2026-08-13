import { TipoDocumentoOrigen } from '../models/nota-credito';

export class NotaCreditoListDto {
    idNotaCredito: number;
    total: number;
    usuario: string;
    cliente: string;
    nitCliente: string;
    correlativoFacturaSat: string;
    serieFacturaSat: string;
    tipoDocumentoOrigen: TipoDocumentoOrigen;
    noProforma: string;
    fechaCreacion: Date;
    fechaEntregaEstimada: Date;
    estado: string;
}
