package com.orderprocessing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Processing System API")
                        .version("1.0.0")
                        .description("Backend API for the Order Processing System assignment. " +
                                "Supports order creation, retrieval, filtering, and cancellation.")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@orderprocessing.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
