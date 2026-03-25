# Arquitectura

## Decision principal

La aplicacion se construye como un monolito clasico Spring Boot.

No existe separacion `backend/frontend` porque el producto usa:

- Spring MVC,
- Thymeleaf,
- recursos estaticos simples.

Esta decision reduce complejidad, acelera el MVP y encaja con una UX sencilla para escritorio y movil.

## Stack

- Java 21
- Spring Boot 3.x
- Spring MVC
- Thymeleaf
- PostgreSQL
- Flyway
- Bean Validation

## Estructura del repositorio

```text
checkpol/
  AGENTS.md
  README.md
  docs/
  ops/
  pom.xml
  src/
    main/
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

La organizacion base prevista es:

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
- mapeo web basico,
- vistas Thymeleaf.

### `service`

Responsabilidades:

- coordinar casos de uso,
- aplicar reglas de aplicacion,
- orquestar repositorios y adaptadores,
- calcular estado operativo de estancias,
- gestionar revision interna de datos cargados por enlace.

### `domain`

Responsabilidades:

- entidades,
- value objects si aportan claridad,
- enums,
- reglas del dominio,
- trazabilidad de origen y revision de huespedes,
- versionado de XML generado.

### `repository`

Responsabilidades:

- acceso a persistencia,
- repositorios JPA o adaptadores equivalentes.

### `infrastructure`

Responsabilidades:

- adaptadores tecnicos,
- generacion XML de parte de viajeros,
- configuracion externa,
- integraciones futuras cuando existan.

## Principios arquitectonicos

- Mantener capas claras.
- Evitar abstracciones prematuras.
- No crear modulos o servicios separados sin necesidad.
- Mantener aislada la logica de generacion XML.
- Permitir evolucion futura sin pagar coste ahora.

## Persistencia

La base de datos objetivo es PostgreSQL.

Se usara Flyway para:

- versionar esquema,
- dejar trazabilidad de cambios,
- mantener despliegues repetibles.

En tests puede usarse una base embebida para arrancar el contexto sin depender del entorno local.

## XML de SES

La generacion XML debe estar desacoplada del resto del sistema.

Para este proyecto, la unica modalidad XML objetivo es `parte de viajeros`.
No se implementara la modalidad `reserva de hospedaje`.

Diseno recomendado cuando toque implementarlo:

1. modelo de entrada para la generacion,
2. interfaz del generador,
3. implementacion concreta.

Esto permite cambiar la implementacion si se verifica el formato oficial o aparece una integracion distinta.

Actualmente el sistema ya persiste cada XML generado asociado a una estancia, con:

- version incremental por estancia,
- fecha de generacion,
- contador de descargas,
- fecha de ultima descarga.

## Enlace publico para huespedes

Existe una primera implementacion separada del backoffice interno:

- el gestor genera o revoca un enlace por estancia,
- el enlace tiene token y caducidad,
- el huesped puede crear o editar sus propios datos en una vista publica,
- los huespedes creados por este flujo quedan pendientes de revision interna.

## Lo que no se debe hacer

- No crear una API independiente y un frontend SPA por defecto.
- No anadir eventos, colas o microservicios.
- No disenar ya para multi-tenant.
- No meter seguridad avanzada antes de que una fase lo requiera.
