# Modelo de dominio

## Entidades principales

### Accommodation

Representa una vivienda turistica.

Responsabilidades esperadas:

- identificar la vivienda,
- guardar datos basicos,
- guardar datos del titular o responsable,
- servir como contenedor de estancias.

## Booking o Reservation

Representa una estancia o contrato interno.

Responsabilidades esperadas:

- asociarse a una vivienda,
- guardar identificador de contrato o referencia interna,
- guardar canal de origen,
- guardar fechas de entrada y salida,
- reflejar estado operativo,
- mantener, cuando exista, un enlace publico temporal para captura de huespedes.

## Guest

Representa un huesped asociado a una estancia.

Responsabilidades esperadas:

- guardar los datos necesarios para el parte de viajeros,
- permitir validacion de completitud,
- permitir edicion sencilla,
- reflejar si se cargo manualmente o desde enlace,
- reflejar si requiere revision interna.

## GeneratedCommunication o Submission

Representa una comunicacion generada.

Responsabilidades esperadas:

- asociarse a una estancia,
- guardar momento de generacion,
- guardar contenido XML o una referencia estable,
- guardar version por estancia,
- registrar descargas realizadas.

## Relaciones conceptuales

- Una `Accommodation` puede tener muchas `Booking`.
- Una `Booking` pertenece a una `Accommodation`.
- Una `Booking` puede tener muchos `Guest`.
- Un `GeneratedCommunication` pertenece a una `Booking`.

## Vocabularios controlados

Es razonable prever enums para:

- `BookingChannel`: `AIRBNB`, `BOOKING`, `DIRECT`, `OTHER`.
- `DocumentType`: segun necesidad real y datos confirmados.
- `BookingOperationalStatus`: `WAITING_GUESTS`, `REVIEW_PENDING`, `INCOMPLETE`, `READY_FOR_XML`, `XML_GENERATED`.
- `GuestSubmissionSource`: `MANUAL`, `SELF_SERVICE`.
- `GuestReviewStatus`: `PENDING_REVIEW`, `REVIEWED`.

Estos vocabularios ya existen porque el sistema los usa en la operativa actual.

## Reglas de modelado

- Nombres del dominio claros y consistentes.
- No introducir entidades futuras aun no usadas.
- Evitar value objects si no clarifican realmente.
- Mantener las validaciones esenciales cerca del dominio o del caso de uso.

## Regla critica sobre datos SES

El modelo debe prepararse para alimentar un generador XML, pero no debe asumir campos o estructuras que no hayan sido confirmados para el formato oficial.

En este proyecto, el formato confirmado a implementar es el de `parte de viajeros`.
La estructura de `reserva de hospedaje` no forma parte del alcance.

Si una fase requiere un modelo provisional:

- marcarlo de forma explicita,
- aislarlo de la implementacion definitiva,
- no presentarlo como version oficial.
