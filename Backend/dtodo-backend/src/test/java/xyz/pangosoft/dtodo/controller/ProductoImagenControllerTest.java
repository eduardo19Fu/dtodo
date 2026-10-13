package xyz.pangosoft.dtodo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.pangosoft.dtodo.service.IProductoService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductoImagenControllerTest {

    @Test
    void entregaFotografiaComoImagenEnLugarDeJson() throws Exception {
        IProductoService service = mock(IProductoService.class);
        byte[] bytes = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
        when(service.cargar("producto.jpeg")).thenReturn(new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "producto.jpeg";
            }
        });
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoApiController(service, null, null, null, null)).build();

        mvc.perform(get("/api/uploads/img/producto.jpeg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"producto.jpeg\""))
                .andExpect(content().bytes(bytes));
    }
}
