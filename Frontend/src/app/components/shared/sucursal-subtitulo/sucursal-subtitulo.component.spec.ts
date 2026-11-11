import { SucursalSubtituloComponent } from './sucursal-subtitulo.component';

describe('SucursalSubtituloComponent', () => {
  it('expone la sesión para mostrar la sucursal del usuario', () => {
    const auth: any = { usuario: { sucursal: { nombre: 'Sucursal Central' } } };

    const componente = new SucursalSubtituloComponent(auth);

    expect(componente.auth.usuario.sucursal.nombre).toBe('Sucursal Central');
  });
});
