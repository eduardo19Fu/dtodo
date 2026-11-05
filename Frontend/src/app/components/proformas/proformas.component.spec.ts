import { ProformaDto } from '../../dtos/proforma-dto';
import Swal from 'sweetalert2';
import { of, Subject, throwError } from 'rxjs';
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

describe('ProformasComponent - reemisión tras anular la factura', () => {
  const component = new ProformasComponent(null, null);

  function proforma(valores: Partial<ProformaDto>): ProformaDto {
    return Object.assign(new ProformaDto(), valores);
  }

  it('sin factura o con la factura anulada se puede facturar', () => {
    expect(component.facturada(proforma({}))).toBeFalse();
    expect(component.facturada(proforma({ idFactura: 4, estadoFactura: 'ANULADO' }))).toBeFalse();
  });

  it('con una factura vigente ya está facturada', () => {
    expect(component.facturada(proforma({ idFactura: 4, estadoFactura: 'PAGADO' }))).toBeTrue();
  });
});

describe('ProformasComponent - impresión de la proforma mayorista', () => {
  let proformaService: any;
  let component: ProformasComponent;

  function proforma(tipo: 'MINORISTA' | 'MAYORISTA'): ProformaDto {
    return Object.assign(new ProformaDto(), { idProforma: 4, tipoProforma: tipo });
  }

  beforeEach(() => {
    proformaService = { getProformaPdf: jasmine.createSpy('getProformaPdf').and.returnValue(of({ data: new Blob(['x']) })) };
    component = new ProformasComponent(proformaService, null);
    spyOn(window, 'open');
  });

  it('una proforma minorista se imprime directo, sin preguntar la copia', () => {
    const fire = spyOn(Swal, 'fire');

    component.imprimir(proforma('MINORISTA'));

    expect(fire).not.toHaveBeenCalled();
    expect(proformaService.getProformaPdf).toHaveBeenCalledWith(4, 'CLIENTE');
  });

  it('una mayorista pregunta la copia y pide la elegida', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false, isDenied: true } as any));

    component.imprimir(proforma('MAYORISTA'));
    await Promise.resolve();
    await Promise.resolve();

    expect(proformaService.getProformaPdf).toHaveBeenCalledWith(4, 'TIENDA');
  });

  it('si se cancela la elección no se genera ningún PDF', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false, isDenied: false } as any));

    component.imprimir(proforma('MAYORISTA'));
    await Promise.resolve();
    await Promise.resolve();

    expect(proformaService.getProformaPdf).not.toHaveBeenCalled();
  });

  it('mientras se genera el PDF marca la proforma como imprimiendo y lo libera al terminar', () => {
    const respuesta = new Subject<any>();
    proformaService.getProformaPdf.and.returnValue(respuesta);

    component.imprimir(proforma('MINORISTA'));
    expect(component.imprimiendoId).toBe(4);

    component.imprimir(proforma('MINORISTA'));
    expect(proformaService.getProformaPdf).toHaveBeenCalledTimes(1);

    respuesta.next({ data: new Blob(['x']) });
    expect(component.imprimiendoId).toBeNull();
  });

  it('si falla la generación libera el botón y avisa del error', () => {
    spyOn(Swal, 'fire');
    proformaService.getProformaPdf.and.returnValue(throwError({ error: { message: 'boom' } }));

    component.imprimir(proforma('MINORISTA'));

    expect(component.imprimiendoId).toBeNull();
    expect(Swal.fire).toHaveBeenCalled();
  });
});
