import { UsuarioAuxiliar } from "./auxiliar/usuario-auxiliar";
import { Cliente } from "./cliente";
import { NotaCreditoDetalle } from "./nota-credito-detalle";

export type TipoDocumentoOrigen = 'FACTURA' | 'PROFORMA';

export class NotaCredito {
    idNotaCredito: number;
    correlativoFacturaSat: string;
    serieFacturaSat: string;
    tipoDocumentoOrigen: TipoDocumentoOrigen = 'FACTURA';
    noProforma: string;
    total: number;
    fechaCreacion: Date;
    fechaEntregaEstimada: Date;
    estado: string;
    observaciones: string;

    usuario: UsuarioAuxiliar;
    cliente: Cliente;
    items: NotaCreditoDetalle[] = [];

    calcularTotal(): number {
        let total = 0;
        this.items.forEach((item: NotaCreditoDetalle) => {
            total += item.calcularImporteDescuento();
        });

        return total;
    }
}
