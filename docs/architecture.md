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
- gestionar el ciclo de vida operativo de estancias,
- gestionar autoservicio publico y revision,
- resolver y revisar municipios cuando haga falta.

### `domain`

Responsabilidades:

- entidades,
- enums,
- reglas del dominio,
- trazabilidad de origen y revision de huespedes,
- versionado de XML generado,
- estado de resolucion de municipio en direcciones.

### `repository`

Responsabilidades:

- acceso a persistencia con Spring Data JPA,
- consultas operativas por estado y revision,
- soporte a incidencias y reglas aprendidas.

### `infrastructure`

Responsabilidades:

- generacion XML,
- adaptadores tecnicos,
- integraciones externas futuras,
- lookup tecnico de municipios cuando aplique.

## Frontend

El frontend vive dentro del mismo monolito.

Estado actual:

- `public` y `admin` usan la compilacion compartida de Tailwind,
- `owner` sigue parcialmente en CSS legacy,
- existe una transicion controlada hacia una unica base visual.

Piezas relevantes:

- entrada de estilos: `src/main/frontend/app.css`
- CSS compilado: `src/main/resources/static/app.css`
- JS del wizard: `src/main/resources/static/wizard-form.js`

No se usa SPA ni framework de frontend pesado.

## Persistencia

La base de datos objetivo es PostgreSQL y se versiona con Flyway.

La aplicacion ya persiste, entre otras cosas:

- viviendas,
- estancias,
- huespedes,
- direcciones separadas de huespedes,
- XML generados con versionado,
- acceso publico por token,
- incidencias y reglas de resolucion de municipios.

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
- los datos enviados quedan sujetos a revision interna antes del XML.

## Admin de municipios

Existe un area admin inicial para resolver incidencias de municipio:

- lista de incidencias abiertas,
- correccion manual de municipio,
- aprendizaje de reglas futuras a partir de la correccion.

No es un backoffice generalista; es una herramienta puntual para no bloquear el flujo.

## Principios arquitectonicos

- Mantener capas claras.
- Evitar abstracciones prematuras.
- No crear modulos o servicios separados sin necesidad.
- Mantener aislada la generacion XML.
- Mantener el MVP pequeno, usable y evolutivo.

## Lo que no se debe hacer

- No crear una API independiente y un frontend SPA por defecto.
- No anadir microservicios, colas o eventos sin necesidad real.
- No disenar ya para multi-tenant.
- No meter seguridad avanzada antes de que una fase lo requiera.
