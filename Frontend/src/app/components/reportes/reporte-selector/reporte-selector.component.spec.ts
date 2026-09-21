import { ElementRef } from '@angular/core';

import { ReporteSelectorComponent } from './reporte-selector.component';

describe('ReporteSelectorComponent', () => {
  it('filtra opciones por etiqueta o detalle sin distinguir acentos', () => {
    const component = new ReporteSelectorComponent(new ElementRef(document.createElement('div')));
    component.opciones = [
      { valor: 1, etiqueta: 'José Pérez', detalle: 'NIT 5487-9' },
      { valor: 2, etiqueta: 'Comercial Norte', detalle: 'Teléfono 5555-2000' }
    ];

    component.busqueda = 'jose';
    expect(component.opcionesVisibles.map(opcion => opcion.valor)).toEqual([1]);
    component.busqueda = '5555-2000';
    expect(component.opcionesVisibles.map(opcion => opcion.valor)).toEqual([2]);
  });

  it('emite el valor seleccionado y cierra el listado', () => {
    const component = new ReporteSelectorComponent(new ElementRef(document.createElement('div')));
    let valor: any;
    component.valorChange.subscribe(seleccion => valor = seleccion);
    component.abierto = true;

    component.seleccionar({ valor: 7, etiqueta: 'Sucursal Central' });

    expect(valor).toBe(7);
    expect(component.abierto).toBeFalse();
  });
});
