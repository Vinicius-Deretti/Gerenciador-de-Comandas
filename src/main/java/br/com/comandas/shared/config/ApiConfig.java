package br.com.comandas.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ApiConfig {
    @Bean
    ZoneId estabelecimentoZoneId(@Value("${estabelecimento.fuso}") String fuso) {
        if (!ZoneId.getAvailableZoneIds().contains(fuso)) {
            throw new IllegalArgumentException("ESTABELECIMENTO_FUSO deve ser um identificador IANA válido.");
        }
        return ZoneId.of(fuso);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Comandas API").version("v1")
            .description("Um estabelecimento. Valores em BRL. Instantes em UTC; filtros por datas locais no fuso configurado. "
                + "Listas usam page (base zero) e size (1 a 100). "
                + "Finalização repetida retorna 409. Erros seguem ApiError."));
    }
}
