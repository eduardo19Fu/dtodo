import { Directive, ElementRef, HostListener } from '@angular/core';

/**
 * Evita que la rueda del ratón cambie el valor de un campo numérico: con el campo enfocado y la página desplazándose,
 * la rueda sube o baja el valor un paso (con paso de 0.01, 25000 pasa a 24999.99 sin que nadie se dé cuenta).
 * Se aplica a todos los campos numéricos del sistema. Las flechas del teclado siguen funcionando.
 */
@Directive({
  selector: 'input[type=number]'
})
export class SinRuedaDirective {

  constructor(private elemento: ElementRef<HTMLInputElement>) { }

  @HostListener('wheel')
  alUsarLaRueda(): void {
    if (document.activeElement === this.elemento.nativeElement) {
      this.elemento.nativeElement.blur();
    }
  }
}
