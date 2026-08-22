import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';
import { Cliente } from './cliente';
import { Estado } from './estado';
import { TipoFactura } from './tipo-factura';
import { DetalleProforma } from './detalle-proforma';

export class Proforma {

    idProforma: number;
    noProforma: string;
    total = 0;
    fechaEmision: Date;

    estado: Estado;
    usuario: UsuarioAuxiliar;
    cliente: Cliente;
    tipoFactura: TipoFactura;
    itemsProforma: DetalleProforma[] = [];

    calcularTotal(): number{
        this.total = 0;
        this.itemsProforma.forEach((item: DetalleProforma) => {
            this.total += item.calcularImporteDescuento();
        });

        return this.total;
    }

    generarNoProforma(): string {
        const min = 0;
        const max = 10000000000;
        const noProforma = Math.floor(Math.random() * (max - min + 1) + min) + 'P';
        return noProforma;
    }
}
