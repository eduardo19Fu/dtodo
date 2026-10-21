package xyz.pangosoft.dtodo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.service.impl.UsuarioServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("context-test")
class UsuarioApiControllerTest {

    private static final String BODY = "{"
            + "\"idUsuario\":3,\"usuario\":\"DAVID\",\"password\":\"x\",\"primerNombre\":\"JAIRON\","
            + "\"apellido\":\"AGUILAR\",\"enabled\":true,\"fechaRegistro\":\"2022-04-30T09:49:21\","
            + "\"correlativos\":[],\"roles\":[{\"idRole\":1,\"role\":\"ROLE_ADMIN\"}],"
            + "\"sucursal\":{\"idSucursal\":2,\"nombre\":\"MATAQUESCUINTLA\",\"direccion\":\"X\","
            + "\"codigoEstablecimientoSat\":2,\"esPrincipal\":false,\"fechaRegistro\":\"2026-10-21T07:13:06\","
            + "\"estado\":{\"idEstado\":1,\"estado\":\"ACTIVO\"},"
            + "\"usuario\":{\"idUsuario\":3,\"usuario\":\"DAVID\",\"enabled\":true}}}";

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioServiceImpl serviceUsuario;

    @Test
    void creaUsuarioConSucursal() throws Exception {
        when(serviceUsuario.save(any())).thenReturn(new Usuario());
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andDo(print()).andExpect(status().isCreated());
    }

    @Test
    void actualizaUsuarioConSucursal() throws Exception {
        when(serviceUsuario.update(any(), anyInt())).thenReturn(new Usuario());
        mockMvc.perform(put("/api/usuarios/3").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andDo(print()).andExpect(status().isCreated());
    }
}
