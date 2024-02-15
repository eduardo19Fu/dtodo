import { UsuarioAuxiliar } from "./auxiliar/usuario-auxiliar";
import { Cliente } from "./cliente";
import { Estado } from "./estado";
import { NotaCreditoDetalle } from "./nota-credito-detalle";

export class NotaCredito {
    idNotaCredito: number;
    abono: number;
    total: number;
    restante: number;
    fechaCreacion: Date;
    fechaPagoLimite: Date;

    usuario: UsuarioAuxiliar;
    estado: Estado;
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
