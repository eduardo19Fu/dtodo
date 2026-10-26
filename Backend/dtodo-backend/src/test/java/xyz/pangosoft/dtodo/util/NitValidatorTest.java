package xyz.pangosoft.dtodo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NitValidatorTest {

    @Test
    void aceptaUnNitConDigitoVerificadorCorrecto() {
        assertTrue(NitValidator.esValido("4514627-6"));
        assertTrue(NitValidator.esValido("45146276"));
        assertTrue(NitValidator.esValido("1234567-9"));
    }

    @Test
    void aceptaElDigitoVerificadorK() {
        assertTrue(NitValidator.esValido("6-K"));
        assertTrue(NitValidator.esValido("6k"));
    }

    @Test
    void aceptaConsumidorFinalEnSusVariantes() {
        assertTrue(NitValidator.esValido("CF"));
        assertTrue(NitValidator.esValido("C/F"));
        assertTrue(NitValidator.esValido(" cf "));
        assertTrue(NitValidator.esConsumidorFinal("C/F"));
    }

    @Test
    void rechazaUnTelefonoGuardadoComoNit() {
        assertFalse(NitValidator.esValido("55551234"));
        assertFalse(NitValidator.esValido("50123456"));
    }

    @Test
    void rechazaUnDigitoVerificadorIncorrecto() {
        assertFalse(NitValidator.esValido("4514627-5"));
        assertFalse(NitValidator.esValido("1234567-K"));
    }

    @Test
    void rechazaValoresVaciosOConCaracteresInvalidos() {
        assertFalse(NitValidator.esValido(null));
        assertFalse(NitValidator.esValido(""));
        assertFalse(NitValidator.esValido("   "));
        assertFalse(NitValidator.esValido("ABC123"));
        assertFalse(NitValidator.esValido("K"));
        assertFalse(NitValidator.esValido("123456789012345"));
        assertFalse(NitValidator.esConsumidorFinal(null));
    }
}
