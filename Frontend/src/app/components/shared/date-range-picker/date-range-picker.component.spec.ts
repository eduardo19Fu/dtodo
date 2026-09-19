import { ElementRef } from '@angular/core';

import { DateRangePickerComponent } from './date-range-picker.component';

describe('DateRangePickerComponent', () => {
  let component: DateRangePickerComponent;

  beforeEach(() => {
    component = new DateRangePickerComponent(new ElementRef(document.createElement('div')));
  });

  it('emite y cierra el selector despues de elegir cada fecha', () => {
    spyOn(component.fechaInicioChange, 'emit');
    component.calendarioAbierto = true;

    component.seleccionarFecha({ iso: '2026-09-10', fecha: new Date(2026, 8, 10) } as any);

    expect(component.fechaInicioChange.emit).toHaveBeenCalledWith('2026-09-10');
    expect(component.calendarioAbierto).toBeFalse();
  });

  it('formatea las fechas para mostrarlas al usuario', () => {
    expect(component.formatearFechaVisible('2026-09-18')).toBe('18/09/2026');
    expect(component.formatearFechaVisible(null)).toBe('Seleccionar fecha');
  });
});
