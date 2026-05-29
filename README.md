# neuro-live-backend

## Public API

- Production URL: `neurolive-backend.azurewebsites.net`
- Health endpoint: `https://neurolive-backend.azurewebsites.net/health`

## Participacion del equipo

### Ana Maria Ruales

**Rol:** Analisis, diseno, frontend e IoT.

Ana Maria se encargo del analisis de necesidades del usuario, el diseno de la experiencia de uso, el desarrollo del frontend y la implementacion del componente IoT del proyecto. Su participacion abarco distintas etapas del proceso de construccion, desde la definicion de requisitos hasta la integracion de los componentes funcionales del sistema.

En la fase de diseno, realizo una revision de referentes especializados en accesibilidad, usabilidad y diseno orientado a personas neurodivergentes. A partir de este analisis, definio decisiones visuales relacionadas con la paleta cromatica, la tipografia, la jerarquia de informacion y la densidad de los elementos en pantalla, procurando que la interfaz respondiera a las caracteristicas cognitivas y sensoriales de la poblacion objetivo. El diseno tomo como referencia los criterios de accesibilidad WCAG AA.

Asimismo, desarrollo el frontend mediante Next.js y TypeScript, integrando las vistas principales de la aplicacion y su comunicacion con los servicios del sistema. Tambien asumio la responsabilidad del componente de hardware e IoT, incluyendo la integracion fisica del microcontrolador ESP32 con el sensor biometrico MAX30102, el modulo de iluminacion RGB WS2812 y el reproductor de audio DFPlayer Mini. Esta labor incluyo la programacion del firmware cargado en el microcontrolador, desarrollado mediante Arduino IDE.

### Santiago Nicolas Saavedra

**Rol:** Backend, infraestructura, inteligencia artificial y servidor WebSocket.

Santiago Nicolas Saavedra se encargo del diseno e implementacion de la arquitectura backend del sistema, la infraestructura de despliegue, la integracion de inteligencia artificial y el desarrollo del servidor WebSocket. El backend fue construido con Spring Boot e incluyo servicios REST, autenticacion basada en JWT, gestion de usuarios por roles, control de acceso a la informacion clinica, vinculacion entre pacientes, cuidadores y profesionales de salud, y organizacion de la logica de negocio mediante patrones de diseno de software.

Adicionalmente, implemento componentes asociados al procesamiento de telemetria biometrica, la deteccion y gestion de eventos de crisis, el registro de respuestas SAM, la generacion de analiticas clinicas y la persistencia de informacion relevante para el seguimiento del paciente. Tambien participo en la estructuracion de la base de datos, el manejo de migraciones, la validacion de entidades y la definicion de pruebas unitarias e integracion para verificar el comportamiento del backend.

En el componente de comunicacion en tiempo real, desarrollo el servidor WebSocket en Python con FastAPI, encargado de actuar como puente entre el dispositivo ESP32 y el backend principal. Este servicio permite recibir informacion del dispositivo, validar su comunicacion y reenviar la telemetria al sistema central. Finalmente, apoyo el despliegue de los servicios en la nube y la configuracion de infraestructura necesaria para la integracion entre frontend, backend, base de datos y servicio WebSocket.
