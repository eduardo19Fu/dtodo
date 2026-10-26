import { ProformaDto } from '../../dtos/proforma-dto';
import { ProformasComponent } from './proformas.component';

describe('ProformasComponent - estado de facturación', () => {
  const component = new ProformasComponent(null, null);

  function proforma(valores: Partial<ProformaDto>): ProformaDto {
    return Object.assign(new ProformaDto(), valores);
  }

  it('no muestra referencia cuando la proforma no tiene factura', () => {
    expect(component.referenciaFactura(proforma({}))).toBe('');
  });

  it('muestra serie, número y quién facturó', () => {
    const dto = proforma({ idFactura: 10, serieFactura: 'A', noFactura: 55, facturadoPor: 'Luis Perez' });

    expect(component.referenciaFactura(dto)).toBe('A-55 · Luis Perez');
  });

  it('omite la serie y el emisor cuando no vienen', () => {
    expect(component.referenciaFactura(proforma({ idFactura: 10, noFactura: 7 }))).toBe('7');
  });
});
