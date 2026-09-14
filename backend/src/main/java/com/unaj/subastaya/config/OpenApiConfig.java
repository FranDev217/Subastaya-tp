package com.unaj.subastaya.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI subastaYaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SubastaYa API")
                        .description("""
                                API REST de SubastaYa: catálogo y publicación de subastas, pujas con \
                                escrow atómico y regla anti-sniping, billetera virtual y panel de \
                                actividad del usuario.

                                Todavía no hay sesión/token: los endpoints que necesitan identificar \
                                al usuario reciben su id explícito (ej. `compradorId`, `vendedorId`, \
                                `usuarioId`), tal como hoy lo usa el frontend guardando el resultado \
                                del login en `localStorage`.""")
                        .version("v1")
                        .contact(new Contact()
                                .name("SubastaYa")
                                .url("https://github.com/FranDev217/subastaya-tp")));
    }
}
