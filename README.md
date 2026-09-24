# Museo

API REST para la gestión de eventos de un museo. El proyecto está desarrollado con Spring Boot, Spring Data JPA y Spring Security. Incluye autenticación mediante JWT, autorización basada exclusivamente en roles y exportación de eventos a Excel.

## Requisitos

- Java JDK 21.
- Maven 3.9+ o Maven Wrapper incluido en el proyecto (`./mvnw`).
- MySQL 8.0+ ejecutándose localmente.
- Una base de datos llamada `museodb`.

La versión de Java está definida en `pom.xml`:

```xml
<java.version>21</java.version>
<maven.compiler.release>21</maven.compiler.release>
```

## Configuración de la base de datos

El perfil activo por defecto es `local`:

```properties
spring.profiles.active=local
```

Por ese motivo se utiliza `src/main/resources/application-local.properties`, que actualmente está configurado para MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/museodb
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=xxxxxxxx
spring.datasource.password=xxxxxxxx
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Antes de iniciar la aplicación, crear la base de datos:

```sql
CREATE DATABASE museodb;
```

Si el usuario o la contraseña de MySQL son diferentes, modificar estas propiedades en `application-local.properties`:

```properties
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD
```

El conector JDBC de MySQL ya está incluido en `pom.xml` como dependencia de runtime.

## Instalación y ejecución

Desde la raíz del proyecto:

### macOS/Linux

```bash
chmod +x mvnw
./mvnw clean install
./mvnw spring-boot:run
```

### Windows

```bat
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

También se puede ejecutar el JAR generado:

```bash
./mvnw clean package
java -jar target/museo-0.0.1-SNAPSHOT.jar
```

La aplicación queda disponible en:

```text
http://localhost:8000
```

## Configuración JWT

El token de acceso se genera al iniciar sesión y contiene un único rol del usuario en el claim `role`:

```json
{
  "sub": "usuario@ejemplo.com",
  "role": "CURADOR"
}
```

Las propiedades relevantes son:

```properties
jwt.secret=test-secret-with-at-least-32-characters
jwt.expiration-ms=900000
```

`jwt.expiration-ms` está expresado en milisegundos. El valor actual equivale a 15 minutos.

Para un entorno real se debe reemplazar `jwt.secret` por una clave segura, larga y externa al repositorio.

## Roles y autorización

Los roles utilizados por la aplicación son:

- `ADMINISTRADOR`
- `CURADOR`
- `VISITANTE`

La autorización se realiza con la anotación `@RequiresRoles`. Si se declaran varios roles, alcanza con que el usuario tenga uno de ellos:

```java
@RequiresRoles({Roles.ADMIN, Roles.CURADOR})
```

El cliente debe enviar el JWT en el header:

```http
Authorization: Bearer <token>
```

Los endpoints anotados responden `401 Unauthorized` cuando falta el token o es inválido, y `403 Forbidden` cuando el token es válido pero el rol no está autorizado.

## Endpoints principales

### Registro

```http
POST /api/auth/register
Content-Type: application/json
```

Ejemplo de body:

```json
{
  "email": "usuario@ejemplo.com",
  "firstName": "Nombre",
  "lastName": "Apellido",
  "roleId": "VISITANTE",
  "phoneNumber": "1112345678",
  "password": "Password1!"
}
```

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "usuario@ejemplo.com",
  "password": "Password1!"
}
```

Respuesta exitosa:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

### Usuario autenticado

```http
GET /api/auth/me
Authorization: Bearer <token>
```

### Eventos

```http
GET  /api/eventos
POST /api/eventos
GET  /api/eventos/exportar
```

`GET /api/eventos/exportar` requiere un JWT cuyo rol sea `ADMINISTRADOR` o `CURADOR`.

## Consola H2

El proyecto conserva la dependencia de H2 para pruebas y desarrollo alternativo. Sin embargo, con el perfil `local` activo la conexión utilizada es MySQL. La configuración H2 definida en `application.properties` no es la configuración efectiva del perfil local.

Para usar H2 se debe cambiar el perfil y/o las propiedades del datasource, por ejemplo:

```properties
spring.datasource.url=jdbc:h2:mem:museodb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```

## Pruebas

Ejecutar la suite de tests con:

```bash
./mvnw test
```

Los tests unitarios mockean los repositorios y cubren autenticación, servicios de usuarios, JWT, autorización por roles y exportación de eventos. No se incluye un test específico para `EventoController`.

## Estructura general

```text
src/main/java/com/unla/museo/
├── aspect/          # Autorización por roles
├── configuration/   # Configuración de Spring Security
├── controllers/     # Endpoints REST y filtro JWT
├── entities/        # Entidades JPA
├── repositories/    # Repositorios de persistencia
├── services/        # Interfaces y servicios de negocio
└── helper/          # Utilidades y mappers
```

Los scripts SQL de roles se encuentran en:

```text
src/main/resources/migration/
```

La creación y actualización de tablas se realiza actualmente mediante `spring.jpa.hibernate.ddl-auto=update`. Si se requiere ejecutar migraciones versionadas automáticamente, se deberá configurar una herramienta como Flyway o Liquibase.
