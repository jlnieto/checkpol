# Checkpol

Aplicacion para pequenos propietarios o gestores de viviendas turisticas que necesitan registrar estancias, recoger datos de huespedes y preparar o presentar la comunicacion de `parte de viajeros` en SES Hospedajes.

## Proposito

`checkpol` no intenta ser un PMS ni automatizar toda la operativa de hospedaje. Su objetivo es resolver bien un problema concreto:

1. registrar una estancia,
2. completar los datos de los huespedes,
3. validar la informacion esencial,
4. generar un XML descargable,
5. presentar automaticamente por WS cuando el owner tenga configurado SES,
6. mantener trazabilidad operativa,
7. reducir carga manual con un flujo publico de autoservicio para huespedes.

## Estado actual

La aplicacion ya incluye:

- login persistido con usuarios en base de datos,
- área administrativa para gestionar usuarios propietarios,
- área administrativa para gestionar el catálogo global de `municipalities`,
- alta y edicion de viviendas,
- alta y edicion de estancias,
- alta y edicion de huespedes desde backoffice,
- wizard interno para alta y edicion de huespedes,
- generacion de XML de `parte de viajeros`,
- historial de XML generados por estancia con version y descargas,
- presentacion automatica opcional por servicio web de SES,
- enlace publico por estancia para que los huespedes completen sus datos,
- flujo publico guiado en varios pasos con seleccion o creacion de direccion,
- revision interna de huespedes enviados por enlace,
- catálogo local de municipios de España y relacion codigo postal -> municipio para validar direcciones españolas,
- registro publico de propietarios en `/registro`,
- pago recurrente mensual con Stripe Checkout embebido,
- activacion de cuentas `OWNER` por webhook confirmado de Stripe,
- limite de viviendas segun cantidad contratada,
- pantalla owner de facturacion en `/bookings/billing` con acceso a Stripe Customer Portal.

Limitaciones importantes:

- solo se trabaja con la modalidad XML de `parte de viajeros`,
- el enlace publico sigue siendo por estancia, no individual por huesped,
- no hay notificaciones ni envio automatico del enlace,
- los datos se aislan por usuario, pero no existe modelo multi-tenant ni gestion de equipos,
- la carga administrativa del catálogo municipal depende de que las URLs oficiales sigan manteniendo el formato esperado,
- el area admin de billing todavia no esta implementada,
- la operacion comercial con Stripe requiere configurar producto, Tax, portal y webhook antes de vender,
- la migracion visual a Tailwind aun no esta cerrada en todo `owner`.

## Registro y billing con Stripe

La primera vertical de cobro esta integrada como parte del monolito:

- `/registro` crea un alta pendiente y lanza Stripe Checkout embebido,
- `/webhooks/stripe` verifica la firma de Stripe y procesa eventos de checkout, suscripcion y factura,
- el `AppUser` con rol `OWNER` se crea solo cuando el webhook confirma una suscripcion utilizable,
- `paid_accommodation_limit` limita las viviendas que puede crear el owner,
- `/bookings/billing` muestra estado, viviendas contratadas, datos fiscales resumidos y ultimas facturas,
- `POST /bookings/billing/portal` abre Stripe Customer Portal.

Variables de entorno soportadas:

- `CHECKPOL_BILLING_STRIPE_SECRET_KEY`
- `CHECKPOL_BILLING_STRIPE_PUBLISHABLE_KEY`
- `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`
- `CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `CHECKPOL_PUBLIC_BASE_URL`
- `CHECKPOL_BILLING_STRIPE_CUSTOMER_PORTAL_CONFIGURATION_ID`
- `CHECKPOL_BILLING_SIGNUP_EXPIRATION_MINUTES`
- `CHECKPOL_BILLING_GRACE_PERIOD_DAYS`

Guia de producto, operacion y pruebas:

- [docs/billing-stripe.md](/home/jose/IdeaProjects/checkpol/docs/billing-stripe.md)

## SES por servicio web

Para presentar automaticamente en SES no basta con usuario y clave del WS. La JVM debe confiar en el certificado remoto mediante un truststore configurado por entorno.

Variables de entorno soportadas:

- `CHECKPOL_SES_WS_URL`
- `CHECKPOL_SES_WS_APPLICATION_NAME`
- `CHECKPOL_SES_WS_TRUSTSTORE_PATH`
- `CHECKPOL_SES_WS_TRUSTSTORE_PASSWORD`
- `CHECKPOL_SES_WS_TRUSTSTORE_TYPE`

Recomendacion operativa:

- `dev`: usar `pre-ses` y un truststore de pruebas
- `prod`: usar `ses` y un truststore de produccion
- no guardar truststores ni claves en el repositorio

Guia rapida local:

- [docs/ses-ws-truststore-setup.md](/home/jose/IdeaProjects/checkpol/docs/ses-ws-truststore-setup.md)

## Restricciones clave

- No presentar como definitivo un formato XML no verificado.
- No implementar la modalidad XML de `reserva de hospedaje`.
- No convertir el producto en un PMS ni en una SPA compleja.
- No introducir complejidad tecnica sin retorno claro para el MVP.

## Stack

- Java 21
- Spring Boot 3.x
- Spring MVC
- Thymeleaf
- PostgreSQL
- Flyway
- Bean Validation
- Tailwind CSS v4 para la base visual compartida

## Frontend

La interfaz se sirve desde el mismo monolito Spring Boot.

Estado actual del frontend:

- `public` usa la compilacion compartida de Tailwind,
- `owner` y `admin` usan la misma compilacion compartida servida como `app.css`.

Archivos principales de estilo:

- entrada Tailwind: `src/main/frontend/app.css`
- CSS compilado servido por Spring: `src/main/resources/static/app.css`
- JS del wizard: `src/main/resources/static/wizard-form.js`

Scripts disponibles:

```bash
npm install
npm run build:css
npm run watch:css
```

El build Maven recompila `src/main/resources/static/app.css` automaticamente en la fase `generate-resources`, asi que `./mvnw test` y `./mvnw clean package` ya validan tambien la hoja compartida.

## Arranque local

Forma recomendada:

```bash
dev up checkpol
```

Comandos relacionados:

```bash
dev stop checkpol
dev status
dev logs checkpol
dev url checkpol
```

Si `checkpol` todavia no esta dado de alta en `dev`, puedes arrancarlo manualmente:

```bash
docker compose up -d
./mvnw test
```

Si necesitas compilar fuera de Docker:

```bash
./mvnw clean package
```

Si vienes de una base local creada antes de la simplificación del subsistema de municipios o antes de introducir usuarios persistidos y ownership por usuario, recrea la base o limpia el historial de Flyway local antes de arrancar. Estos cambios reescriben parte de la historia de municipios y añaden `owner_user_id` obligatorio en viviendas y estancias.

## Administración

El `SUPER_ADMIN` tiene dos áreas principales:

- `/admin/users` para crear y mantener usuarios `OWNER`,
- `/admin/municipalities` para descargar, validar, previsualizar e importar el catálogo municipal global.

La home `/admin` también muestra el estado resumido de salud de las fuentes oficiales para que el `SUPER_ADMIN` vea si hay que revisar `municipalities`.

El módulo de `municipalities` funciona así:

1. descarga el fichero oficial de municipios del INE desde la URL indicada,
2. transforma ese XLSX al CSV interno de la aplicación,
3. descarga el ZIP oficial del callejero y deriva de ahí el mapping postal,
4. permite verificar las fuentes oficiales sin tocar la BD,
5. guarda el resultado de esa verificación y muestra un estado visible en la pantalla,
6. valida ambos datasets,
7. muestra una previsualización,
8. y solo después importa a base de datos.

Opcionalmente puede ejecutarse una verificación programada:

- `CHECKPOL_MUNICIPALITY_ADMIN_VERIFICATION_ENABLED=true`
- `CHECKPOL_MUNICIPALITY_ADMIN_VERIFICATION_CRON=0 0 6 * * *`
- `CHECKPOL_MUNICIPALITY_ADMIN_VERIFICATION_ZONE=Europe/Madrid`

Compatibilidad adicional:

- si hace falta, el mapping postal remoto también puede venir ya como CSV `postalCode;municipalityCode` o como ZIP con un único CSV de ese formato.

## Direcciones y XML

Para direcciones de España, el flujo operativo actual es:

1. el formulario consulta municipios válidos por `postalCode`,
2. el usuario selecciona un `municipalityCode` oficial,
3. la aplicación persiste ese `municipalityCode` en `Address`,
4. y el XML usa ese código persistido como `codigoMunicipio`.

Para países distintos de `ESP`, la dirección guarda `municipalityName` como texto libre y el XML exporta `nombreMunicipio`.

El flujo público por enlace no requiere sesión autenticada de `OWNER`: la resolución de direcciones y huéspedes se hace contra la `Booking` validada por token.

## Estructura

```text
checkpol/
  AGENTS.md
  README.md
  package.json
  docs/
  ops/
  pom.xml
  src/
    main/
      frontend/
      java/es/checkpol/
      resources/
        application.yml
        db/migration/
        static/
        templates/
    test/
      java/es/checkpol/
```

## Arquitectura

Se usa un monolito clasico en la raiz del repositorio. No hay frontend separado.

Paquetes principales:

- `es.checkpol.web`
- `es.checkpol.service`
- `es.checkpol.domain`
- `es.checkpol.repository`
- `es.checkpol.infrastructure`

## Documentacion

Documentacion principal del proyecto:

- `AGENTS.md`
- `docs/product-vision.md`
- `docs/architecture.md`
- `docs/domain-model.md`
- `docs/mvp-scope.md`
- `docs/billing-stripe.md`
- `docs/development-guidelines.md`
- `docs/roadmap.md`
- `docs/municipality-catalog.md`
- `docs/guest-self-service.md`
- `docs/xml/README.md`
- `docs/stitch-mcp-usage.md`
