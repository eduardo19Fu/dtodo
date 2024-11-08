import { UsuarioAuxiliar } from "./auxiliar/usuario-auxiliar";
import { Cliente } from "./cliente";
import { Estado } from "./estado";
import { NotaCreditoDetalle } from "./nota-credito-detalle";

export class NotaCredito {
    idNotaCredito: number;
    correlativoFacturaSat: String;
    serieFacturaSat: String;
    total: number;
    fechaCreacion: Date;
    fechaEntregaEstimada: Date;
    estado: String;
    observaciones: String;

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

    calcularSaldoRestante(abono: number, totalEnvio: number): number {
        let saldoRestante = 0;
        saldoRestante = totalEnvio - abono;
        return saldoRestante;
    }
}
