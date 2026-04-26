# NeuroLive Backend API Routes

## Swagger y OpenAPI

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## Endpoints publicos

### Estado del servicio

- `GET /`
  - Verifica que el backend este arriba y devuelve informacion basica del servicio.
- `GET /health`
  - Devuelve un estado simple de salud del backend.
- `GET /actuator/health`
  - Expone el health check de Spring Actuator.

### Autenticacion y recuperacion

- `POST /auth/register`
  - Registra un nuevo usuario en la plataforma.
- `POST /auth/login`
  - Autentica un usuario y devuelve el JWT para acceder a rutas protegidas.
- `POST /auth/account-recovery/request`
  - Solicita el inicio del flujo de recuperacion de cuenta.
- `POST /auth/account-recovery/validate`
  - Valida el codigo o token de recuperacion.
- `POST /auth/account-recovery/reset`
  - Actualiza la contrasena al finalizar la recuperacion.

## Endpoints protegidos por JWT

### Perfil de usuario

- `GET /users/me`
  - Consulta el perfil del usuario autenticado.
- `PUT /users/me`
  - Actualiza el perfil del usuario autenticado.

### Vinculos entre usuarios

- `POST /links/tokens`
  - Genera un token de vinculacion para conectar un paciente con un cuidador o medico.
- `POST /links/redeem`
  - Canjea un token de vinculacion emitido previamente.
- `GET /links/me`
  - Lista las relaciones activas del usuario autenticado.

### Biometria y monitoreo

- `POST /biometrics/telemetry`
  - Ingresa telemetria biometrica de un paciente al pipeline de analisis.
- `POST /biometrics/keystrokes`
  - Registra dinamica de tecleo para seguimiento conductual.
- `GET /biometrics/patients/{patientId}/baseline`
  - Consulta la linea base biometrica de un paciente.
- `POST /biometrics/patients/{patientId}/thresholds`
  - Configura umbrales de activacion biometrica para un paciente.
- `POST /biometrics/me/thresholds`
  - Configura umbrales biometrico-personales del usuario autenticado.
- `POST /biometrics/patients/{patientId}/consent`
  - Registra el consentimiento del paciente para monitoreo biometrico.
- `GET /biometrics/patients/{patientId}/keystrokes/recent`
  - Obtiene el historial reciente de tecleo de un paciente.

### Crisis y analitica clinica

- `GET /crises/{crisisId}`
  - Consulta el detalle de una crisis especifica.
- `GET /crises/patients/{patientId}`
  - Lista crisis de un paciente, con filtro opcional por rango de fechas.
- `POST /crises/{crisisId}/close`
  - Cierra una crisis y registra su estado final.
- `POST /crises/{crisisId}/sam`
  - Registra la respuesta SAM del paciente durante una crisis.
- `GET /crises/{crisisId}/sam`
  - Consulta la respuesta SAM almacenada para una crisis.
- `GET /crises/patients/{patientId}/analysis`
  - Devuelve analisis clinico agregado para un paciente.
- `GET /crises/patients/{patientId}/export`
  - Exporta eventos de crisis en CSV.

### Dispositivos

- `POST /devices/patients/{patientId}/link`
  - Vincula explicitamente un dispositivo ESP32 con un paciente.

## Endpoints internos

Estos endpoints no usan JWT, pero exigen el header `X-Internal-Token` y estan pensados para integracion entre servicios.

- `POST /internal/telemetry`
  - Recibe telemetria enviada por el servicio de WebSocket y la reinyecta al backend.
- `POST /internal/devices/{deviceId}/disconnected`
  - Notifica que un dispositivo se desconecto.
- `POST /internal/devices/{deviceId}/authenticated`
  - Notifica que un dispositivo se autentico correctamente.
- `POST /internal/devices/validate-token`
  - Valida un token de dispositivo para el servicio interno.

## WebSocket

- `GET /ws/patient-state`
  - Endpoint STOMP para handshake WebSocket.
- Broker: `/topic/**`
  - Canal de publicacion para actualizaciones en tiempo real.
- App prefix: `/app/**`
  - Prefijo de destinos de entrada al backend.

## Notas de uso

- Las rutas protegidas usan `Authorization: Bearer <jwt>`.
- En Swagger UI puedes autenticarte con el boton `Authorize`.
- El token interno se configura con la variable `INTERNAL_TOKEN`.
