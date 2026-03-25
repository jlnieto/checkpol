# Alcance funcional

## Vision completa del producto

La vision completa cubre estas areas:

### A. Gestion de viviendas

- alta de vivienda turistica,
- datos del titular o responsable,
- datos basicos de la vivienda,
- posibilidad futura de varias viviendas por usuario.

### B. Gestion de estancias

- crear una estancia o contrato manualmente,
- asociarlo a una vivienda,
- indicar canal de origen si aporta valor operativo,
- guardar numero de contrato o referencia interna,
- guardar fechas de entrada y salida,
- gestionar estado operativo.

### C. Gestion de huespedes

- añadir uno o varios huespedes a una estancia,
- guardar datos obligatorios,
- validar completitud,
- permitir edicion sencilla,
- distinguir origen manual o por enlace,
- permitir revision final interna de datos enviados por enlace.

### D. Generacion SES

- generar XML de parte de viajeros desde una estancia,
- persistir XML o referencia,
- permitir descarga,
- registrar trazabilidad,
- mantener historial versionado.

### E. Vista operativa

- listado de estancias,
- pendientes de comunicar,
- entradas de hoy,
- estancias incompletas,
- estancias con XML generado.

### F. Captura publica inicial

- generar enlace publico por estancia,
- permitir alta publica de huespedes,
- permitir edicion publica de los huespedes enviados por enlace,
- mantener revision interna antes del XML.

## Fuera del alcance actual

- OCR de DNI o pasaporte,
- recordatorios automaticos,
- integracion oficial con SES,
- XML de reserva de hospedaje,
- enlace individual por huesped,
- uso unico o autocierre del enlace,
- notificaciones o envio automatico del enlace,
- multiusuario,
- SaaS multi-tenant,
- integraciones con Airbnb o PMS,
- firma digital o check-in remoto.

## Regla de alcance para fases

Cada fase futura debe cumplir:

- cambio pequeno,
- alcance bien cerrado,
- sistema usable al terminar,
- sin expansion por iniciativa propia.

## Criterio MVP-first

Cuando haya varias opciones de implementacion, elegir la que:

- entregue utilidad antes,
- reduzca pasos para el usuario,
- minimice riesgo tecnico,
- mantenga abierta una evolucion razonable.
