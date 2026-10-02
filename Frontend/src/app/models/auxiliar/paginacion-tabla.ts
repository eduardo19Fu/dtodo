/**
 * Estado de paginación de una tabla alimentada por una respuesta paginada de Spring (`Page<T>`).
 * Evita repetir en cada listado los campos y el cálculo de las páginas visibles.
 */
export class PaginacionTabla {
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize: number;
  readonly pageSizeOptions: number[];
  isFirst = true;
  isLast = false;

  constructor(pageSize = 5, pageSizeOptions: number[] = [5, 10, 15, 25, 50]) {
    this.pageSize = pageSize;
    this.pageSizeOptions = pageSizeOptions;
  }

  /** Copia los datos de paginación de una respuesta `Page<T>` del backend. */
  actualizar(respuesta: any): void {
    this.paginaActual = respuesta.number;
    this.totalPaginas = respuesta.totalPages;
    this.totalElementos = respuesta.totalElements;
    this.pageSize = respuesta.size;
    this.isFirst = respuesta.first;
    this.isLast = respuesta.last;
  }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  /** Número del primer registro mostrado (1-based); 0 si no hay registros. */
  desde(): number {
    return this.totalElementos === 0 ? 0 : this.paginaActual * this.pageSize + 1;
  }

  hasta(registrosEnPagina: number): number {
    return this.paginaActual * this.pageSize + registrosEnPagina;
  }
}
