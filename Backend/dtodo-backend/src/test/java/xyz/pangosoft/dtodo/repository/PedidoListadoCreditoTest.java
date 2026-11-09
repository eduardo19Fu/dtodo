package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;

/** La lista de pedidos trae la autorización del crédito, para no ofrecer avanzar un pedido que pasa del límite. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class PedidoListadoCreditoTest {

    @Autowired
    private IProformaRepository proformaRepository;
    @Autowired
    private EntityManager em;

    private Proforma pedido(String numero, Estado estado, Usuario usuario, Cliente cliente, EstadoAutorizacionEnum credito) {
        Proforma proforma = new Proforma();
        proforma.setNoProforma(numero);
        proforma.setTotal(new BigDecimal("100.00"));
        proforma.setEstado(estado);
        proforma.setUsuario(usuario);
        proforma.setCliente(cliente);
        proforma.setTipoProforma(TipoProformaEnum.MAYORISTA);
        proforma.setEstadoPedido(EstadoPedidoEnum.CONFIRMADA);
        proforma.setCreditoAutorizacion(credito);
        em.persist(proforma);
        return proforma;
    }

    @Test
    void cadaFilaTraeElEstadoDeAutorizacionDelCredito() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        Usuario vendedor = Usuario.builder().usuario("irene").primerNombre("Irene").apellido("Ortega").password("x").build();
        em.persist(vendedor);
        Cliente cliente = Cliente.builder().nombre("ANA PEREZ").nit("4514627-6").build();
        em.persist(cliente);
        pedido("A1P", activo, vendedor, cliente, null);
        pedido("B2P", activo, vendedor, cliente, EstadoAutorizacionEnum.PENDIENTE);
        pedido("C3P", activo, vendedor, cliente, EstadoAutorizacionEnum.RECHAZADA);
        em.flush();
        em.clear();

        Date desde = Date.from(LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date hasta = Date.from(LocalDate.of(2100, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<PedidoDto> filas = proformaRepository.findPedidos(TipoProformaEnum.MAYORISTA,
                Arrays.asList(EstadoPedidoEnum.CONFIRMADA), null, null, null, desde, hasta, PageRequest.of(0, 50));

        assertEquals(3, filas.size());
        assertEquals(List.of("A1P", "B2P", "C3P"),
                filas.stream().map(PedidoDto::getNoProforma).sorted().collect(Collectors.toList()));
        assertNull(filas.stream().filter(f -> "A1P".equals(f.getNoProforma())).findFirst().get().getCreditoAutorizacion());
        assertEquals(EstadoAutorizacionEnum.PENDIENTE,
                filas.stream().filter(f -> "B2P".equals(f.getNoProforma())).findFirst().get().getCreditoAutorizacion());
        assertEquals(EstadoAutorizacionEnum.RECHAZADA,
                filas.stream().filter(f -> "C3P".equals(f.getNoProforma())).findFirst().get().getCreditoAutorizacion());
    }
}
