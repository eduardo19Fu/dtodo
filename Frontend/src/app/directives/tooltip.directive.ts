import { Directive, ElementRef, HostListener, Input, OnDestroy, Renderer2 } from '@angular/core';

@Directive({
  selector: '[appTooltip]'
})
export class TooltipDirective implements OnDestroy {

  @Input() appTooltip = '';

  private tooltipElement: HTMLElement;

  constructor(private elementRef: ElementRef, private renderer: Renderer2) { }

  @HostListener('mouseenter')
  @HostListener('focusin')
  show(): void {
    if (!this.appTooltip || this.tooltipElement || this.elementRef.nativeElement.disabled) {
      return;
    }

    this.tooltipElement = this.renderer.createElement('span');
    this.renderer.addClass(this.tooltipElement, 'app-tooltip');
    this.renderer.setAttribute(this.tooltipElement, 'role', 'tooltip');
    this.renderer.appendChild(this.tooltipElement, this.renderer.createText(this.appTooltip));
    this.renderer.appendChild(document.body, this.tooltipElement);

    const trigger = this.elementRef.nativeElement.getBoundingClientRect();
    const tooltip = this.tooltipElement.getBoundingClientRect();
    const viewportPadding = 8;
    let top = trigger.top - tooltip.height - 9;
    let left = trigger.left + (trigger.width - tooltip.width) / 2;

    left = Math.max(viewportPadding, Math.min(left, window.innerWidth - tooltip.width - viewportPadding));
    if (top < viewportPadding) {
      top = trigger.bottom + 9;
      this.renderer.addClass(this.tooltipElement, 'app-tooltip-bottom');
    }

    this.renderer.setStyle(this.tooltipElement, 'left', `${left}px`);
    this.renderer.setStyle(this.tooltipElement, 'top', `${top}px`);
    this.renderer.addClass(this.tooltipElement, 'app-tooltip-visible');
  }

  @HostListener('mouseleave')
  @HostListener('focusout')
  @HostListener('window:scroll')
  @HostListener('window:resize')
  hide(): void {
    if (this.tooltipElement) {
      this.renderer.removeChild(document.body, this.tooltipElement);
      this.tooltipElement = null;
    }
  }

  ngOnDestroy(): void {
    this.hide();
  }
}
