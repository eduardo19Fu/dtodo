package xyz.pangosoft.dtodo.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprueba al arrancar que cada tabla y columna mapeada por las entidades JPA exista en la base de datos.
 *
 * <p>El esquema se migra a mano con scripts SQL ({@code spring.jpa.generate-ddl=false}), así que un script
 * olvidado solo se descubría cuando una operación fallaba en producción. Si falta algo, la aplicación no
 * arranca y el mensaje lista todo lo que falta. Solo se valida existencia, no tipos: el esquema legado tiene
 * diferencias de tipo inocuas (INT frente a Long, etc.) que {@code ddl-auto=validate} rechazaría.</p>
 *
 * <p>Se desactiva con {@code dtodo.schema-check.enabled=false}.</p>
 */
@Component
@ConditionalOnProperty(prefix = "dtodo.schema-check", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class VerificadorEsquema implements InitializingBean {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws SQLException {
        Map<String, Set<String>> esperado = columnasEsperadas(entityManagerFactory);
        List<String> faltantes = faltantes(esperado, columnasExistentes(dataSource));

        if (!faltantes.isEmpty()) {
            String detalle = String.join("\n  - ", faltantes);
            log.error("El esquema de la base de datos no coincide con las entidades. Faltan:\n  - {}", detalle);
            throw new IllegalStateException("Esquema de base de datos incompleto; ¿falta correr un script SQL? Faltan:\n  - "
                    + detalle);
        }
        log.info("Esquema de base de datos verificado: {} tablas mapeadas presentes", esperado.size());
    }

    /** Tablas y columnas físicas que usan las entidades y colecciones mapeadas, en minúsculas. */
    static Map<String, Set<String>> columnasEsperadas(EntityManagerFactory entityManagerFactory) {
        MappingMetamodelImplementor metamodelo = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel();
        Map<String, Set<String>> esperado = new TreeMap<>();

        metamodelo.forEachEntityDescriptor(entidad -> {
            entidad.getIdentifierMapping().forEachSelectable((i, columna) -> agregar(esperado, columna));
            entidad.forEachSelectable((i, columna) -> agregar(esperado, columna));
        });
        metamodelo.forEachCollectionDescriptor(coleccion -> {
            PluralAttributeMapping atributo = coleccion.getAttributeMapping();
            atributo.getKeyDescriptor().getKeyPart().forEachSelectable((i, columna) -> agregar(esperado, columna));
        });
        return esperado;
    }

    /** Columnas existentes en el esquema actual de la conexión, agrupadas por tabla, en minúsculas. */
    static Map<String, Set<String>> columnasExistentes(DataSource dataSource) throws SQLException {
        Map<String, Set<String>> existente = new TreeMap<>();
        try (Connection conexion = dataSource.getConnection()) {
            DatabaseMetaData metaData = conexion.getMetaData();
            try (ResultSet columnas = metaData.getColumns(conexion.getCatalog(), conexion.getSchema(), "%", "%")) {
                while (columnas.next()) {
                    existente.computeIfAbsent(normalizar(columnas.getString("TABLE_NAME")), t -> new TreeSet<>())
                            .add(normalizar(columnas.getString("COLUMN_NAME")));
                }
            }
        }
        return existente;
    }

    /** Diferencia legible: tablas completas faltantes y columnas faltantes en tablas existentes. */
    static List<String> faltantes(Map<String, Set<String>> esperado, Map<String, Set<String>> existente) {
        List<String> faltantes = new ArrayList<>();
        esperado.forEach((tabla, columnas) -> {
            Set<String> columnasExistentes = existente.get(tabla);
            if (columnasExistentes == null) {
                faltantes.add("tabla " + tabla);
                return;
            }
            columnas.stream()
                    .filter(columna -> !columnasExistentes.contains(columna))
                    .forEach(columna -> faltantes.add("columna " + tabla + "." + columna));
        });
        return faltantes;
    }

    private static void agregar(Map<String, Set<String>> esperado, SelectableMapping columna) {
        if (columna.isFormula() || columna.getContainingTableExpression() == null) {
            return;
        }
        esperado.computeIfAbsent(normalizar(columna.getContainingTableExpression()), t -> new TreeSet<>())
                .add(normalizar(columna.getSelectionExpression()));
    }

    /** Quita comillas y prefijo de esquema ({@code `db`.`tabla`} → {@code tabla}) y pasa a minúsculas. */
    static String normalizar(String identificador) {
        String limpio = identificador.replace("`", "").replace("\"", "");
        return limpio.substring(limpio.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
