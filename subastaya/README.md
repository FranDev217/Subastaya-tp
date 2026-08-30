Trabajo Práctico de la materia Proyecto de Software - Ing. en Informática.

## Integrantes

- Destefani Franco
- Santillan Facundo

## Stack Tecnológico

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 4.1.1 (Spring Framework 7)
- **Persistencia:** Spring Data JPA / Hibernate
- **Base de datos:** PostgreSQL 16
- **Migraciones:** Flyway (esquema code-first)
- **Comunicación en tiempo real:** WebSockets (Spring WebSocket)
- **Documentación de API:** Springdoc OpenAPI (Swagger UI)
- **Build:** Maven
- **Contenedores:** Docker Compose (para levantar PostgreSQL localmente)

## Decisiones de arquitectura

- **Optimistic Locking:** las entidades `Subasta` y `Billetera` incluyen un
  campo `version` para evitar condiciones de carrera en operaciones
  concurrentes de puja y actualización de saldo. Ante conflicto, la API
  responde `409 Conflict` en lugar de un error genérico `500`.
- **Transaccionalidad (ACID):** las operaciones críticas (congelar/liberar
  saldo, liquidación final de subasta) se ejecutan dentro de bloques
  `@Transactional`, garantizando rollback completo ante cualquier fallo.
- **Endpoints RESTful:** rutas basadas exclusivamente en sustantivos
  plurales y jerarquías de recursos (ej. `GET /api/v1/subastas/{id}/pujas`),
  sin verbos en la URL.
- **Background Worker:** un proceso `@Scheduled` verifica periódicamente las
  subastas vencidas para liquidarlas o marcarlas como `DESIERTA`.
- **Auditoría:** los cambios críticos (cambios de estado, extensiones por
  anti-sniping, pujas rechazadas, acreditaciones manuales) quedan
  registrados de forma inmutable en una tabla de `AuditLog`.

## Requisitos previos

- JDK 17 o superior
- Docker Desktop
- Un IDE compatible con Maven (recomendado: IntelliJ IDEA)
- Git

No es necesario instalar PostgreSQL manualmente: se levanta con Docker.

## Cómo levantar el proyecto

1. **Clonar el repositorio**
```bash
   git clone https://github.com/TU_USUARIO/subastaya-tp.git
   cd subastaya-tp/subastaya
```

2. **Levantar la base de datos con Docker**
```bash
   docker compose up -d
```
Verificar que el contenedor esté sano:
```bash
   docker compose ps
```
Debería mostrar `subastaya-db` con estado `healthy`.

3. **Configurar el Run en el IDE**

   En IntelliJ: `Run → Edit Configurations → Add new → Application`
    - Main class: `com.unaj.subastaya.SubastayaApplication`
    - Module: `subastaya`
    - JDK: 17 (o superior instalado)

   **Importante:** si tu sistema operativo usa una zona horaria que
   PostgreSQL no reconoce (error típico:
   `FATAL: invalid value for parameter "TimeZone"`), agregar en
   **VM options**:

-Duser.timezone=America/Argentina/Buenos_Aires

(ajustar al identificador IANA correspondiente a tu ubicación si no
estás en Argentina).

4. **Correr la aplicación**

   La API queda disponible en `http://localhost:8080`.
   Documentación interactiva (Swagger UI) en:
   `http://localhost:8080/swagger-ui.html`

## Estructura del proyecto

subastaya/
├── src/main/java/com/unaj/subastaya/
│ ├── controller/ # Endpoints REST
│ ├── service/ # Lógica de negocio
│ ├── repository/ # Acceso a datos (Spring Data JPA)
│ ├── model/ # Entidades JPA
│ └── dto/ # Objetos de transferencia (request/response)
├── src/main/resources/
│ ├── db/migration/ # Migraciones Flyway (V1__init.sql, etc.)
│ └── application.properties
└── docker-compose.yaml # Definición de PostgreSQL local

![img.png](img.png)
_(Se irá actualizando a medida que se agreguen módulos.)_

## Convenciones de trabajo

- Ramas de trabajo: `feature/nombre-de-la-funcionalidad`
- No se trabaja directo sobre `main`: se integra vía Pull Request
- Commits descriptivos en español o inglés, consistente entre ambos

## Estado actual del TP

- [x] Setup del proyecto (Spring Boot + PostgreSQL + Docker)
- [ ] Modelado de entidades JPA y primera migración
- [ ] Lógica de escrow atómico
- [ ] Regla anti-sniping
- [ ] WebSockets - sala de subastas en vivo
- [ ] Background Worker de liquidación
- [ ] Auditoría de eventos
- [ ] Documentación Swagger completa.
