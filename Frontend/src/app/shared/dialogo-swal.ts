import Swal, { SweetAlertOptions, SweetAlertResult } from 'sweetalert2';

/** Color de acento del diálogo: cada tipo de acción usa el suyo (avanzar, peligro, información). */
export type TonoDialogo = 'verde' | 'rojo' | 'azul';

export interface OpcionesDialogo extends SweetAlertOptions {
  /** Clase de Font Awesome del icono de la cabecera, por ejemplo `fa-dolly`. */
  iconoFa?: string;
  tono?: TonoDialogo;
}

/**
 * SweetAlert con el estilo de los demás diálogos del sistema (ver `.app-dialog` en styles.css): barra de acento,
 * icono en círculo, campos con la altura de los formularios y botones con el mismo peso que los de los listados.
 */
export function dialogo(opciones: OpcionesDialogo): Promise<SweetAlertResult> {
  const { iconoFa, tono, customClass, ...resto } = opciones;
  const clases = typeof customClass === 'object' && customClass ? customClass : {};
  const configuracion: SweetAlertOptions = {
    buttonsStyling: false,
    reverseButtons: true,
    focusCancel: false,
    iconHtml: `<i class="fas ${iconoFa || 'fa-question'}" aria-hidden="true"></i>`,
    icon: 'info',
    ...resto,
    customClass: {
      popup: `app-dialog app-dialog-${tono || 'verde'}`,
      icon: 'app-dialog-icon',
      title: 'app-dialog-title',
      htmlContainer: 'app-dialog-content',
      input: 'app-dialog-input',
      actions: 'app-dialog-actions',
      confirmButton: 'app-dialog-confirm',
      cancelButton: 'app-dialog-cancel',
      validationMessage: 'app-dialog-validation',
      ...clases
    }
  };
  return Swal.fire(configuracion);
}

/** Escapa texto para incrustarlo en el HTML de un diálogo. */
export function escaparHtml(texto: string): string {
  return String(texto === null || texto === undefined ? '' : texto)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
