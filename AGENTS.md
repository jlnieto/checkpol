# AGENTS.md

Este archivo define como debe trabajar un agente sobre `checkpol`.

## Objetivo del proyecto

`checkpol` es una aplicacion para pequeños propietarios o gestores de viviendas turisticas que necesitan preparar y, cuando sea posible, presentar la comunicacion obligatoria de huespedes a SES Hospedajes.

El producto debe cubrir dos formas de trabajo:

1. Presentacion automatica por servicio web de SES cuando el owner tenga configuradas sus credenciales.
2. Generacion XML descargable para presentacion manual cuando no las tenga.

Su objetivo es:

1. Registrar una estancia o contrato.
2. Registrar huespedes.
3. Validar datos esenciales.
4. Generar un XML descargable de parte de viajeros.
5. Mantener trazabilidad del XML generado.
6. Presentar automaticamente a SES cuando el owner tenga configurado el servicio web.
7. Permitir una primera captura publica de huespedes por enlace con revision interna.

## Regla principal

Trabajar siempre con enfoque MVP-first.

- Implementar solo el alcance pedido en la fase actual.
- No anticipar funcionalidades futuras sin necesidad real.
- No convertir la aplicacion en un PMS.
- No introducir complejidad tecnica si no aporta valor inmediato.

## Restricciones no negociables

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
- definir una interfaz o puerto para presentar por servicio web,
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
- `docs/stitch-mcp-usage.md`

## Uso de Stitch

Para tareas de UI/UX usar Stitch por defecto, pero con estas reglas operativas:

- consultar `docs/stitch-mcp-usage.md` antes de llamadas no triviales,
- no asumir que los valores de ejemplo del MCP son exactamente los aceptados por backend,
- para `deviceType`, preferir `MOBILE` o `DESKTOP` en mayusculas,
- si `generate_screen_from_text` falla, reintentar sin `deviceType`,
- no depender de `list_screens` como unica fuente de verdad,
- usar `edit_screens` como alternativa a `generate_variants` si este falla con `invalid argument`.

### Checklist rapido de Stitch

Antes de una tarea de diseno:

1. Crear o identificar `projectId`.
2. Empezar por una llamada minima a `generate_screen_from_text`.
3. Usar `deviceType` en mayusculas o no enviarlo.
4. Guardar siempre `projectId`, `screenId` y `name` devueltos.
5. Recuperar pantallas con `get_screen` si `list_screens` no devuelve nada.
6. Para variantes A/B, preferir dos llamadas separadas a `edit_screens` si `generate_variants` falla.
7. Si Stitch sigue fallando, explicarlo y continuar con la mejor alternativa local.

### Ejemplos seguros

- `generate_screen_from_text(projectId, prompt)` con prompt corto y claro.
- `generate_screen_from_text(projectId, prompt, deviceType=\"MOBILE\")`
- `edit_screens(projectId, selectedScreenIds, prompt, deviceType=\"MOBILE\")`

### Casos a evitar

- `deviceType` en minusculas (`mobile`, `desktop`) porque ha fallado en esta sesion.
- depender de `list_screens` para descubrir pantallas recien creadas.
- depender de `generate_variants` sin una prueba minima previa.

## Criterio de decision

Ante una duda, elegir siempre la opcion que:

1. mantenga el MVP pequeno,
2. deje el sistema usable,
3. no bloquee una evolucion futura razonable,
4. reduzca complejidad innecesaria.
