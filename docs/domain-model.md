# Modelo de dominio

## Entidades principales

### AppUser

Representa una persona que puede iniciar sesión en la aplicación.

Responsabilidades:

- autenticar acceso persistido,
- distinguir roles `SUPER_ADMIN` y `OWNER`,
- actuar como propietario de viviendas y estancias,
- aislar los datos operativos internos por usuario.

### Accommodation

Representa una vivienda turistica.

Responsabilidades:

- pertenecer a un `AppUser`,
- identificar la vivienda,
- guardar datos basicos y del titular,
- actuar como contenedor de estancias.

### Booking

Representa una estancia o contrato interno.

Responsabilidades:

- pertenecer a un `AppUser`,
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
- guardar `municipalityCode` y `municipalityName` ya validados cuando el pais es España.

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

### MunicipalityCatalogEntry

Representa un municipio canónico del catálogo local.

Responsabilidades:

- guardar codigo oficial de municipio,
- guardar provincia y nombre oficial,
- servir como fuente local de validacion para direcciones españolas.

### PostalCodeMunicipalityMapping

Representa la relacion entre un codigo postal y uno o varios municipios canonicos.

Responsabilidades:

- soportar seleccion local por codigo postal,
- permitir que un codigo postal apunte a varios municipios,
- desacoplar el dato maestro del estado operativo guardado en `Address`.

### MunicipalityImportRecord

Representa una importacion administrativa del catálogo municipal.

Responsabilidades:

- guardar origen y versión de cada carga,
- registrar quién la ejecutó,
- reflejar si terminó bien o falló,
- mantener contadores básicos de municipios y mappings afectados,
- dejar trazabilidad operativa del área `/admin/municipalities`.

## Relaciones conceptuales

- Un `AppUser` puede tener muchas `Accommodation`.
- Un `AppUser` puede tener muchas `Booking`.
- Una `Accommodation` puede tener muchas `Booking`.
- Una `Booking` pertenece a una `Accommodation`.
- Una `Booking` puede tener muchas `Guest`.
- Una `Booking` puede tener muchas `Address`.
- Un `Guest` pertenece a una `Booking`.
- Un `Guest` referencia una `Address`.
- Un `GeneratedCommunication` pertenece a una `Booking`.
- Un `SelfServiceAccess` pertenece a una `Booking`.

## Vocabularios controlados

El sistema ya trabaja con enums y estados operativos como:

- `AppUserRole`
- `BookingChannel`
- `BookingOperationalStatus`
- `DocumentType`
- `GuestSubmissionSource`
- `GuestReviewStatus`
- `GuestSex`

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

## Regla de acceso actual

- `SUPER_ADMIN` accede al área administrativa global.
- `OWNER` accede solo a sus viviendas, estancias, huéspedes y XML.
- El sistema no es multi-tenant; el aislamiento actual es por usuario.
