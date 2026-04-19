# Guia de desarrollo

## Principios generales

- Cambios pequenos y cerrados.
- Implementar solo lo pedido.
- No inventar requisitos.
- Priorizar claridad y mantenibilidad.
- Mantener el enfoque MVP-first.

## Estilo de implementacion

- Preferir soluciones directas y explicitas.
- Evitar capas o interfaces sin necesidad real.
- Mantener nombres consistentes con el lenguaje del producto.
- Dejar la logica de negocio en servicios o dominio cuando aporte claridad.
- No introducir patrones por costumbre.

## UX

La UX debe priorizar a una persona no tecnica.

Criterios:

- formularios claros,
- siguiente accion evidente,
- pocos pasos,
- validaciones utiles,
- textos comprensibles,
- vistas limpias,
- sin ruido visual innecesario.

## Frontend

La base visual objetivo converge hacia una unica compilacion de estilos.

Reglas:

- Tailwind CSS v4 es la base compartida para la evolucion del frontend.
- No usar el CDN de Tailwind en plantillas.
- La entrada compartida de estilos es `src/main/frontend/app.css`.
- El CSS compilado servido por Spring es `src/main/resources/static/app.css`.
- `public`, `owner` y `admin` deben usar esa compilacion compartida.
- Si un patron visual se repite, extraerlo como fragmento Thymeleaf o capa pequena de utilidades compartidas.

Scripts:

- `npm install`
- `npm run build:css`
- `npm run watch:css`

Build:

- Maven recompila la hoja compartida en `generate-resources`.
- `./mvnw test` y `./mvnw clean package` deben seguir pasando con el CSS actualizado.

## JavaScript

No introducir frameworks de frontend pesados para este MVP.

Reglas:

- usar JS propio cuando haya logica real de formulario o wizard,
- evitar dependencias nuevas si no aportan retorno claro,
- no introducir Alpine, React o SPA sin necesidad explicitamente justificada.

## Base de datos

- PostgreSQL es la base objetivo.
- Flyway es obligatorio para todos los cambios de esquema.
- No modificar esquemas manualmente fuera de migraciones.

## Billing y Stripe

La integracion de pagos debe seguir [billing-stripe.md](billing-stripe.md).

Reglas:

- no activar cuentas por redirect de navegador,
- procesar Stripe por webhooks verificados,
- guardar eventos de Stripe de forma idempotente,
- no calcular IVA manualmente en Checkpol,
- no generar facturas PDF propias en el MVP,
- usar Stripe Customer Portal para pago, facturas y datos fiscales,
- mantener el registro publico con el minimo de campos posible.

## Testing

Cada cambio debe dejar una verificacion proporcionada a su alcance.

Preferencias:

- tests MVC para flujos web importantes,
- tests de servicio para reglas de negocio,
- tests de contexto y arranque para la base,
- tests de regresion cuando se corrija un bug operativo real.

Comando base:

- `./mvnw test`

## XML

Si una fase toca generacion XML:

- aislar la generacion en un componente claro,
- definir entradas bien nombradas,
- no fijar como oficial una estructura no verificada,
- implementar solo `parte de viajeros` salvo cambio explicito de alcance.

## Documentacion

Actualizar `docs/` y `README.md` cuando cambie cualquiera de estos puntos:

- alcance funcional real,
- arquitectura,
- frontend compartido,
- flujos publicos o internos,
- reglas de implementacion,
- decisiones relevantes de producto.

La documentacion debe reflejar lo que existe hoy en el codigo, no una mezcla de pasado y futuro.

## Lo que debe evitarse

- SPA al inicio,
- microservicios,
- colas,
- automatizaciones prematuras,
- autenticacion avanzada sin necesidad,
- diseno de un PMS generalista.
