# Captura de datos por enlace para huespedes

## Objetivo

Esta funcionalidad permite que una estancia tenga un enlace publico para que los huespedes completen o corrijan sus propios datos antes de la revision interna final.

## Motivo

El flujo reduce trabajo manual del gestor:

- reduce transcripcion manual,
- reduce errores,
- reparte parte del esfuerzo con el propio huesped,
- mantiene el control final dentro del backoffice.

## Alcance actual

La implementacion actual incluye:

1. generacion o renovacion de un enlace por estancia,
2. token publico y fecha de caducidad,
3. revocacion manual del enlace,
4. resumen publico del estado del check-in,
5. alta publica de huespedes,
6. edicion publica de huespedes ya creados,
7. wizard guiado en varios pasos,
8. seleccion de direccion existente o creacion de una nueva sin perder el progreso,
9. estado interno de revision para huespedes enviados por enlace,
10. revision final interna antes de generar el XML.

## Flujo publico real

El flujo publico actual se comporta asi:

1. el gestor comparte el enlace de la estancia,
2. el huesped entra al resumen publico,
3. el sistema muestra quien falta y cual es la siguiente accion,
4. el huesped completa sus datos personales, documento, direccion y contacto,
5. si no existe direccion, puede crearla y volver al wizard en el mismo punto,
6. al guardar, vuelve al resumen publico con la ficha ya completada,
7. el gestor revisa internamente antes de generar el XML.

## Restricciones activas

- no permitir acceso publico sin token,
- no exponer listados internos ni otras estancias,
- no generar el XML automaticamente al terminar,
- no asumir identidad verificada del huesped,
- no enviar automaticamente nada al Ministerio.

## Modelo operativo

El responsable final sigue siendo el gestor:

- el huesped aporta datos,
- el sistema los integra en la estancia,
- el estado queda preparado para revision,
- el gestor valida,
- el gestor genera el XML.

## Impacto tecnico actual

Esta funcionalidad ya implica:

- token de acceso por estancia,
- caducidad del acceso,
- vistas publicas separadas,
- origen de carga por huesped,
- estado de revision por huesped,
- wizard de captura con JS propio,
- gestion de direccion dentro del flujo publico,
- resolucion de direcciones y huespedes contra la `Booking` validada por token, sin depender de una sesion autenticada de `OWNER`.

## Limitaciones actuales

- no hay enlace individual por huesped,
- no hay uso unico del enlace,
- no hay cierre automatico por numero esperado de huespedes,
- no hay notificaciones ni envio automatico del enlace,
- no hay verificacion documental avanzada.
