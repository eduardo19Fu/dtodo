import { AfterViewInit, Component, HostListener, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements AfterViewInit, OnDestroy {
  title = 'frontend';
  sidebarColapsado = false;
  sidebarMovilAbierto = false;
  preloaderVisible = true;
  preloaderCerrando = false;

  private preloaderTimer: ReturnType<typeof setTimeout>;
  private desmontarPreloaderTimer: ReturnType<typeof setTimeout>;

  constructor(
    public router: Router,
    public authService: AuthService
  ){}

  get esRutaPublica(): boolean {
    return this.router.url === '/' || this.router.url === '/login';
  }

  ngAfterViewInit(): void {
    this.preloaderTimer = setTimeout(() => {
      this.preloaderCerrando = true;
      this.desmontarPreloaderTimer = setTimeout(() => this.preloaderVisible = false, 320);
    }, 1200);
  }

  ngOnDestroy(): void {
    clearTimeout(this.preloaderTimer);
    clearTimeout(this.desmontarPreloaderTimer);
  }

  alternarSidebar(): void {
    if (window.innerWidth <= 991) {
      this.sidebarMovilAbierto = !this.sidebarMovilAbierto;
      return;
    }
    this.sidebarColapsado = !this.sidebarColapsado;
  }

  expandirSidebar(): void {
    this.sidebarColapsado = false;
  }

  cerrarSidebarMovil(): void {
    this.sidebarMovilAbierto = false;
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 991) {
      this.sidebarMovilAbierto = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cerrarSidebarMovil();
  }
}
