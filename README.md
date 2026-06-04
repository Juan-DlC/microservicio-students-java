# ms-students

Microservicio responsable del ciclo de vida del estudiante y de la lógica transaccional de matrículas en el sistema UniRemington. Se comunica con `ms-courses` vía OpenFeign para reservar y liberar cupos.

## Puerto

`8082`

## Cómo ejecutar

> Requiere que eureka-server y ms-courses estén corriendo primero.

```bash
mvn spring-boot:run
```

O ejecutar `MsStudentsApplication.java` desde el IDE.

## Swagger UI

```
http://localhost:8082/swagger-ui/index.html
```

## Endpoints

| Método | URL | Descripción | HTTP |
|---|---|---|---|
| GET | `/api/students` | Listar todos los estudiantes | 200 |
| GET | `/api/students/{id}` | Obtener estudiante por ID | 200 / 404 |
| GET | `/api/students/{id}/enrollments` | Listar todas las matrículas de un estudiante | 200 / 404 |
| POST | `/api/students` | Crear estudiante | 200 |
| PUT | `/api/students/{id}` | Actualizar datos del estudiante | 200 / 404 |
| PATCH | `/api/students/{id}/status?isActive=false` | Cambiar estado activo/inactivo | 200 / 404 |
| DELETE | `/api/students/{id}` | Eliminar estudiante | 200 / 404 |
| PUT | `/api/students/{id}/enroll/{courseId}` | Matricular en un curso | 200 / 404 / 409 |
| PUT | `/api/students/enrollments/{enrollmentId}/cancel` | Cancelar matrícula | 200 / 404 |

## Modelos

**Student**
```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Perez",
  "email": "juan@test.com",
  "active": true
}
```

**Enrollment**
```json
{
  "id": 1,
  "studentId": 1,
  "courseId": 2,
  "status": "ACTIVE",
  "enrolledAt": "2026-05-31T10:00:00"
}
```

## Lógica de matrícula

1. Valida que el estudiante exista y esté activo (`isActive = true`).
2. Llama a `PUT /api/courses/{id}/reserve-slot` en ms-courses vía Feign.
3. Si ms-courses devuelve éxito, guarda el registro de matrícula en H2.
4. Si ms-courses devuelve 409 (sin cupos), la matrícula es rechazada.

## Excepciones de negocio

| Excepción | Código HTTP | Descripción |
|---|---|---|
| `StudentNotFoundException` | 404 | El estudiante no existe |
| `StudentNotActiveException` | 409 | El estudiante está inactivo |
| `EnrollmentNotFoundException` | 404 | La matrícula no existe |

## Pruebas

```bash
mvn verify
```

Ejecuta pruebas unitarias (JUnit 6 + Mockito 5.20) y valida cobertura Jacoco ≥ 80%.

- `StudentServiceTest` — lógica de negocio pura, con mocks de repositorio y FeignClient
- `StudentControllerTest` — endpoints con MockMvc standalone

## Stack

- Spring Boot 4.0.6 / Spring Cloud 2025.1.1
- Spring Data JPA + H2 (en memoria)
- OpenFeign (comunicación con ms-courses)
- springdoc-openapi 3.0.3
- Jacoco 0.8.12
