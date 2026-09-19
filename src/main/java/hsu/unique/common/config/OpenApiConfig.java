package hsu.unique.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI uniqueOpenApi() {
        return new OpenAPI().info(new Info()
                .title("UNIQUE 숫자 게임 API")
                .version("v1")
                .description("익명 Cookie 기반 축제 숫자 게임 백엔드 API"));
    }
}
