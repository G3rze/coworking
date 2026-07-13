# Coworking Space Management API

API REST para gestión de reservas de espacios de coworking (salas de reuniones, puestos de trabajo).

## 1. Requisitos

- **Java 21+**
- **Maven 3.9+**
- **Docker** (opcional, para containerización)

## 2. Instrucciones de Ejecución

### 2.1 Rápido (H2 embebido)

```bash
# Clonar y ejecutar
./mvnw spring-boot:run

# O construir y ejecutar JAR
./mvnw package -DskipTests
java -jar target/coworking-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en `http://localhost:8080`

### 2.2 Con Docker (PostgreSQL + App)

```bash
# Solo la base de datos
docker compose up -d postgres

# Base de datos + aplicación
docker compose --profile with-app up -d

# Ver logs
docker compose logs -f

# Detener
docker compose down
```

### 2.3 Desarrollo Local (IDE)

Usar el perfil `dev` (configurado por defecto en `application-dev.yml`):
- H2 en memoria (no requiere PostgreSQL)
- Consola H2 disponible en `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:coworking_dev`

## 3. Credenciales de Prueba

### Usuarios precargados (data.sql)

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| admin | admin123 | ADMIN |
| user | user123 | USER |

### JWT

Obtener token mediante `POST /api/v1/auth/login` con las credenciales anteriores.

## 4. Versionamiento de API

Esta API usa versionamiento semántico con prefijo `/api/v1/`. Todos los endpoints están versionados.

| Versión | Endpoint Base | Estado |
|---------|---------------|--------|
| v1 | `/api/v1` | Actual |

## 5. Estados de Reserva

| Estado | Descripción | Transiciones válidas |
|--------|-------------|---------------------|
| PENDING_PAYMENT | Reserva creada, esperando validación de pago | → CONFIRMED, → CANCELLED |
| CONFIRMED | Pago validado exitosamente | → COMPLETED (admin, después de fecha/hora), → CANCELLED |
| CANCELLED | Reserva cancelada | (terminal) |
| COMPLETED | Reserva finalizada (pasó la fecha/hora) | (terminal) |

## 6. Endpoints Principales

### Autenticación

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Espacios (GET: ADMIN y USER | POST/PUT/DELETE: solo ADMIN)

```http
GET /api/v1/spaces
POST /api/v1/spaces
PUT /api/v1/spaces/{id}
DELETE /api/v1/spaces/{id}
```

### Reservas

```http
# Crear reserva (USER)
POST /api/v1/reservations
{
  "spaceId": "uuid",
  "date": "2026-07-15",
  "startTime": "09:00",
  "endTime": "11:00"
}

# Confirmar reserva (pago) - ADMIN
POST /api/v1/reservations/{id}/confirm

# Completar reserva (después de fecha/hora) - ADMIN
POST /api/v1/reservations/{id}/complete

# Cancelar reserva (USER: propias | ADMIN: todas)
POST /api/v1/reservations/{id}/cancel

# Ver mis reservas (USER)
GET /api/v1/reservations/user/{userId}

# Ver todas (ADMIN)
GET /api/v1/reservations
```

### Reportes (ADMIN)

```http
GET /api/v1/reports/occupancy?dateFrom=2026-07-01&dateTo=2026-07-31
```

### Documentación API

```
Swagger UI: http://localhost:8080/swagger-ui/index.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

### Health Check

```
GET /actuator/health
GET /actuator/circuitbreakers
```

## 5. Decisiones de Diseño y Trade-offs

### 5.1 Stack Tecnológico y Justificación

#### Spring Boot 3.x + Java 21
- **Justificación:** Java 21 ofrece mejoras de performance con Virtual Threads. Spring Boot 3.x es la versión más reciente con soporte nativo para Jakarta EE 10 y mejor integración con GraalVM.
- **Alternativa considerada:** Spring Boot 2.7.x + Java 17 (aún soportado pero sin features modernos).

#### Spring Data JPA con @Query y EntityGraph
- **Justificación:** `@EntityGraph` evita problemas N+1 al cargar relaciones lazily solo cuando es necesario. `@Query` personalizada para `existsConflictingReservation` garantiza eficiencia en validaciones de solapamiento.
- **Evidencia:**
```java
@EntityGraph(attributePaths = {"space", "user"})
List<Reservation> findAll();

@Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.space.id = :spaceId " +
       "AND r.date = :date AND r.status IN ('PENDING_PAYMENT', 'CONFIRMED') " +
       "AND r.startTime < :endTime AND r.endTime > :startTime")
boolean existsConflictingReservation(...);
```

#### Spring Security con JWT
- **Justificación:** JWT es stateless y escalable horizontalmente. `JwtTokenProvider` genera tokens con claims de rol, `JwtAuthenticationFilter` valida cada request.
- **Alternativa:** OAuth2 Resource Server (más idiomático Spring pero overkill para este caso de uso).

#### Bean Validation + @ControllerAdvice
- **Justificación:** `jakarta.validation` valida DTOs en entrada. `GlobalExceptionHandler` centraliza errores y retorna JSON estructurado con códigos HTTP correctos.
- **Beneficio:** El código de negocio no se mezcla con validación.

#### Spring Boot Actuator (health, info, metrics)
- **Justificación:** `health` expone estado de la aplicación y dependencias (DB, circuit breakers). `info` muestra build info. `metrics` (Micrometer) exporta métricas para monitoreo Prometheus.
- **Endpoints:** `/actuator/health`, `/actuator/circuitbreakers`, `/actuator/metrics`

#### @Cacheable/@CacheEvict (Caffeine)
- **Justificación:** El reporte de ocupación es costoso (agrega datos de múltiples reservas). `@Cacheable` almacena resultados por 5 minutos. `@CacheEvict` invalida cuando se crean/cancelan reservas.
- **Evidencia:**
```java
@Cacheable(value = "occupancy", key = "#dateFrom.toString() + '-' + #dateTo.toString()")
public OccupancyReportResponse getOccupancyReport(...) { ... }

@CacheEvict(value = "occupancy", allEntries = true)
public ReservationResponse create(...) { ... }
```

#### OpenAPI/Swagger (springdoc-openapi)
- **Justificación:** Genera documentación interactiva automáticamente desde annotations. Configurado con `SecurityRequirement` para incluir Bearer token en Swagger UI.
- **URL:** `/swagger-ui/index.html`, `/v3/api-docs`

#### Testing (Mockito + Testcontainers)
- **Justificación:** Mockito para unit tests rápidos que prueban lógica de negocio aislada. Testcontainers para integración real con PostgreSQL en contenedor Docker.
- **Coverage:** 89 tests (unitarios + integración).

### 5.2 Arquitectura en Capas

```
controller → service → repository → entity
     ↓           ↓
    dto/response/mapper
```

**Justificación:** Separación clara de responsabilidades. Los controllers solo orquestan, la lógica de negocio está en services, y los repositories solo acceden a datos.

### 5.3 State Pattern para Reservas (GoF - Patrón de Comportamiento)

**Problema:** Sin estado, validar transiciones válidas (ej: no confirmar una ya cancelada) requiere múltiples `if/else` dispersos en el código.

**Solución:** State Pattern con `ReservationState` interface y implementaciones `PendingPaymentState`, `ConfirmedState`, `CancelledState`.

**Beneficio:** Cada estado sabe qué transiciones permite. Agregar el estado `COMPLETED` futuro solo requiere nueva clase, no modificar código existente.

```java
// Ejemplo: transición válida solo si el estado actual lo permite
ReservationState state = stateFactory.getState(reservation.getStatus());
if (!state.canConfirm()) {
    throw new IllegalStateException("Reservation cannot be confirmed");
}
state.confirm(reservation);
```

**Versus if/else:**
```java
// SIN State Pattern - código difícil de mantener
if (reservation.getStatus() == PENDING_PAYMENT) {
    if (canConfirm) { ... }
} else if (reservation.getStatus() == CONFIRMED) {
    // no se puede confirmar de nuevo
} else if (reservation.getStatus() == CANCELLED) {
    throw new Exception("Already cancelled");
}
// Agregar COMPLETED = agregar más if/else
```

### 5.4 Circuit Breaker sobre Servicio de Pago

**Problema:** El servicio de pago es externo y potencialmente lento/inestable. Sin resiliencia, una caída del servicio de pago bloquea toda la API.

**Solución:** Resilience4j con:
- `slidingWindowSize: 10` → evalúa últimas 10 llamadas
- `failureRateThreshold: 50` → abre circuito si >50% fallan
- `waitDurationInOpenState: 30s` → espera 30s antes de reintentar
- **Fallback:** deja reserva en `PENDING_PAYMENT` en lugar de fallar

### 5.5 @Transactional en ReservationServiceImpl

**Justificación:** Garantiza consistencia atómica al crear/confirmar/cancelar reservas. Si algo falla, rollback automático.

**Trade-off:** El `@Transactional` en `create()` bloquea desde que se crea hasta que se confirma. En sistemas de alta concurrencia se podría usar versionado optimista ( `@Version` ).

### 5.6 MockPaymentStrategy

**Decisión:** El servicio de pago está simulado (`MockPaymentStrategy`) en lugar de WireMock con servidor externo.

**Trade-off:** No requiere infraestructura adicional para tests, pero no prueba la integración real con la pasarela de pago.

### 5.7 Observer Pattern para Notificaciones (GoF - Patrón de Comportamiento)

**Problema:** Acoplar el envío de notificaciones directamente en el servicio de reservas genera tight-coupling y riesgo de inconsistencias si la transacción hace rollback.

**Solución:** ApplicationEventPublisher + @TransactionalEventListener
- `ReservationServiceImpl` publica `ReservationEvent` después de confirm/cancel
- `ReservationEventListener` recibe el evento SOLO después del commit (AFTER_COMMIT)
- `NotificationService` procesa asíncronamente (`@Async` con `notificationExecutor`)

**Beneficio:**
- **Desacoplamiento:** ReservationService no conoce NotificationService directamente
- **Consistencia:** Si la transacción hace rollback, no se envía notificación
- **Escalabilidad:** Notificaciones procesadas en thread pool separado (2-5 threads)
- **Testabilidad:** Fácil de mockear el evento sin enviar notificaciones reales

```java
// ReservationServiceImpl.java
eventPublisher.publishEvent(new ReservationEvent(this, reservation, previousStatus, newStatus));

// ReservationEventListener.java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleReservationEvent(ReservationEvent event) {
    if (event.getNewStatus() == ReservationStatus.CONFIRMED) {
        notificationService.sendReservationConfirmation(event.getReservation());
    }
    // ...
}
```

**Versus Observer manual o called directo:**
```java
// SIN Observer - acoplamiento directo
reservationService.confirm(id);
// Y dentro de confirm():
notificationService.sendConfirmation(reservation); // Si falla, todo falla
```

### 5.8 @ConfigurationProperties Centralizado

**Problema:** Usar `@Value` disperso en múltiples archivos genera:
- Valores duplicados en archivos de configuración
- Difícil de mantener consistencia
- Sin validación de tipos en tiempo de compilación

**Solución:** `AppProperties` consolidado con `@ConfigurationProperties(prefix = "app")`

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private JwtConfig jwt = new JwtConfig();
    private String timezone = "America/El_Salvador";

    public static class JwtConfig {
        private String secret;
        private Long expiration = 86400000L;
        // getters y setters
    }
    // getters y setters
}
```

**Beneficio:**
- **Centralización:** Todas las propiedades de aplicación en una sola clase
- **Type-safety:** Validación en tiempo de compilación
- **Sin duplicación:** Valores por defecto definidos solo en código
- **Documentación implícita:** La estructura de `AppProperties.JwtConfig` documenta las propiedades

**Clases que usan AppProperties:**
- `JwtTokenProvider` - usa `appProperties.getJwt().getSecret()`
- `AuthController` - usa `appProperties.getJwt().getExpiration()`
- `ZoneIdProvider` - usa `appProperties.getTimezone()`

## 6. Alcance Completado

Todos los requisitos funcionales y técnicos solicitados están implementados:

| Requisito | Implementación |
|-----------|----------------|
| Spring Boot 3.x + Java 21 | ✅ |
| Spring Data JPA con @Query/EntityGraph | ✅ |
| Spring Security con JWT | ✅ |
| Bean Validation + @ControllerAdvice | ✅ |
| Actuator (health, info, metrics) | ✅ |
| Profile configs + @ConfigurationProperties | ✅ |
| @Cacheable/@CacheEvict (Caffeine) | ✅ |
| @Async + ApplicationEventPublisher | ✅ |
| @Transactional | ✅ |
| OpenAPI/Swagger | ✅ |
| Testing (Mockito + Testcontainers) | ✅ |
| Dockerfile + docker-compose | ✅ |
| Circuit Breaker Resilience4j | ✅ |
| State Pattern (GoF) | ✅ |
| Observer Pattern (GoF) | ✅ |
| Versionamiento API (/api/v1/) | ✅ |
| Estado COMPLETED | ✅ |
| Strategy Pattern (Peak Hours Pricing) | ✅ |
| Optimistic Locking (@Version) | ✅ |
| Flyway Migraciones | ✅ |
| Métricas post-commit (@TransactionalEventListener) | ✅ |

### Servicios Externos Simulados

| Servicio | Implementación Actual | Para Producción |
|----------|---------------------|-----------------|
| **Pago** | `MockPaymentStrategy` (simula latencia + fallos) | Reemplazar con `RestTemplate`/`WebClient` a pasarela real (Stripe, PayPal) |
| **Email** | `NotificationService` (log.info) | Reemplazar con `JavaMailSender` o servicio como SendGrid |

## 7. Qué se Haría con Más Tiempo

1. **Redis** para caché distribuido (actualmente Caffeine en memoria local)
3. **WebClient reactivo** en lugar de RestTemplate para servicios externos
4. **OAuth2 Resource Server** en lugar de JWT manual (más idiomático Spring)
5. **Elasticsearch** para búsqueda full-text de espacios
6. **Micrometer + Prometheus** para métricas exportadas
7. **Rate limiting** con Bucket4j
8. **Estado COMPLETED** para reservas finalizadas (actualmente solo PENDING/CONFIRMED/CANCELLED)
9. **Estrategia de Tarifas** (Strategy pattern para precios según tipo de espacio/horario)
10. **Reintentos con backoff** para llamadas externas

## 8. Colección HTTP

Ver archivo `api-examples.http` para ejemplos de todas las operaciones con `curl`.

## 9. Librerías Adicionales y Justificación

| Librería | Versión | Justificación |
|----------|---------|---------------|
| **Lombok** | 1.18.x | Reduce boilerplate (getters, setters, constructors). Mejora legibilidad del código. Estándar en proyectos Spring Boot. |
| **Caffeine** | 3.x | Caché en memoria local. Elegido sobre Redis por simplicidad en desarrollo local. Para producción se recomendaría Redis. |
| **SpringDoc OpenAPI** | 2.8.16 | Generación automática de documentación OpenAPI 3.0 y Swagger UI. Integrado con Spring Security para JWT Bearer auth. |
| **JJWT** | 0.12.6 | Librería estándar para generación y validación de tokens JWT en Java. Más flexible que Spring Security OAuth2 para este caso de uso. |
| **DevTools** | 3.x | Hot reload durante desarrollo. Acelera el ciclo sin afectar producción. |
| **Resilience4j** | Spring Cloud BOM | Requerido para Circuit Breaker sobre servicio de pago externo. Más ligero que Netflix Hystrix. |
| **Testcontainers** | junit-jupiter | Tests de integración con PostgreSQL real en contenedor Docker. Garantiza que los tests replican el ambiente de producción. |
| **Micrometer Prometheus** | registry-prometheus | Exportación de métricas para Prometheus. Preparado para monitoreo en producción con Prometheus/Grafana. |

## 10. Licencia

Derechos Reservados Transformación Digital El Salvador, 2026
