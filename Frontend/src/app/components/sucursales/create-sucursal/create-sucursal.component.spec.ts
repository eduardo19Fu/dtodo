import { CreateSucursalComponent } from './create-sucursal.component';
import { Sucursal } from 'src/app/models/sucursal';

describe('CreateSucursalComponent - esNueva', () => {
  let component: CreateSucursalComponent;

  beforeEach(() => {
    component = new CreateSucursalComponent(null, null, null, null, null);
  });

  it('es nueva cuando la sucursal aun no tiene id', () => {
    component.sucursal = new Sucursal();

    expect(component.esNueva).toBeTrue();
  });

  it('deja de ser nueva una vez que la sucursal tiene id (edicion)', () => {
    component.sucursal = new Sucursal();
    component.sucursal.idSucursal = 3;

    expect(component.esNueva).toBeFalse();
  });
});
