package com.aivideoassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Video Assistant Backend API")
                        .version("1.0.0")
                        .description("Enterprise Spring Boot REST API for AI meeting intelligence, automated transcription, summarization, action item extraction, and isolated RAG conversational search.")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("engineering@moveinsync.com"))
                        .license(new License().name("Apache 2.0")));
    }
}
