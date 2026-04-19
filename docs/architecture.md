# Arquitectura

## Decision principal

`checkpol` se construye como un monolito clasico Spring Boot.

No existe separacion `backend/frontend` porque el producto usa:

- Spring MVC,
- Thymeleaf,
- recursos estaticos simples,
- JavaScript puntual para formularios guiados.

Esta decision reduce complejidad, acelera el MVP y encaja con un producto de operativa sencilla para escritorio y movil.

## Stack

- Java 21
- Spring Boot 3.x
- Spring MVC
- Thymeleaf
- PostgreSQL
- Flyway
- Bean Validation
- Tailwind CSS v4

## Estructura del repositorio

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

## Estructura de paquetes

```text
es.checkpol
  web
  service
  domain
  repository
  infrastructure
```

### `web`

Responsabilidades:

- controladores MVC,
- formularios,
- binding y validacion web,
- vistas Thymeleaf,
- flujos internos y publicos.

### `service`

Responsabilidades:

- coordinar casos de uso,
- aplicar reglas de aplicacion,
- orquestar repositorios y adaptadores,
- resolver autenticacion persistida y acceso al usuario actual,
- gestionar el ciclo de vida operativo de estancias,
- gestionar autoservicio publico y revision,
- validar direcciones españolas contra el catálogo local de municipios.
- orquestar la descarga, previsualización e importación administrativa del catálogo global de municipalities.

### `domain`

Responsabilidades:

- entidades,
- enums,
- reglas del dominio,
- usuarios persistidos y roles de acceso,
- trazabilidad de origen y revision de huespedes,
- versionado de XML generado,
- catalogo local de municipios y datos operativos de direccion.

### `repository`

Responsabilidades:

- acceso a persistencia con Spring Data JPA,
- consultas operativas por estado y revision,
- soporte a catálogo local de municipios y mappings por código postal.

### `infrastructure`

Responsabilidades:

- generacion XML,
- adaptadores tecnicos,
- integraciones externas futuras,
- adaptadores de billing externos como Stripe cuando se implemente el cobro.

## Billing y pagos

El cobro recurrente se implementara con Stripe, manteniendo Checkpol como monolito.

Principios:

- Checkpol no debe generar facturas propias en el MVP.
- Stripe debe recoger datos de pago y facturacion.
- Stripe Tax debe calcular impuestos con precio inclusivo cuando aplique.
- La activacion de cuentas debe depender de webhooks, no del redirect del navegador.
- Checkpol debe guardar un espejo interno de suscripciones, facturas y eventos para acceso, soporte y diagnostico.
- La declaracion OSS no debe implementarse dentro de Checkpol.

Documento de referencia:

- [billing-stripe.md](billing-stripe.md)

## Frontend

El frontend vive dentro del mismo monolito.

Estado actual:

- `public` usa la compilacion compartida de Tailwind,
- `owner` y `admin` usan la misma compilacion compartida,
- la base visual activa queda concentrada en `src/main/frontend/app.css`.

Piezas relevantes:

- entrada de estilos: `src/main/frontend/app.css`
- CSS compilado: `src/main/resources/static/app.css`
- JS del wizard: `src/main/resources/static/wizard-form.js`

No se usa SPA ni framework de frontend pesado.

## Persistencia

La base de datos objetivo es PostgreSQL y se versiona con Flyway.

La aplicacion ya persiste, entre otras cosas:

- usuarios y roles,
- viviendas,
- estancias,
- huespedes,
- direcciones separadas de huespedes,
- XML generados con versionado,
- acceso publico por token,
- catalogo local de municipios y relacion codigo postal -> municipio.
- histórico de importaciones administrativas del catálogo municipal.

Los datos operativos internos se aislan por usuario propietario. No hay modelo multi-tenant; el aislamiento actual es por `AppUser`.

En tests se usa una base embebida para arrancar el contexto sin depender del entorno local.

## XML de SES

La generacion XML debe seguir desacoplada del resto del sistema.

En este proyecto solo se implementa `parte de viajeros`.

El sistema ya persiste cada XML generado asociado a una estancia, con:

- version incremental por estancia,
- fecha de generacion,
- contador de descargas,
- fecha de ultima descarga.

## Enlace publico para huespedes

El flujo publico ya es una parte estructural del producto:

- el gestor genera o renueva un enlace por estancia,
- el enlace tiene token y caducidad,
- el huesped puede crear o editar sus datos,
- puede seleccionar una direccion existente o crear una nueva sin romper el wizard,
- la resolucion publica de direcciones y huespedes se hace contra la `Booking` validada por token, sin reutilizar la sesion autenticada del `OWNER`,
- los datos enviados quedan sujetos a revision interna antes del XML.

## Catálogo de municipios

Las direcciones de España se validan directamente contra una base local canónica:

- catálogo de municipios con código oficial,
- relación código postal -> municipio,
- selección guiada en formularios internos y públicos.

Para España el flujo operativo es único: código postal, selección de municipio del catálogo y almacenamiento directo del código oficial.

Ese `municipalityCode` queda persistido en `Address` y es el valor que se exporta como `codigoMunicipio` en el XML de `parte de viajeros`.

La actualización del catálogo ya no depende solo de recursos empaquetados:

- el `SUPER_ADMIN` dispone de `/admin/municipalities`,
- la aplicación descarga el fichero oficial de municipios del INE por URL,
- descarga el ZIP oficial del callejero por URL y deriva el mapping postal,
- valida ambos datasets,
- muestra una previsualización,
- y solo después importa a base de datos con trazabilidad de la operación.

## Principios arquitectonicos

- Mantener capas claras.
- Evitar abstracciones prematuras.
- No crear modulos o servicios separados sin necesidad.
- Mantener aislada la generacion XML.
- Mantener el MVP pequeno, usable y evolutivo.
- Preferir aislamiento por usuario antes que introducir multi-tenant prematuro.

## Lo que no se debe hacer

- No crear una API independiente y un frontend SPA por defecto.
- No anadir microservicios, colas o eventos sin necesidad real.
- No disenar ya para multi-tenant.
- No sobredisenar la seguridad mas alla del aislamiento por usuario y el `SUPER_ADMIN`.
