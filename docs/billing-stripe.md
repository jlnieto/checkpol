# Billing, registro y Stripe

## Objetivo

Convertir Checkpol en un SaaS de pago sin romper el enfoque MVP-first.

El sistema debe permitir:

- registro publico de nuevos propietarios,
- pago recurrente mensual con Stripe,
- contratacion por numero de alojamientos,
- facturacion con datos fiscales recogidos por Stripe,
- control interno fiable de suscripciones, facturas y eventos,
- soporte a clientes particulares, autonomos y empresas,
- gestion fiscal para ventas B2C UE mediante OSS cuando aplique.

La prioridad no es construir un sistema de billing propio. La prioridad es integrar Stripe de forma controlada y mantener en Checkpol un espejo interno suficiente para soporte, acceso y diagnostico.

## Decisiones cerradas

### Precio

Precio publico inicial:

- `3,90 EUR / mes / alojamiento`
- impuestos incluidos cuando aplique
- sin prueba gratuita

El precio debe configurarse en Stripe como precio recurrente mensual con `tax_behavior=inclusive`.

Implicacion:

- un particular en Espana paga `3,90 EUR` total,
- si aplica IVA, Stripe desglosa el impuesto dentro de esos `3,90 EUR`,
- una empresa/autonomo con VAT ID intracomunitario valido puede quedar en reverse charge y pagar `3,90 EUR` sin IVA.

### Alcance comercial

El producto se vende por alojamiento contratado.

Ejemplos:

- 1 alojamiento: `3,90 EUR / mes`
- 2 alojamientos: `7,80 EUR / mes`
- 5 alojamientos: `19,50 EUR / mes`

No crear planes separados por numero de alojamientos. Usar cantidad de suscripcion (`quantity`) sobre el mismo precio de Stripe.

### Cliente objetivo inicial

Publico inicial:

- Espana,
- particulares,
- autonomos,
- empresas pequenas,
- propietarios o gestores que necesitan cumplir el tramite de huespedes sin entender tecnicismos.

La landing y el registro deben hablar de tranquilidad, reserva, huespedes, alojamiento y tramite. Evitar vender XML, SES o servicio web como mensaje principal.

### Fiscalidad

La empresa gestora sera una LLC de Estados Unidos.

Decision prevista:

- registrar la LLC en Irlanda para OSS,
- usar el esquema OSS que corresponda para ventas B2C UE,
- validar con asesor fiscal si aplica `Non-Union OSS` por no estar la LLC establecida en la UE,
- si en el futuro existe entidad o establecimiento fijo en Irlanda, revisar si cambia a `Union OSS` u otro tratamiento.

Regla operativa inicial:

- B2C UE: IVA del pais del consumidor, declarado por OSS cuando aplique.
- B2B UE con VAT ID valido: reverse charge cuando Stripe Tax lo determine.
- Cliente que dice ser empresa/autonomo pero no aporta VAT ID valido: tratar como B2C hasta revision.

Checkpol no debe calcular manualmente el IVA ni decidir reverse charge con logica propia salvo para mostrar estados internos. Stripe Tax debe ser la fuente principal de calculo fiscal.

## Registro minimo

El formulario propio de Checkpol debe pedir solo:

1. Email.
2. Contrasena.
3. Numero de alojamientos a contratar.

No pedir en Checkpol:

- nombre fiscal,
- direccion fiscal,
- NIF/CIF/VAT ID,
- datos de tarjeta,
- telefono,
- datos de vivienda,
- credenciales SES.

Esos datos se recogen despues, cuando correspondan:

- facturacion y pago: Stripe,
- vivienda: onboarding interno,
- SES: configuracion interna opcional.

## Flujo de alta

### Paso 1. Formulario de registro

Ruta propuesta:

- `GET /registro`
- `POST /registro`

Pantalla:

- email,
- contrasena,
- selector de alojamientos,
- resumen de precio,
- boton `Continuar al pago`.

Copy recomendado:

- titulo: `Empieza con tu primera vivienda`
- resumen: `Elige cuantos alojamientos quieres cubrir. El pago es mensual.`
- precio: `3,90 EUR al mes por alojamiento`
- nota: `Impuestos incluidos cuando aplique`

### Paso 2. Pending signup

Al enviar el formulario:

1. Validar email unico.
2. Validar contrasena.
3. Validar cantidad de alojamientos.
4. Crear `pending_signup`.
5. Crear o asociar `Stripe Customer`.
6. Crear `Checkout Session` embebida.
7. Mostrar checkout dentro de la pagina.

La cuenta no queda activa en este punto.

### Paso 3. Checkout embebido

Stripe debe recoger:

- metodo de pago,
- nombre o razon social,
- direccion de facturacion,
- pais,
- codigo postal,
- Tax ID/VAT ID si el cliente lo aporta,
- consentimiento o datos que Stripe requiera para suscripciones.

Configuracion esperada de Checkout:

- `mode=subscription`
- `ui_mode=embedded`
- `line_items[0].price = STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `line_items[0].quantity = accommodation_quantity`
- `automatic_tax.enabled = true`
- `tax_id_collection.enabled = true`
- `billing_address_collection = required`
- metadata:
  - `pendingSignupId`
  - `product=checkpol_esencial`
  - `accommodationQuantity`

### Paso 4. Activacion por webhook

No activar nunca la cuenta solo por el redirect del navegador.

La cuenta se activa cuando los webhooks confirman que:

- el checkout se completo,
- la suscripcion existe,
- el estado permite acceso.

Al activar:

1. Crear `AppUser` con rol `OWNER`.
2. Crear `billing_account`.
3. Marcar `pending_signup` como `COMPLETED`.
4. Establecer `paid_accommodation_limit`.
5. Permitir login.

### Paso 5. Pantalla post-pago

Si el usuario vuelve del pago:

- si el webhook ya proceso: mostrar `Cuenta activada` y enlace para entrar,
- si aun no proceso: mostrar `Estamos confirmando el pago` y permitir refrescar,
- si fallo: mostrar accion para volver al pago o contactar soporte.

## Modelo de datos propuesto

### `pending_signup`

Campos:

- `id`
- `email`
- `password_hash`
- `accommodation_quantity`
- `status`
- `stripe_customer_id`
- `stripe_checkout_session_id`
- `expires_at`
- `created_at`
- `updated_at`

Estados:

- `PENDING_PAYMENT`
- `CHECKOUT_CREATED`
- `COMPLETED`
- `EXPIRED`
- `FAILED`

Notas:

- no crear `AppUser` hasta completar pago,
- evitar duplicados por email,
- expirar registros abandonados.

### `billing_account`

Campos:

- `id`
- `owner_user_id`
- `stripe_customer_id`
- `stripe_subscription_id`
- `stripe_subscription_item_id`
- `status`
- `paid_accommodation_limit`
- `current_period_start`
- `current_period_end`
- `cancel_at_period_end`
- `customer_country`
- `customer_type`
- `tax_mode`
- `created_at`
- `updated_at`

Estados internos:

- `ACTIVE`
- `PAST_DUE`
- `UNPAID`
- `CANCEL_AT_PERIOD_END`
- `CANCELED`
- `INCOMPLETE`
- `INCOMPLETE_EXPIRED`
- `SUSPENDED`

`customer_type` propuesto:

- `INDIVIDUAL`
- `BUSINESS_REVERSE_CHARGE`
- `BUSINESS_WITHOUT_VALID_VAT_ID`
- `UNKNOWN`

`tax_mode` propuesto:

- `OSS_B2C`
- `EU_B2B_REVERSE_CHARGE`
- `NO_TAX_COLLECTED`
- `MANUAL_REVIEW`
- `UNKNOWN`

### `billing_invoice`

Campos:

- `id`
- `billing_account_id`
- `stripe_invoice_id`
- `stripe_invoice_number`
- `status`
- `total_amount`
- `currency`
- `tax_amount`
- `tax_country`
- `tax_behavior`
- `hosted_invoice_url`
- `invoice_pdf_url`
- `period_start`
- `period_end`
- `created_at`
- `updated_at`

Objetivo:

- soporte interno,
- contraste con Stripe,
- diagnostico de pagos fallidos,
- acceso rapido a factura PDF o pagina alojada.

### `stripe_event_log`

Campos:

- `id`
- `stripe_event_id`
- `event_type`
- `processing_status`
- `processed_at`
- `error_message`
- `payload`
- `created_at`

Reglas:

- `stripe_event_id` debe ser unico,
- todos los webhooks se guardan antes de procesarse,
- el procesamiento debe ser idempotente,
- si falla, debe quedar visible para revision.

## Webhooks obligatorios

Endpoint:

- `POST /webhooks/stripe`

Eventos minimos:

- `checkout.session.completed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.finalized`
- `invoice.paid`
- `invoice.payment_failed`
- `invoice.voided`
- `invoice.marked_uncollectible`
- `customer.updated`

Reglas de procesamiento:

- verificar firma con `STRIPE_WEBHOOK_SECRET`,
- guardar evento,
- ignorar eventos ya procesados,
- no asumir orden perfecto,
- poder reintentar eventos fallidos,
- no borrar payloads necesarios para diagnostico sin una politica explicita.

## Reglas de acceso por estado

### `ACTIVE`

Acceso normal.

### `PAST_DUE`

Permitir acceso con aviso visible:

`No hemos podido cobrar la renovacion. Actualiza el metodo de pago.`

### `UNPAID`

Permitir entrar solo para resolver billing y consultar informacion minima.

Bloquear:

- crear estancias,
- generar XML,
- presentar en SES,
- crear alojamientos.

### `CANCEL_AT_PERIOD_END`

Permitir acceso normal hasta `current_period_end`.

Mostrar aviso:

`Tu suscripcion se cancelara al final del periodo.`

### `CANCELED`

Bloquear operativa.

Permitir:

- acceso a pantalla de billing,
- reactivacion si se implementa,
- contacto con soporte.

### `INCOMPLETE` / `INCOMPLETE_EXPIRED`

No activar cuenta.

## Limite de alojamientos

Regla:

- `paid_accommodation_limit` viene de la cantidad de la suscripcion,
- alojamientos activos no pueden superar ese limite.

Si el propietario intenta crear otro alojamiento:

`Tu plan cubre 1 alojamiento. Anade otro alojamiento para crear esta vivienda.`

Accion:

- `Gestionar plan`

Primera version:

- usar Stripe Customer Portal si permite actualizar cantidad de forma adecuada.

Version posterior si hace falta:

- crear flujo propio para aumentar/disminuir cantidad.

Regla recomendada:

- aumentar cantidad: cobrar prorrateo inmediato,
- disminuir cantidad: aplicar al siguiente ciclo.

## Portal de cliente

No construir gestion de facturas propia al inicio.

Crear pantalla:

- `GET /bookings/billing`

Mostrar:

- estado de suscripcion,
- alojamientos contratados,
- proxima renovacion,
- ultimo pago,
- boton `Gestionar pago y facturas`.

El boton abre Stripe Customer Portal.

Funciones delegadas en Stripe:

- actualizar tarjeta,
- actualizar datos de facturacion,
- descargar facturas,
- cancelar suscripcion si se habilita,
- cambiar cantidad si se configura y funciona bien.

## Facturacion

La factura la emite la LLC.

Stripe genera:

- invoice,
- hosted invoice page,
- invoice PDF,
- emails de facturacion si se activan.

Checkpol no debe generar PDFs propios en el MVP.

Configuracion necesaria en Stripe:

- nombre legal de la LLC,
- direccion legal/fiscal,
- email de soporte,
- web,
- tax IDs de la empresa si aplica,
- footer o campos legales si el asesor fiscal lo pide,
- plantilla de factura.

Datos del cliente:

- se recogen en Stripe Checkout o Customer Portal,
- no se duplican manualmente en Checkpol salvo IDs, estado y resumen necesario.

## OSS y fiscalidad

La decision prevista es registrar la LLC en Irlanda para OSS.

Pendiente de validar con asesor fiscal:

- si corresponde `Non-Union OSS`,
- obligaciones exactas por vender SaaS/digital services a consumidores UE,
- tratamiento de clientes B2B con VAT ID valido,
- textos legales de factura,
- conservacion de registros,
- reporting trimestral.

Checkpol debe guardar suficiente informacion para contraste:

- pais fiscal del cliente,
- tipo de cliente,
- tax mode,
- invoice id,
- total,
- impuesto,
- periodo,
- invoice PDF/url,
- estado de suscripcion.

No implementar declaracion OSS dentro de Checkpol.

Operacion prevista:

1. Stripe Tax calcula.
2. Stripe genera reportes.
3. Asesor fiscal presenta OSS en Irlanda.
4. Checkpol conserva espejo interno para soporte y contraste.

## Configuracion requerida

Variables:

- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `CHECKPOL_PUBLIC_BASE_URL`

Opcionales futuras:

- `STRIPE_CUSTOMER_PORTAL_CONFIGURATION_ID`
- `CHECKPOL_BILLING_SIGNUP_EXPIRATION_MINUTES`
- `CHECKPOL_BILLING_GRACE_PERIOD_DAYS`

## Pantallas necesarias

Publicas:

- `/registro`
- `/registro/pago`
- `/registro/confirmando`
- `/registro/completado`
- `/registro/error`

Owner:

- `/bookings/billing`

Admin:

- `/admin/billing`
- `/admin/billing/events`
- `/admin/billing/invoices`

Las pantallas admin pueden ser posteriores al MVP de cobro, pero deben estar contempladas.

## MVP de implementacion

Primera entrega usable:

1. Configuracion Stripe.
2. Migraciones de billing.
3. Formulario publico de registro.
4. Checkout embebido.
5. Webhook con idempotencia.
6. Activacion de usuario por webhook.
7. Limite de alojamientos segun cantidad pagada.
8. Pantalla owner de billing con enlace al portal Stripe.
9. Tests de controlador, servicio y webhook.

No incluir en esta primera entrega:

- declaracion OSS automatica,
- PDF de factura propio,
- gestion manual avanzada de eventos,
- cambio de cantidad propio si Customer Portal lo cubre,
- promociones,
- cupones,
- pruebas gratuitas,
- planes por niveles.

## Pendientes para segunda entrega

- Admin billing con filtros de problemas.
- Reprocesado manual de webhooks fallidos.
- Export CSV interno para contraste fiscal.
- Flujo propio para cambiar cantidad si Customer Portal no es suficiente.
- Avisos mas finos para pagos fallidos.
- Politica de suspension tras periodo de gracia.
- Documentacion operativa para soporte.

## Criterios de no desviacion

No hacer:

- planes complejos,
- trial gratuito,
- cupones,
- facturacion propia,
- calculo manual de IVA,
- multi-tenant,
- roles de equipo,
- marketplace,
- integraciones PMS,
- portal fiscal interno.

Si una tarea no ayuda directamente a cobrar, activar cuenta, controlar acceso o gestionar incidencias de billing, queda fuera de la primera fase.
