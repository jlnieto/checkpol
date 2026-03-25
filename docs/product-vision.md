# Vision de producto

## Problema que resolvemos

El producto nace para resolver un problema real de pequeños propietarios de viviendas turisticas en Espana: registrar huespedes y preparar la comunicacion obligatoria a SES Hospedajes de una forma mas simple, mas rapida y con menos errores.

Hoy ese trabajo suele ser manual, incomodo y propenso a fallos. El objetivo del producto es reducir al minimo esa carga administrativa.

## Usuario principal

Usuario inicial:

- pequeño propietario o gestor de una o pocas viviendas,
- no tecnico,
- centrado en rapidez y simplicidad,
- sin interes en entender XML ni requisitos burocraticos.

Usuario real de referencia:

- el padre del promotor del producto.

## Resultado ideal

El flujo objetivo para el usuario es:

1. entrar en la aplicacion,
2. ver o crear una estancia,
3. completar los datos de los huespedes,
4. validar que no falta nada esencial,
5. pulsar un boton para generar el XML de parte de viajeros,
6. descargar el XML,
7. subirlo manualmente a SES.

## Propuesta de valor

No se busca construir un PMS completo ni una plataforma compleja. Se busca resolver muy bien un problema concreto:

- guardar la informacion necesaria,
- validarla correctamente,
- preparar un XML de parte de viajeros para carga masiva,
- dejar trazabilidad operativa,
- reducir parte de la carga manual mediante captura publica inicial de datos.

## Objetivo del MVP

Entregar valor real desde fases tempranas con una herramienta util y sencilla.

El MVP debe:

- ser usable,
- tener alcance pequeno,
- evitar sobreingenieria,
- permitir evolucion controlada.

## Alcance XML confirmado

La unica modalidad XML objetivo del MVP es `parte de viajeros`.

Queda fuera del alcance:

- XML de reserva de hospedaje,
- otras modalidades de carga masiva de SES.

## Flujo publico ya implementado

El producto ya incluye una primera implementacion para que el huesped complete sus datos mediante un enlace externo asociado a una estancia.

Ese flujo:

- usa token y fecha de caducidad,
- permite alta y edicion publica de los huespedes enviados por enlace,
- mantiene revision final interna antes de generar el XML.

Todavia no cubre variantes mas avanzadas como enlace individual por huesped, uso unico o notificaciones.

## Fuera del MVP inicial

Estas lineas de evolucion son posibles, pero no deben condicionar el diseno inicial mas alla de dejar puntos de extension razonables:

- OCR de documentos,
- recordatorios automaticos,
- integracion oficial,
- check-in remoto,
- integraciones con Airbnb o PMS,
- multiusuario,
- multi-tenant SaaS.
