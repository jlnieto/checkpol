# Checkpol

Aplicacion MVP para pequenos propietarios o gestores de viviendas turisticas que necesitan registrar estancias, recoger datos de huespedes y preparar el XML de `parte de viajeros` para carga manual en SES Hospedajes.

## Proposito

`checkpol` no intenta ser un PMS ni automatizar toda la operativa de hospedaje. Su objetivo es resolver bien un problema concreto:

1. registrar una estancia,
2. completar los datos de los huespedes,
3. validar la informacion esencial,
4. generar un XML descargable,
5. mantener trazabilidad operativa,
6. reducir carga manual con un flujo publico de autoservicio para huespedes.

## Estado actual

La aplicacion ya incluye:

- alta y edicion de viviendas,
- alta y edicion de estancias,
- alta y edicion de huespedes desde backoffice,
- wizard interno para alta y edicion de huespedes,
- generacion manual de XML de `parte de viajeros`,
- historial de XML generados por estancia con version y descargas,
- enlace publico por estancia para que los huespedes completen sus datos,
- flujo publico guiado en varios pasos con seleccion o creacion de direccion,
- revision interna de huespedes enviados por enlace,
- panel admin inicial para incidencias de resolucion de municipios.

Limitaciones importantes:

- no existe integracion oficial con SES,
- no se envia nada automaticamente al Ministerio,
- solo se trabaja con la modalidad XML de `parte de viajeros`,
- el enlace publico sigue siendo por estancia, no individual por huesped,
- no hay notificaciones ni envio automatico del enlace,
- la migracion visual a Tailwind aun no esta cerrada en todo `owner`.

## Restricciones clave

- No integrar por ahora con la API oficial de SES.
- No automatizar el envio al Ministerio.
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
- `admin` usa la compilacion compartida de Tailwind,
- `owner` sigue en transicion desde CSS legacy a la base visual nueva.

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
- `docs/development-guidelines.md`
- `docs/roadmap.md`
- `docs/guest-self-service.md`
- `docs/xml/README.md`
- `docs/stitch-mcp-usage.md`
