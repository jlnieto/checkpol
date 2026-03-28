# XML de SES

## Alcance confirmado

En `checkpol` solo se implementa la generacion del XML de `parte de viajeros`.

Queda fuera de alcance:

- XML de `reserva de hospedaje`
- otras modalidades de carga masiva de SES
- envio automatico a la plataforma oficial

## Estado actual en el producto

El sistema ya permite:

- generar manualmente el XML desde una estancia,
- persistir cada XML generado,
- versionar el XML por estancia,
- descargar versiones anteriores,
- registrar descargas y ultima fecha de descarga.

## Documentos disponibles en el repositorio

- `docs/instrucciones-crear-xml-hospedaje-ses-v1.2.0.pdf`
  Documento oficial de referencia con reglas, validaciones y tablas de codigos.
- `docs/xml/plantilla-parte-viajeros-ses-v1.2.0.xml`
  Plantilla XML usada como referencia estructural para `parte de viajeros`.

## Observacion importante

La plantilla XML disponible en el proyecto encaja con `parte de viajeros`:

- usa `codigoEstablecimiento` directamente bajo `solicitud`,
- no incluye bloque `establecimiento`,
- el `rol` de persona aparece restringido a `VI`.

Por tanto, mientras no exista otra referencia oficial distinta, esta plantilla debe tratarse como base de trabajo para `parte de viajeros`.

## Criterio de implementacion

- No presentar como definitivo ningun campo no confirmado en el PDF oficial.
- Priorizar las validaciones que el PDF explicita para `parte de viajeros`.
- Mantener la generacion XML desacoplada del resto de la aplicacion.
- No introducir soporte para `reserva de hospedaje` sin cambio explicito de alcance.
