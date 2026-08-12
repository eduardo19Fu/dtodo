# Migración a Spring Boot 3.5.16

## Alcance ejecutado

La API compila sobre Java 17 y Spring Boot 3.5.16. La migración conserva `POST /oauth/token`, los grants `password` y `refresh_token`, y los claims consumidos por Angular (`user_name`, `authorities`, `id_usuario` y nombres). Cada rol de `Usuario.roles` se emite como autoridad `ROLE_*` y continúa funcionando con `@Secured`.

## Librerías actualizadas

| Componente | Antes | Ahora | Motivo |
| --- | --- | --- | --- |
| Spring Boot | 2.4.3 | 3.5.16 | Objetivo de la migración; requiere Java 17+ |
| Java | 8 | 17 | Base mínima soportada |
| Spring Security OAuth/JWT | OAuth 2.3.4 + JWT 1.0.9 | Security y OAuth2 Resource Server administrados por Boot | Los proyectos heredados están discontinuados |
| MySQL Connector/J | `mysql:mysql-connector-java` | `com.mysql:mysql-connector-j` | Coordenadas vigentes |
| Lombok | 1.18.30 explícito | Versión administrada por Boot + annotation processor | Compatibilidad con JDK recientes |
| JasperReports / fonts | 6.12.2 / 6.16.0 | 6.21.5 | Última línea 6.x compatible con los reportes existentes |
| iText local | JAR con `systemPath` | Dependencia PDF transitiva de Jasper | Elimina una dependencia local no reproducible |
| JAXB `javax` | API explícita sin uso | Eliminada | No existen imports JAXB en el proyecto |

No se adoptó JasperReports 7: rompe deliberadamente la compatibilidad con archivos `.jasper` y `.jrxml` 6.x. Los imports JPA, Validation y Servlet migraron de `javax.*` a `jakarta.*`; `javax.sql.DataSource` permanece porque pertenece al JDK.

## Seguridad y configuración requerida

- `DTODO_SECURITY_ALLOWED_ORIGINS`: orígenes CORS separados por coma; por defecto solo `http://localhost:4200`.

La clave de firma JWT se encuentra temporalmente como una constante Base64 de 256 bits en `JwtConfig`.
El identificador y secreto del cliente también se encuentran temporalmente como constantes en
`OAuthTokenController`. Deben externalizarse y rotarse antes de operar la API en producción.

Opcionales: `DTODO_SECURITY_JWT_ISSUER`, `DTODO_SECURITY_CLIENT_ID`, `DTODO_SECURITY_ACCESS_TOKEN_TTL` (por defecto `PT15M`) y `DTODO_SECURITY_REFRESH_TOKEN_TTL` (por defecto `PT8H`).

Access y refresh tokens tienen validadores separados de firma, expiración, issuer, audience y tipo. Un refresh token no puede utilizarse como Bearer. Las claves y el secreto que antes estaban incrustados deben considerarse comprometidos y rotarse antes del despliegue.

## Validación y despliegue

Ejecute `.\mvnw.cmd test` y luego `.\mvnw.cmd clean package -Ptest` con JDK 17. Pruebe login, renovación, rechazo 401/403 y cada combinación de roles contra una base aislada. El cambio de firma invalida todos los JWT anteriores, por lo que el despliegue requiere una nueva sesión de los usuarios.

El grant de contraseña se mantiene únicamente por compatibilidad con el frontend actual. El siguiente endurecimiento recomendado es migrar el navegador a Authorization Code con PKCE o a un BFF; un cliente público no puede proteger un secreto incrustado.

## Referencias oficiales

- [Requisitos de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Guía de migración a Spring Boot 3](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Migración de Spring Security 6](https://docs.spring.io/spring-security/reference/6.5/migration/index.html)
- [OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Compatibilidad de JasperReports 7](https://github.com/Jaspersoft/jasperreports)
