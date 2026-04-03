# Roadmap de alto nivel

## Objetivo

Construir el producto por incrementos pequenos, dejando siempre algo usable y sin ensanchar el alcance por inercia.

## Fases ya materializadas

### Base tecnica

- proyecto Spring Boot arrancable,
- estructura de capas clara,
- migraciones con Flyway,
- tests base.

### Operativa interna

- alta y edicion de viviendas,
- alta y edicion de estancias,
- alta y edicion de huespedes,
- estados operativos de estancias,
- detalle de estancia con acciones principales.

### XML

- generacion manual de XML de `parte de viajeros`,
- persistencia del XML generado,
- versionado por estancia,
- historial y descargas.

### Autoservicio publico

- enlace por estancia con token y caducidad,
- resumen publico de progreso,
- alta y edicion publica de huespedes,
- wizard guiado de varios pasos,
- seleccion o creacion de direccion sin perder el contexto,
- revision interna final antes del XML.

## Fases razonables siguientes

### 1. Cierre visual del frontend compartido

- consolidar la base visual compartida de `public`, `owner` y `admin`,
- simplificar naming, fragmentos y utilidades para reducir deuda visual.

### 2. Mejora operativa del autoservicio

- reforzar mensajes y ayudas por paso,
- ajustar edge cases del wizard,
- mejorar consistencia movil entre vistas publicas relacionadas.

### 3. Mayor control del enlace publico

- enlace individual por huesped,
- cierre por numero esperado de huespedes,
- reglas mas estrictas de ciclo de vida del acceso.

### 4. Capas de ayuda al gestor

- mejoras en revision interna,
- mejoras en validacion y cobertura del catálogo nacional de municipios,
- mejor soporte a datos dudosos o incompletos.

### 5. Endurecimiento del area administrativa

- gestion de contraseñas y estado de usuarios,
- trazabilidad de operaciones administrativas,
- consolidar el módulo administrativo de `municipalities`,
- vigilar y endurecer la compatibilidad con futuras publicaciones oficiales del callejero del INE.

## Fuera del roadmap inmediato

- integracion oficial con SES,
- envio automatico al Ministerio,
- OCR de documentos,
- recordatorios automaticos,
- integraciones con PMS,
- SaaS multi-tenant.

## Regla del roadmap

El roadmap orienta, pero no justifica implementar nada fuera del alcance concreto pedido.

Cada fase debe:

- dejar algo usable,
- tener cierre funcional claro,
- mantener el MVP pequeno,
- no introducir complejidad estructural que hoy no haga falta.
