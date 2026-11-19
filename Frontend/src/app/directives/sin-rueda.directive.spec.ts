import { ElementRef } from '@angular/core';

import { SinRuedaDirective } from './sin-rueda.directive';

describe('SinRuedaDirective', () => {
  let campo: HTMLInputElement;
  let directiva: SinRuedaDirective;

  beforeEach(() => {
    campo = document.createElement('input');
    campo.type = 'number';
    document.body.appendChild(campo);
    directiva = new SinRuedaDirective(new ElementRef(campo));
  });

  afterEach(() => campo.remove());

  it('al girar la rueda con el campo enfocado le quita el foco para que no cambie el valor', () => {
    campo.focus();
    expect(document.activeElement).toBe(campo);

    directiva.alUsarLaRueda();

    expect(document.activeElement).not.toBe(campo);
  });

  it('si el campo no está enfocado no hace nada', () => {
    const otro = document.createElement('input');
    document.body.appendChild(otro);
    otro.focus();

    directiva.alUsarLaRueda();

    expect(document.activeElement).toBe(otro);
    otro.remove();
  });
});
