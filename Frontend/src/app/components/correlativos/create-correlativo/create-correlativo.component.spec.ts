import { of } from 'rxjs';
import swal from 'sweetalert2';

import { Correlativo } from '../../../models/correlativo';
import { CreateCorrelativoComponent } from './create-correlativo.component';

describe('CreateCorrelativoComponent', () => {
  let component: CreateCorrelativoComponent;
  let correlativoService: jasmine.SpyObj<any>;
  let router: jasmine.SpyObj<any>;

  beforeEach(() => {
    correlativoService = jasmine.createSpyObj('CorrelativoService', ['create', 'update', 'getCorrelativo']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    component = new CreateCorrelativoComponent(
      correlativoService,
      jasmine.createSpyObj('UsuarioService', ['getCajeros']),
      router,
      { params: of({}) } as any
    );
  });

  it('muestra el identificador devuelto al crear un correlativo', () => {
    const creado = new Correlativo();
    creado.idCorrelativo = 49;
    correlativoService.create.and.returnValue(of(creado));
    const alert = spyOn(swal, 'fire');

    component.create();

    expect(alert).toHaveBeenCalledWith(
      'Correlativo Creado',
      'El correlativo #49 fue creado correctamente.',
      'success'
    );
    expect(router.navigate).toHaveBeenCalledWith(['/facturas/correlativos/index']);
  });

  it('usa directamente el correlativo devuelto al actualizar', () => {
    const actualizado = new Correlativo();
    actualizado.idCorrelativo = 17;
    correlativoService.update.and.returnValue(of(actualizado));
    const alert = spyOn(swal, 'fire');

    component.update();

    expect(alert).toHaveBeenCalledWith(
      'Correlativo Actualizado',
      'El correlativo #17 fue actualizado correctamente.',
      'success'
    );
  });
});
