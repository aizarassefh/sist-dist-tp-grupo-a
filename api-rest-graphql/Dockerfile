# ============================================================================
# Dockerfile para API REST/GraphQL Spring Boot con gRPC
# ============================================================================

# Etapa 1: Construcción
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar dependencias y Maven Wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Copiar el código fuente y construir
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ===========================================================================
# Etapa 2: Imagen de producción
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root
RUN addgroup -S museo && adduser -S museo -G museo

# Copiar el JAR construido
COPY --from=builder /app/target/*.jar app.jar

# Cambiar permisos
RUN chown -R museo:museo /app

USER museo

# Puerto REST
EXPOSE 8000
# Puerto gRPC
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=docker", \
  "-jar", \
  "/app/app.jar"]
