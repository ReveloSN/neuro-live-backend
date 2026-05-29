# NeuroLive Backend

API REST de NeuroLive desarrollada con Java y Spring Boot. Gestiona usuarios, autenticacion, dispositivos ESP32, telemetria biometrica, deteccion de crisis, vinculos clinicos, historial de eventos y analisis clinico.

## API publica

* URL de produccion: https://neurolive-backend.azurewebsites.net
* Health endpoint: https://neurolive-backend.azurewebsites.net/health

## Descripcion

El backend recibe datos biometricos enviados desde el servicio WebSocket en tiempo real, los procesa, los almacena en PostgreSQL y expone endpoints para que el frontend consulte el estado del paciente, dispositivos, crisis y analisis clinico.

Flujo general:

```text
ESP32 / Simulador
-> WebSocket Railway
-> Backend Azure /internal/telemetry
-> PostgreSQL
-> Frontend Vercel
```

## Tecnologias utilizadas

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
* Integracion con servicio WebSocket externo

## Funcionalidades principales

* Registro e inicio de sesion de usuarios.
* Autenticacion mediante JWT.
* Gestion de roles:

  * Paciente
  * Medico
  * Cuidador
  * Usuario personal
* Vinculacion de cuentas mediante token.
* Vinculacion de dispositivos ESP32 por direccion MAC.
* Recepcion interna de telemetria biometrica.
* Persistencia de BPM, SpO2 y contacto del sensor.
* Consulta de ultima telemetria del paciente.
* Deteccion de estados de riesgo o crisis.
* Registro de eventos de crisis.
* Cierre de crisis y almacenamiento de duracion.
* Cuestionario SAM posterior a intervencion.
* Exportacion de datos en CSV.
* Auditoria de accesos clinicos.
* Integracion con IA para analisis predictivo o clinico cuando esta configurada.

## Endpoints principales

### Salud

```http
GET /health
```

### Autenticacion

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

### Telemetria

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

### Vinculos

```http
GET /links/me
PATCH /links/{linkId}/revoke
```

## Seguridad

Los endpoints publicos son limitados. La mayoria de rutas requieren JWT:

```text
Authorization: Bearer <token>
```

Los endpoints internos usan token interno:

```text
X-Internal-Token: <internal-token>
```

No se deben subir tokens, contrasenas ni claves al repositorio.

## Variables de entorno

Variables requeridas en produccion:

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

## Ejecucion local

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

Verificar salud despues del despliegue:

```powershell
curl.exe https://neurolive-backend.azurewebsites.net/health
```

## Base de datos

El backend usa PostgreSQL y migraciones con Flyway. Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

No se deben modificar ni renombrar migraciones ya aplicadas en produccion.

## Verificacion recomendada

Antes de desplegar:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
git diff --check
```

Despues de desplegar:

```powershell
curl.exe https://neurolive-backend.azurewebsites.net/health
```

## Estado del despliegue

El backend se encuentra desplegado en Azure App Service y conectado a PostgreSQL.

## Participacion del equipo

### Ana Maria Ruales

**Rol:** Analisis, diseno, frontend e IoT.

Ana Maria se encargo del analisis de necesidades del usuario, el diseno de la experiencia de uso, el desarrollo del frontend y la implementacion del componente IoT del proyecto. Su participacion abarco distintas etapas del proceso de construccion, desde la definicion de requisitos hasta la integracion de los componentes funcionales del sistema.

En la fase de diseno, realizo una revision de referentes especializados en accesibilidad, usabilidad y diseno orientado a personas neurodivergentes. A partir de este analisis, definio decisiones visuales relacionadas con la paleta cromatica, la tipografia, la jerarquia de informacion y la densidad de los elementos en pantalla, procurando que la interfaz respondiera a las caracteristicas cognitivas y sensoriales de la poblacion objetivo. El diseno tomo como referencia los criterios de accesibilidad WCAG AA.

Asimismo, desarrollo el frontend mediante Next.js y TypeScript, integrando las vistas principales de la aplicacion y su comunicacion con los servicios del sistema. Tambien asumio la responsabilidad del componente de hardware e IoT, incluyendo la integracion fisica del microcontrolador ESP32 con el sensor biometrico MAX30102, el modulo de iluminacion RGB WS2812 y el reproductor de audio DFPlayer Mini. Esta labor incluyo la programacion del firmware cargado en el microcontrolador, desarrollado mediante Arduino IDE.

### Santiago Nicolas Revelo Saavedra

**Rol:** Backend, infraestructura, inteligencia artificial y servidor WebSocket.

Santiago Nicolas Saavedra se encargo del diseno e implementacion de la arquitectura backend del sistema, la infraestructura de despliegue, la integracion de inteligencia artificial y el desarrollo del servidor WebSocket. El backend fue construido con Spring Boot e incluyo servicios REST, autenticacion basada en JWT, gestion de usuarios por roles, control de acceso a la informacion clinica, vinculacion entre pacientes, cuidadores y profesionales de salud, y organizacion de la logica de negocio mediante patrones de diseno de software.

Adicionalmente, implemento componentes asociados al procesamiento de telemetria biometrica, la deteccion y gestion de eventos de crisis, el registro de respuestas SAM, la generacion de analiticas clinicas y la persistencia de informacion relevante para el seguimiento del paciente. Tambien participo en la estructuracion de la base de datos, el manejo de migraciones, la validacion de entidades y la definicion de pruebas unitarias e integracion para verificar el comportamiento del backend.

En el componente de comunicacion en tiempo real, desarrollo el servidor WebSocket en Python con FastAPI, encargado de actuar como puente entre el dispositivo ESP32 y el backend principal. Este servicio permite recibir informacion del dispositivo, validar su comunicacion y reenviar la telemetria al sistema central. Finalmente, apoyo el despliegue de los servicios en la nube y la configuracion de infraestructura necesaria para la integracion entre frontend, backend, base de datos y servicio WebSocket.
