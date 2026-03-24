# Despliegue en Azure App Service

Este backend ya esta preparado para desplegarse como `Java SE` en `Azure App Service` usando el plugin `azure-webapp-maven-plugin` que existe en `pom.xml`.

## Arquitectura recomendada

- Backend: Azure App Service (Linux, Java 21, Java SE)
- Base de datos: Azure Database for PostgreSQL Flexible Server
- Secretos y configuracion: App Settings del App Service

## Variables necesarias en Azure

Configura estas variables en `App Service > Settings > Environment variables`:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `PORT` opcional, si defines un comando de arranque personalizado

Ejemplo de `SPRING_DATASOURCE_URL`:

```text
jdbc:postgresql://<tu-servidor>.postgres.database.azure.com:5432/neurolive?sslmode=require
```

## Comandos de despliegue

1. Inicia sesion en Azure CLI:

```powershell
az login
az account set --subscription "<tu-subscription-id-o-nombre>"
```

2. Genera el artefacto:

```powershell
.\mvnw.cmd clean package
```

3. Despliega:

```powershell
.\mvnw.cmd azure-webapp:deploy
```

Tambien puedes hacerlo en un solo paso:

```powershell
.\mvnw.cmd package azure-webapp:deploy
```

## Configuracion actual del proyecto

En `pom.xml` ya quedaron definidos estos recursos:

- `resourceGroup`: `rg-neurolive`
- `appName`: `neurolive-backend`
- `pricingTier`: `B1`
- `region`: `canadacentral`
- `runtime`: Linux + Java 21 + Java SE

Si alguno de esos nombres no existe en tu suscripcion, cambialo antes de desplegar.

## Verificaciones despues del deploy

- Salud publica: `https://<app-name>.azurewebsites.net/health`
- Salud actuator: `https://<app-name>.azurewebsites.net/actuator/health`

## Problemas comunes

### 1. La app sube pero no arranca

Revisa:

- que `SPRING_DATASOURCE_URL` apunte a Azure PostgreSQL
- que el firewall de PostgreSQL permita conexiones desde Azure
- que `JWT_SECRET` tenga suficiente longitud para HMAC
- que el plan y la region coincidan con lo configurado en `pom.xml`

### 2. Error de conexion a PostgreSQL

Para Azure Database for PostgreSQL normalmente necesitas:

- host `*.postgres.database.azure.com`
- puerto `5432`
- `sslmode=require`

### 3. El despliegue falla por Maven Wrapper

Se corrigio `mvnw.cmd` para que funcione mejor en Windows cuando `.m2` no es un enlace simbolico.

### 4. La app no escucha en el puerto esperado

`application.properties` ahora usa:

```properties
server.port=${PORT:8080}
```

Eso permite respetar un puerto inyectado por Azure o seguir usando `8080` localmente.
