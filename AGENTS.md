# AGENTS.md

Este archivo define como debe trabajar un agente sobre `checkpol`.

## Objetivo del proyecto

`checkpol` es una aplicacion para pequeños propietarios o gestores de viviendas turisticas que necesitan preparar la comunicacion obligatoria de huespedes a SES Hospedajes.

El MVP no envia nada al Ministerio. Su objetivo es:

1. Registrar una estancia o contrato.
2. Registrar huespedes.
3. Validar datos esenciales.
4. Generar un XML descargable de parte de viajeros para carga manual en SES.
5. Mantener trazabilidad del XML generado.
6. Permitir una primera captura publica de huespedes por enlace con revision interna.

## Regla principal

Trabajar siempre con enfoque MVP-first.

- Implementar solo el alcance pedido en la fase actual.
- No anticipar funcionalidades futuras sin necesidad real.
- No convertir la aplicacion en un PMS.
- No introducir complejidad tecnica si no aporta valor inmediato.

## Restricciones no negociables

- No integrar la API oficial de SES por ahora.
- No automatizar el envio al Ministerio.
- No inventar el formato XML oficial como si fuera definitivo.
- No implementar el XML de reserva de hospedaje.
- No anadir SPA ni frontend complejo.
- No introducir microservicios, colas ni arquitectura distribuida.
- No anadir autenticacion avanzada en las primeras fases.
- No introducir multi-tenant ni multiusuario salvo que una fase futura lo pida.

## Stack objetivo

- Java 21
- Spring Boot 3.x
- Spring MVC
- Thymeleaf
- PostgreSQL
- Flyway
- Bean Validation

## Arranque local

Para este proyecto, no usar IntelliJ como forma principal de arranque.

Usar por defecto:

- `dev up checkpol`
- `dev stop checkpol`
- `dev status`
- `./mvnw test`

Si `checkpol` todavia no esta dado de alta en `dev`, usar temporalmente:

- `docker compose up -d`
- `./mvnw test`
- `./mvnw clean package`

## Arquitectura esperada

Aplicacion monolitica clasica en la raiz del repositorio.

Paquetes previstos:

- `es.checkpol.web`: controladores MVC, formularios y vistas.
- `es.checkpol.service`: casos de uso y coordinacion.
- `es.checkpol.domain`: entidades, reglas, enums y modelos del dominio.
- `es.checkpol.repository`: persistencia.
- `es.checkpol.infrastructure`: adaptadores tecnicos, incluyendo generacion XML y otros componentes externos.

## Modelo mental del dominio

- `Accommodation`: vivienda turistica.
- `Booking` o `Reservation`: estancia o contrato interno asociado a una vivienda.
- `Guest`: huesped asociado a una estancia, con origen de carga y estado de revision.
- `GeneratedCommunication` o `Submission`: XML de parte de viajeros generado para una estancia, con trazabilidad y version.

## Regla para el XML

El XML es una capacidad clave del producto, pero su formato oficial no debe asumirse si no esta verificado.

En este proyecto solo se implementara la modalidad de `parte de viajeros`.
No se implementara la modalidad de `reserva de hospedaje`.

Se permite:

- definir una interfaz o puerto para generar XML,
- definir un modelo de datos de entrada al generador,
- crear una implementacion provisional claramente marcada cuando una fase lo requiera.

No se permite:

- presentar una estructura inventada como si fuera el formato oficial definitivo.

## UX esperada

El usuario principal es no tecnico. La interfaz debe ser:

- simple,
- directa,
- limpia,
- con pocos pasos,
- con validaciones comprensibles.

## Forma de implementar

- Cambios pequenos y cerrados.
- Nombres claros.
- Validaciones cerca del dominio o del caso de uso cuando tenga sentido.
- Vistas funcionales y sobrias.
- Sin abstracciones innecesarias.
- Sin patrones introducidos por moda.

## Documentacion a mantener

Si una fase cambia el producto o la arquitectura de forma relevante, actualizar la documentacion afectada en `docs/`.

Los archivos clave son:

- `README.md`
- `docs/product-vision.md`
- `docs/architecture.md`
- `docs/domain-model.md`
- `docs/development-guidelines.md`
- `docs/roadmap.md`
- `docs/guest-self-service.md`
- `docs/xml/README.md`

## Criterio de decision

Ante una duda, elegir siempre la opcion que:

1. mantenga el MVP pequeno,
2. deje el sistema usable,
3. no bloquee una evolucion futura razonable,
4. reduzca complejidad innecesaria.
