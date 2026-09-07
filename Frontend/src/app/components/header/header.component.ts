import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  @Input() sidebarCollapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  pantallaCompleta = false;

  async alternarPantallaCompleta(): Promise<void> {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      } else {
        await document.exitFullscreen();
      }
    } catch (error) {
      console.error('No fue posible cambiar el modo de pantalla completa.', error);
    }
  }

  @HostListener('document:fullscreenchange')
  sincronizarPantallaCompleta(): void {
    this.pantallaCompleta = !!document.fullscreenElement;
  }
}
