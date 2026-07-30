package com.wellsfargo.counselor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 metadata. springdoc generates the spec from the controllers and serves Swagger UI at
 * {@code /swagger-ui.html} (raw spec at {@code /v3/api-docs}). A global HTTP bearer (JWT) scheme lets
 * the UI's "Authorize" button attach the token obtained from {@code /auth/login}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI portfolioManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Management System API")
                        .version("v1")
                        .description("Enterprise FinTech portfolio platform - advisors, accounts, an immutable "
                                + "trade ledger, valuation, analytics, automated rebalancing and live streaming.")
                        .license(new License().name("Educational use")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
