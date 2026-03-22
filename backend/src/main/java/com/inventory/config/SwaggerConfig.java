package com.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration SpringDoc / OpenAPI.
 *
 * Swagger UI est accessible à /swagger-ui.html (autorisé sans auth dans SecurityConfig).
 *
 * Note auth : Swagger UI ne peut pas envoyer les cookies HttpOnly automatiquement.
 * Workaround : faire un POST /api/auth/login via Swagger — le navigateur stocke
 * les cookies, et les requêtes suivantes dans le même onglet les envoient automatiquement.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory SaaS API")
                        .description("API de gestion d'inventaire multi-tenant — bâtiments, zones, produits et mouvements de stock.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Inventory SaaS")
                        )
                );
    }
}
