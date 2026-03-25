# Captura de datos por enlace para huespedes

## Objetivo

Esta funcionalidad permite que uno o varios huespedes completen sus propios datos mediante un enlace enviado por el gestor o propietario.

## Motivo de esta funcionalidad

Aunque el MVP sigue apoyandose en revision y generacion interna, este flujo reduce trabajo manual:

- reduce trabajo manual del gestor,
- reduce errores de transcripcion,
- acelera la preparacion del parte de viajeros.

## Alcance actual

La implementacion actual incluye:

1. generacion o renovacion de un enlace por estancia,
2. formulario publico limitado a la captura de datos del huesped,
3. caducidad temporal del enlace,
4. revocacion manual del enlace por parte del gestor,
5. alta publica de huespedes,
6. edicion publica de los huespedes creados desde ese enlace,
7. estado interno de revision pendiente para huespedes enviados por enlace,
8. revision final por parte del gestor antes de generar el XML.

## Restricciones recomendadas

- no permitir acceso publico sin token,
- no exponer listados internos ni otras estancias,
- no generar el XML automaticamente al completar el formulario,
- no asumir identidad verificada del huesped sin un paso adicional.

## Modelo de trabajo recomendado

La aplicacion deberia seguir considerando al gestor como responsable final del envio:

- el huesped rellena datos,
- el sistema marca la informacion como pendiente de revision o completada,
- el gestor revisa,
- el gestor genera el XML de parte de viajeros.

## Impacto tecnico actual

Esta funcionalidad ya ha requerido añadir:

- token de acceso por estancia,
- vistas publicas separadas,
- origen de carga por huesped,
- estado de revision por huesped,
- caducidad del acceso,
- trazabilidad basica de cambios introducidos por terceros.

## Estado actual

Existe una primera implementacion funcional:

- el gestor puede generar o renovar un enlace por estancia,
- el gestor puede revocar el enlace cuando quiera,
- el enlace usa token y fecha de caducidad,
- el huesped puede añadir y editar sus datos desde un formulario publico,
- la revision final y la generacion del XML siguen siendo internas.

Limitaciones de esta primera version:

- no hay enlace individual por huesped,
- no hay uso unico ni bloqueo tras completar un numero concreto de huespedes,
- no hay notificaciones ni envio automatico del enlace.
