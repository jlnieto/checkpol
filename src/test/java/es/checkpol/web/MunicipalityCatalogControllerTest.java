package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.service.MunicipalityCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MunicipalityCatalogController.class)
@Import(SecurityConfig.class)
class MunicipalityCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MunicipalityCatalogService municipalityCatalogService;

    @Test
    void returnsSpanishMunicipalitiesForPostalCode() throws Exception {
        when(municipalityCatalogService.findSpanishOptionsByPostalCode("28001"))
            .thenReturn(List.of(new MunicipalityCatalogService.MunicipalityOption("28079", "Madrid", "28", "Madrid")));

        mockMvc.perform(get("/municipality-catalog/spanish-municipalities").param("postalCode", "28001"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [
                  {
                    "municipalityCode": "28079",
                    "municipalityName": "Madrid",
                    "provinceCode": "28",
                    "provinceName": "Madrid"
                  }
                ]
                """));
    }

    @Test
    void returnsEmptyArrayWhenPostalCodeHasNoCatalogEntries() throws Exception {
        when(municipalityCatalogService.findSpanishOptionsByPostalCode("99999"))
            .thenReturn(List.of());

        mockMvc.perform(get("/municipality-catalog/spanish-municipalities").param("postalCode", "99999"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}
