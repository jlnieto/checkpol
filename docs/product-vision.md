# Vision de producto

## Problema que resolvemos

Pequenos propietarios y gestores de viviendas turisticas en Espana tienen que recoger datos de huespedes y preparar la comunicacion obligatoria a SES Hospedajes. Hoy ese trabajo suele ser manual, repetitivo y propenso a errores.

`checkpol` busca reducir esa carga administrativa sin obligar al usuario a entender XML, normativa o flujos tecnicos complejos.

## Usuario principal

Usuario objetivo inicial:

- pequeno propietario o gestor de una o pocas viviendas,
- no tecnico,
- orientado a rapidez y simplicidad,
- con poca tolerancia a burocracia o interfaces recargadas.

## Resultado ideal

El flujo ideal del producto es:

1. crear o abrir una estancia,
2. completar los datos de los huespedes,
3. revisar que no falta nada esencial,
4. presentar la comunicacion en SES si el owner tiene configurado su servicio web,
5. o, si no lo tiene, generar y descargar el XML de `parte de viajeros`,
6. mantener una trazabilidad clara de lo enviado o descargado.

## Propuesta de valor

El producto no compite como PMS completo. Su valor esta en resolver bien un trabajo muy concreto:

- guardar la informacion necesaria,
- validarla con criterio operativo,
- permitir una primera captura publica por enlace,
- mantener revision interna cuando haga falta,
- preparar el XML correcto,
- presentarlo automaticamente cuando el owner tenga WS,
- dejar trazabilidad de lo generado o enviado.

## Estado real del MVP

Hoy el MVP ya cubre:

- acceso persistido con usuarios reales,
- área administrativa básica para crear y mantener propietarios,
- gestion de viviendas,
- gestion de estancias,
- gestion manual de huespedes,
- estados operativos de estancias,
- generacion de XML de `parte de viajeros`,
- presentacion automatica opcional por servicio web para `parte de viajeros`,
- historial versionado de XML,
- enlace publico por estancia,
- alta y edicion publica de huespedes,
- creacion de direccion desde el flujo publico,
- revision interna de huespedes enviados por enlace,
- validacion de direcciones españolas contra un catálogo local de municipios y codigos postales.

## Alcance XML confirmado

La unica modalidad XML objetivo del producto es `parte de viajeros`.

Queda fuera de alcance:

- XML de `reserva de hospedaje`,
- otras modalidades de carga masiva de SES distintas de `parte de viajeros`.

## Flujo publico implementado

El autoservicio publico ya no es una prueba minima. Ahora cubre un flujo guiado:

- acceso por token asociado a la estancia,
- resumen publico del estado del check-in,
- alta publica de huespedes por pasos,
- edicion publica de huespedes ya creados,
- seleccion de direccion existente o creacion de una nueva sin perder el progreso,
- cierre del flujo con confirmacion y vuelta al resumen,
- revision final interna antes de generar el XML.

## Fuera del MVP actual

Estas lineas pueden explorarse mas adelante, pero no deben condicionar el diseno actual:

- OCR de documentos,
- recordatorios automaticos,
- enlace individual por huesped,
- uso unico o autocierre del enlace,
- notificaciones,
- check-in remoto,
- integraciones con Airbnb o PMS,
- SaaS multi-tenant.
