# ============================================================================
# Dockerfile para Spring Cloud Gateway
# ============================================================================

# Etapa 1: Construcción
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos Maven
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descargar dependencias
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Copiar código y compilar
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ===========================================================================
# Etapa 2: Imagen de producción
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root
RUN addgroup -S gateway && adduser -S gateway -G gateway

# Copiar JAR
COPY --from=builder /app/target/*.jar app.jar

RUN chown -R gateway:gateway /app
USER gateway

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", \
  "/app/app.jar"]
