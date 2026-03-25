# Checkpol

Aplicacion MVP para gestionar estancias y huespedes de viviendas turisticas y preparar el XML de parte de viajeros para carga manual en SES Hospedajes.

## Proposito

El producto esta orientado a pequeños propietarios o gestores de viviendas turisticas que necesitan reducir el trabajo manual de la comunicacion obligatoria de huespedes.

Objetivo del MVP:

1. registrar una estancia o contrato,
2. registrar huespedes,
3. validar datos esenciales,
4. generar un XML descargable de parte de viajeros,
5. permitir su carga manual en SES,
6. ofrecer una primera captura publica de datos de huespedes por enlace con revision interna final.

## Estado actual

La aplicacion ya incluye:

- alta y edicion de viviendas,
- alta y edicion de estancias,
- alta y edicion de huespedes,
- validaciones operativas para preparar el parte de viajeros,
- listado de estancias con filtros operativos,
- generacion manual de XML de parte de viajeros,
- historial de XML generados con version, fecha y descargas,
- enlace publico por estancia para que los huespedes añadan o editen sus datos,
- revision interna de huespedes enviados por enlace antes de generar el XML.

Limitaciones actuales importantes:

- no existe integracion oficial con SES,
- no se envia nada automaticamente al Ministerio,
- el enlace publico es por estancia, no individual por huesped,
- no hay uso unico del enlace ni cierre automatico por numero de huespedes,
- no hay notificaciones ni envio automatico del enlace.

## Restricciones clave

- No integrar por ahora con la API oficial de SES.
- No automatizar el envio al Ministerio.
- No inventar el formato oficial del XML como si fuera definitivo.
- No implementar la modalidad XML de reserva de hospedaje.
- No construir una SPA ni un PMS completo.
- No introducir complejidad tecnica innecesaria.

## Stack

- Java 21
- Spring Boot 3.x
- Spring MVC
- Thymeleaf
- PostgreSQL
- Flyway

## Arranque local

Forma recomendada, igual que en `whatsappbot`:

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

Si todavia no has dado de alta `checkpol` en `dev`, puedes arrancarlo manualmente:

1. Levanta el stack local:

```bash
docker compose up -d
```

2. Ejecuta tests:

```bash
./mvnw test
```

3. Si necesitas compilar fuera de Docker:

```bash
./mvnw clean package
```

`checkpol` ya incluye `mvnw`, igual que `whatsappbot`, para no depender de la version de Maven instalada en la maquina.

## Estructura

```text
checkpol/
  AGENTS.md
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

## Arquitectura

Se usa un monolito clasico en la raiz del repositorio. No hay separacion `backend/frontend` porque en este MVP las vistas Thymeleaf y la logica MVC viven dentro de la misma aplicacion Spring Boot.

Paquetes previstos:

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
- `ops/dev-java-projects/checkpol.conf.example`
