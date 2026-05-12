package com.finai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione OpenAPI 3 / Swagger UI.
 * Accessibile in sviluppo su: http://localhost:3001/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finaiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FINAI API")
                        .description("Advisor finanziario personale per investitori italiani. " +
                                "Dati di mercato real-time, analisi tecnica, alert, portafoglio e IPO.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FINAI")
                                .url("https://github.com/giole89/TestClaude"))
                        .license(new License().name("MIT")));
    }
}
