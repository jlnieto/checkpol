# Alcance funcional

## Alcance cubierto hoy

### A. Gestion de viviendas

- alta y edicion de vivienda turistica,
- datos basicos de la vivienda,
- datos del titular o responsable.

### B. Gestion de estancias

- crear una estancia manualmente,
- asociarla a una vivienda,
- guardar referencia interna o del canal,
- guardar fechas operativas,
- calcular estado operativo.

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

### H. Admin de municipios

- listado de incidencias abiertas,
- correccion manual,
- aprendizaje de reglas de resolucion.

## Fuera del alcance actual

- OCR de DNI o pasaporte,
- recordatorios automaticos,
- integracion oficial con SES,
- XML de `reserva de hospedaje`,
- enlace individual por huesped,
- uso unico o autocierre del enlace,
- notificaciones o envio automatico del enlace,
- multiusuario,
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
