# NeuroLive Backend

API REST de NeuroLive desarrollada con Java y Spring Boot.
Gestiona usuarios, autenticación, dispositivos ESP32, telemetría biométrica, detección de crisis, vínculos clínicos, historial de eventos y análisis clínico.

## API pública

* URL de producción: https://neurolive-backend.azurewebsites.net
* healt point: https://neurolive-backend.azurewebsites.net/health

## Descripción

El backend recibe datos biométricos enviados desde el servicio WebSocket en tiempo real, los procesa, los almacena en PostgreSQL y expone endpoints para que el frontend consulte el estado del paciente, dispositivos, crisis y análisis clínico.

Flujo general:

```text
ESP32 / Simulador
→ WebSocket Railway
→ Backend Azure /internal/telemetry
→ PostgreSQL
→ Frontend Vercel
```

## Tecnologías utilizadas

* Java 21
* Spring Boot
* Spring Security
* JWT
* PostgreSQL
* Flyway
* Maven
* Azure App Service
* JPA / Hibernate
* API REST
* Integración con servicio WebSocket externo

## Funcionalidades principales

* Registro e inicio de sesión de usuarios.
* Autenticación mediante JWT.
* Gestión de roles:

  * Paciente
  * Médico
  * Cuidador
  * Usuario personal
* Vinculación de cuentas mediante token.
* Vinculación de dispositivos ESP32 por dirección MAC.
* Recepción interna de telemetría biométrica.
* Persistencia de BPM, SpO2 y contacto del sensor.
* Consulta de última telemetría del paciente.
* Detección de estados de riesgo o crisis.
* Registro de eventos de crisis.
* Cierre de crisis y almacenamiento de duración.
* Cuestionario SAM posterior a intervención.
* Exportación de datos en CSV.
* Auditoría de accesos clínicos.
* Integración con IA para análisis predictivo o clínico cuando está configurada.

## Endpoints principales

### Salud

```http
GET /health
```

### Autenticación

```http
POST /auth/register
POST /auth/login
```

### Usuario

```http
GET /users/me
PUT /users/me
```

### Dispositivos

```http
POST /devices/patients/{patientId}/link
GET /devices/patients/{patientId}
```

### Telemetría

```http
POST /internal/telemetry
GET /biometrics/patients/{patientId}/telemetry/latest
```

### Crisis

```http
GET /crises/patients/{patientId}
GET /crises/{crisisId}
POST /crises/{crisisId}/sam
GET /crises/patients/{patientId}/export
```

### Vínculos

```http
GET /links/me
PATCH /links/{linkId}/revoke
```

## Seguridad

Los endpoints públicos son limitados.
La mayoría de rutas requieren JWT:

```text
Authorization: Bearer <token>
```

Los endpoints internos usan token interno:

```text
X-Internal-Token: <internal-token>
```

No se deben subir tokens, contraseñas ni claves al repositorio.

## Variables de entorno

Variables requeridas en producción:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
INTERNAL_TOKEN=
APP_ALLOWED_ORIGINS=
REALTIME_SERVICE_URL=
GEMINI_API_KEY=
GEMINI_MODEL=
GEMINI_ENABLED=
```

## Ejecución local

Compilar y ejecutar tests:

```bash
./mvnw test
```

Generar JAR:

```bash
./mvnw -DskipTests package
```

Ejecutar localmente:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

## Despliegue en Azure

El backend se despliega como JAR en Azure App Service Java.

Comando usado para despliegue:

```powershell
az webapp deploy `
  --resource-group rg-neurolive `
  --name neurolive-backend `
  --src-path target/neuro-live-backend-0.0.1-SNAPSHOT.jar `
  --type jar
```

Verificar salud después del despliegue:

```powershell
curl.exe https://neurolive-backend.azurewebsites.net/health
```

## Base de datos

El backend usa PostgreSQL y migraciones con Flyway.
Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

No se deben modificar ni renombrar migraciones ya aplicadas en producción.

## Verificación recomendada

Antes de desplegar:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
git diff --check
```

Después de desplegar:

```powershell
curl.exe https://neurolive-backend.azurewebsites.net/health
```

## Estado del despliegue

El backend se encuentra desplegado en Azure App Service y conectado a PostgreSQL.
