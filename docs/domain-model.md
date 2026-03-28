# Modelo de dominio

## Entidades principales

### Accommodation

Representa una vivienda turistica.

Responsabilidades:

- identificar la vivienda,
- guardar datos basicos y del titular,
- actuar como contenedor de estancias.

### Booking

Representa una estancia o contrato interno.

Responsabilidades:

- asociarse a una vivienda,
- guardar referencia interna o del canal,
- guardar fechas de reserva, entrada y salida,
- reflejar estado operativo,
- mantener, cuando exista, un acceso publico temporal para el autoservicio de huespedes.

### Guest

Representa un huesped asociado a una estancia.

Responsabilidades:

- guardar los datos necesarios para el parte de viajeros,
- reflejar origen de captura,
- reflejar estado de revision,
- permitir alta y edicion,
- apuntar a una direccion habitual,
- participar en la evaluacion de completitud de la estancia.

### Address

Representa la direccion habitual de residencia de una persona.

Responsabilidades:

- desacoplar la direccion del resto del formulario de huesped,
- permitir reutilizacion dentro de una misma estancia,
- guardar informacion necesaria para municipio, codigo postal y pais,
- mantener datos de resolucion automatica o manual de municipio.

### GeneratedCommunication

Representa una comunicacion generada en XML.

Responsabilidades:

- asociarse a una estancia,
- guardar momento de generacion,
- guardar version por estancia,
- registrar descargas realizadas,
- mantener trazabilidad del fichero generado.

### SelfServiceAccess

Representa el acceso publico por token a una estancia.

Responsabilidades:

- exponer un token publico por estancia,
- controlar caducidad,
- permitir regeneracion o revocacion,
- separar el flujo publico del area interna.

### MunicipalityResolutionIssue

Representa una incidencia de resolucion de municipio que requiere seguimiento.

Responsabilidades:

- registrar una asignacion automatica que necesita revision,
- dejar trazabilidad del texto original y del municipio asignado,
- permitir correccion desde administracion.

### MunicipalityResolutionRule

Representa una regla aprendida por correccion manual.

Responsabilidades:

- recordar correcciones ya aceptadas,
- reutilizar esas correcciones en futuras resoluciones similares,
- reducir trabajo manual repetido.

## Relaciones conceptuales

- Una `Accommodation` puede tener muchas `Booking`.
- Una `Booking` pertenece a una `Accommodation`.
- Una `Booking` puede tener muchas `Guest`.
- Una `Booking` puede tener muchas `Address`.
- Un `Guest` pertenece a una `Booking`.
- Un `Guest` referencia una `Address`.
- Un `GeneratedCommunication` pertenece a una `Booking`.
- Un `SelfServiceAccess` pertenece a una `Booking`.
- Una `MunicipalityResolutionIssue` pertenece a un `Guest`.

## Vocabularios controlados

El sistema ya trabaja con enums y estados operativos como:

- `BookingChannel`
- `BookingOperationalStatus`
- `DocumentType`
- `GuestSubmissionSource`
- `GuestReviewStatus`
- `GuestSex`
- `MunicipalityResolutionStatus`
- `MunicipalityIssueStatus`

## Reglas de modelado

- Nombres de dominio claros y consistentes.
- No introducir entidades futuras aun no usadas.
- Mantener validaciones esenciales cerca del dominio o del caso de uso.
- Evitar modelar como definitivo lo que dependa de reglas SES no confirmadas.

## Regla critica sobre datos SES

El modelo debe servir para alimentar un generador XML, pero no debe inventar estructuras no verificadas.

En este proyecto:

- la modalidad implementada es `parte de viajeros`,
- `reserva de hospedaje` sigue fuera de alcance,
- cualquier ampliacion de modelo debe hacerse sin presentar como oficial un formato no confirmado.
