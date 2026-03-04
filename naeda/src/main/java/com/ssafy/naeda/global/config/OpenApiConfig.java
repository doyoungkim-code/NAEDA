package com.ssafy.naeda.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI naedaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAEDA API 문서")
                        .description("NAEDA 백엔드 API 명세입니다. 얼굴 인식과 포인트 기능을 제공합니다.")
                        .version("v1")
                        .contact(new Contact().name("NAEDA Backend Team"))
                        .license(new License().name("Internal Use")));
    }
}
