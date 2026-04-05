# XML de SES

## Alcance confirmado

En `checkpol` solo se implementa la modalidad `parte de viajeros`.

La presentacion puede hacerse de dos formas:

- por servicio web de SES cuando el owner tenga configurado su acceso,
- por descarga manual del XML cuando no lo tenga.

Queda fuera de alcance:

- XML de `reserva de hospedaje`
- otras modalidades de carga masiva de SES

## Estado actual en el producto

El sistema ya permite:

- generar el XML desde una estancia,
- persistir cada XML generado,
- versionar el XML por estancia,
- descargar versiones anteriores,
- registrar descargas y ultima fecha de descarga,
- preparar la base para presentar `parte de viajeros` por servicio web.

Para el modo WS:

- la URL de SES es configurable por entorno,
- el cliente usa un truststore propio si se configura `CHECKPOL_SES_WS_TRUSTSTORE_PATH`,
- ese truststore no debe depender del almacén global de la máquina.

En direcciones:

- si el país es `ESP`, el XML usa `codigoMunicipio` a partir del `municipalityCode` persistido en `Address`,
- si el país no es `ESP`, el XML usa `nombreMunicipio` a partir del `municipalityName` guardado en la dirección.

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
- Mantener desacoplada la presentacion por servicio web para poder usar o no esa via segun la configuracion del owner.
- No introducir soporte para `reserva de hospedaje` sin cambio explicito de alcance.
