# Alcance funcional

## Alcance cubierto hoy

### A. Gestion de viviendas

- alta y edicion de vivienda turistica,
- aislamiento de viviendas por usuario propietario,
- datos basicos de la vivienda,
- datos del titular o responsable.

### B. Gestion de estancias

- crear una estancia manualmente,
- aislarla por usuario propietario,
- asociarla a una vivienda,
- guardar referencia interna o del canal,
- guardar fechas operativas,
- calcular estado operativo.

### B2. Acceso y administracion

- login persistido con usuarios en base de datos,
- rol `SUPER_ADMIN` para administración global,
- rol `OWNER` para operativa diaria,
- área administrativa para crear y mantener usuarios propietarios.

### C. Gestion de huespedes

- anadir varios huespedes a una estancia,
- editar huespedes existentes,
- validar datos operativos,
- distinguir origen manual o por enlace,
- permitir revision final interna.

### D. Direcciones

- direcciones separadas del huesped,
- seleccion desde wizard,
- creacion de nueva direccion desde el flujo interno o publico.

### E. Generacion SES

- generar XML de `parte de viajeros`,
- persistir versiones,
- descargar XML ya generado,
- registrar trazabilidad de descargas.

### F. Vista operativa

- listado de estancias,
- detalle de estancia,
- pendientes de completar,
- estado de revision,
- estado de XML generado.

### G. Captura publica

- enlace publico por estancia,
- resumen publico de progreso,
- alta y edicion publica de huespedes,
- creacion de direccion dentro del flujo,
- revision interna antes del XML.
- validacion de direcciones españolas con catálogo local de municipios.

## Fuera del alcance actual

- OCR de DNI o pasaporte,
- recordatorios automaticos,
- integracion oficial con SES,
- XML de `reserva de hospedaje`,
- enlace individual por huesped,
- uso unico o autocierre del enlace,
- notificaciones o envio automatico del enlace,
- SaaS multi-tenant,
- integraciones con Airbnb o PMS,
- firma digital o check-in remoto.

## Regla de alcance para futuras fases

Cada fase debe cumplir:

- cambio pequeno,
- alcance bien cerrado,
- sistema usable al terminar,
- sin expansion por iniciativa propia.

## Criterio MVP-first

Cuando haya varias opciones, elegir la que:

- entregue utilidad antes,
- reduzca pasos al usuario,
- minimice riesgo tecnico,
- mantenga abierta una evolucion razonable.
