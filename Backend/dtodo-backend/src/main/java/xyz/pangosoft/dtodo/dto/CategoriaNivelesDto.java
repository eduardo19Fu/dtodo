package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;

/** Los porcentajes de descuento activos de una categoría de cliente. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaNivelesDto {

    private CategoriaClienteEnum categoria;
    private List<BigDecimal> porcentajes;
}
