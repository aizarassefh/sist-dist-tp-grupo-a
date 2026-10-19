package com.unla.museo.comun;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Museo Virtual — API REST",
                version = "1.0.0",
                description = "API REST del Museo Virtual: catálogo de obras, eventos, inscripciones, "
                        + "filtros favoritos y reportes de asistencia. Para probar los endpoints protegidos, "
                        + "iniciar sesión con POST /api/auth/login y pegar el accessToken devuelto en el botón "
                        + "Authorize (esquema Bearer)."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
