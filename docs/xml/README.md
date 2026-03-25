# XML de SES

## Alcance confirmado

En `checkpol` solo se implementara la generacion del XML de `parte de viajeros`.

Queda fuera de alcance:

- XML de `reserva de hospedaje`
- otras modalidades de carga masiva de SES

## Documentos disponibles en el repositorio

- `docs/instrucciones-crear-xml-hospedaje-ses-v1.2.0.pdf`
  Documento oficial de referencia con reglas, validaciones y tablas de codigos.
- `docs/xml/plantilla-parte-viajeros-ses-v1.2.0.xml`
  Plantilla XML actualmente disponible en el proyecto como referencia estructural.

## Observacion importante

La plantilla XML disponible en el proyecto encaja con `parte de viajeros`:

- usa `codigoEstablecimiento` directamente bajo `solicitud`
- no incluye bloque `establecimiento`
- el `rol` de persona aparece restringido a `VI`

Por tanto, hasta que exista otra plantilla oficial distinta, esta debe tratarse como referencia para `parte de viajeros`.

El archivo se ha renombrado a `plantilla-parte-viajeros-ses-v1.2.0.xml` para reflejar su uso real dentro del proyecto.

## Criterio de implementacion

- No presentar como definitivo ningun campo no confirmado en el PDF oficial.
- Priorizar las validaciones que el PDF explicita para `parte de viajeros`.
- Mantener la generacion XML desacoplada del resto de la aplicacion.
