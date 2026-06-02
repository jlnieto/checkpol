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

## Estado implementado

Primera vertical implementada:

- `GET /registro` y `POST /registro`
- `GET /registro/pago/{token}`
- `GET /registro/confirmando/{token}`
- `POST /webhooks/stripe`
- `GET /bookings/billing`
- `POST /bookings/billing/portal`
- migracion `V28__create_billing_tables.sql`
- dependencia `com.stripe:stripe-java:32.0.0`
- Checkout embebido de Stripe
- activacion de `AppUser` con rol `OWNER` solo desde webhook
- limite de alojamientos segun `subscription.quantity`
- portal de Stripe para facturas, datos fiscales y cambios permitidos por la configuracion del portal

Variables de entorno reales:

- `CHECKPOL_BILLING_STRIPE_SECRET_KEY`
- `CHECKPOL_BILLING_STRIPE_PUBLISHABLE_KEY`
- `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`
- `CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `CHECKPOL_PUBLIC_BASE_URL`
- `CHECKPOL_BILLING_STRIPE_CUSTOMER_PORTAL_CONFIGURATION_ID` opcional
- `CHECKPOL_BILLING_SIGNUP_EXPIRATION_MINUTES` opcional
- `CHECKPOL_BILLING_GRACE_PERIOD_DAYS` opcional

Pendiente operativo antes de vender:

- crear el producto/precio mensual en Stripe con impuestos inclusivos,
- activar Stripe Tax y los datos legales de la LLC,
- configurar Customer Portal para permitir facturas, metodo de pago y, si se acepta, cambio de cantidad,
- configurar el webhook en Stripe apuntando a `/webhooks/stripe`,
- validar fiscalidad OSS con asesor antes de emitir facturas reales.

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

Rutas implementadas:

- `GET /registro`
- `POST /registro`

Pantalla:

- email,
- contrasena,
- selector de alojamientos,
- resumen de precio,
- boton `Continuar al pago`.

Copy de referencia:

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
- `line_items[0].price = CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `line_items[0].quantity = accommodation_quantity`
- `automatic_tax.enabled = true`
- `tax_id_collection.enabled = true`
- `billing_address_collection = required`
- metadata:
  - `pending_signup_id`

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

## Modelo de datos implementado

### `pending_signup`

Campos:

- `id`
- `email`
- `password_hash`
- `accommodation_quantity`
- `status`
- `stripe_customer_id`
- `stripe_checkout_session_id`
- `checkout_client_secret`
- `expires_at`
- `created_at`
- `updated_at`

Estados:

- `PENDING_PAYMENT`
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

- `PENDING`
- `ACTIVE`
- `TRIALING`
- `PAST_DUE`
- `UNPAID`
- `CANCELED`
- `INCOMPLETE`
- `INCOMPLETE_EXPIRED`
- `PAUSED`

`customer_type` implementado:

- `INDIVIDUAL`
- `BUSINESS`
- `UNKNOWN`

`tax_mode` implementado:

- `OSS_EU_B2C`
- `EU_B2B_REVERSE_CHARGE`
- `DOMESTIC_OR_OTHER`
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
- `invoice.created`
- `invoice.finalized`
- `invoice.paid`
- `invoice.payment_failed`

Pendientes de una segunda entrega:

- `invoice.voided`
- `invoice.marked_uncollectible`
- `customer.updated`

Reglas de procesamiento:

- verificar firma con `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`,
- guardar evento,
- ignorar eventos ya procesados,
- no asumir orden perfecto,
- dejar los eventos fallidos registrados para revision,
- no borrar payloads necesarios para diagnostico sin una politica explicita.

No implementado todavia:

- pantalla admin para consultar eventos,
- reprocesado manual de eventos fallidos,
- eventos `invoice.voided`, `invoice.marked_uncollectible` y `customer.updated`.

## Reglas de acceso por estado

Alcance implementado en la primera vertical:

- `ACTIVE` y `TRIALING` permiten crear viviendas si no se supera el limite contratado.
- `PAST_DUE` permite crear viviendas solo dentro del periodo de gracia configurado.
- estados no utilizables bloquean la creacion de nuevas viviendas.
- owners sin `BillingAccount` se consideran cuentas manuales y no se bloquean por Stripe.

Bloqueos mas amplios sobre crear estancias, generar XML o presentar en SES quedan para una segunda entrega.

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

### `cancel_at_period_end`

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

- `CHECKPOL_BILLING_STRIPE_SECRET_KEY`
- `CHECKPOL_BILLING_STRIPE_PUBLISHABLE_KEY`
- `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`
- `CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`
- `CHECKPOL_PUBLIC_BASE_URL`

Variables opcionales:

- `CHECKPOL_BILLING_STRIPE_CUSTOMER_PORTAL_CONFIGURATION_ID`
- `CHECKPOL_BILLING_SIGNUP_EXPIRATION_MINUTES`
- `CHECKPOL_BILLING_GRACE_PERIOD_DAYS`

Valores esperados:

- `CHECKPOL_BILLING_STRIPE_SECRET_KEY`: clave secreta de Stripe, normalmente `sk_test_...` o `sk_live_...`.
- `CHECKPOL_BILLING_STRIPE_PUBLISHABLE_KEY`: clave publicable de Stripe, normalmente `pk_test_...` o `pk_live_...`.
- `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`: secreto `whsec_...` del endpoint de webhook.
- `CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`: id de precio recurrente mensual, normalmente `price_...`.
- `CHECKPOL_PUBLIC_BASE_URL`: URL publica de Checkpol, sin barra final preferiblemente.
- `CHECKPOL_BILLING_STRIPE_CUSTOMER_PORTAL_CONFIGURATION_ID`: id opcional de configuracion del portal, normalmente `bpc_...`.
- `CHECKPOL_BILLING_SIGNUP_EXPIRATION_MINUTES`: minutos de validez de un registro pendiente. Por defecto `60`.
- `CHECKPOL_BILLING_GRACE_PERIOD_DAYS`: dias de gracia para `PAST_DUE`. Por defecto `7`.

## Runbook operativo Stripe

Antes de vender:

1. Crear el producto `Checkpol Esencial` en Stripe.
2. Crear un precio recurrente mensual de `3,90 EUR` con `tax_behavior=inclusive`.
3. Guardar el id del precio en `CHECKPOL_BILLING_STRIPE_PRICE_CHECKPOL_ESENCIAL`.
4. Activar Stripe Tax y revisar que Checkout exige direccion de facturacion.
5. Configurar datos legales, soporte, web, emails y plantilla de factura de la LLC.
6. Configurar Customer Portal para actualizar metodo de pago, datos fiscales, descarga de facturas y, si se acepta, cambio de cantidad.
7. Crear endpoint de webhook apuntando a `https://dominio/webhooks/stripe`.
8. Suscribir los eventos minimos documentados en la seccion de webhooks.
9. Guardar el signing secret del endpoint en `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET`.
10. Validar con asesor la configuracion fiscal OSS antes de emitir facturas reales.

Prueba local con Stripe CLI:

```bash
stripe login
stripe listen --forward-to localhost:8080/webhooks/stripe
```

Usar el `whsec_...` mostrado por `stripe listen` como `CHECKPOL_BILLING_STRIPE_WEBHOOK_SECRET` local.

Flujo local esperado:

1. Arrancar Checkpol con las variables Stripe de test.
2. Entrar en `/registro`.
3. Crear registro con email nuevo y cantidad de alojamientos.
4. Completar Checkout con tarjeta de prueba de Stripe.
5. Esperar el webhook `checkout.session.completed`.
6. Confirmar que se crea `AppUser`, `billing_account` y `pending_signup` queda `COMPLETED`.
7. Entrar con el email y contrasena usados en el registro.
8. Revisar `/bookings/billing`.

## Incidencias y soporte

Checkout completado pero cuenta sin activar:

- revisar `stripe_event_logs` por `checkout.session.completed`,
- confirmar que el evento no esta `FAILED`,
- confirmar que la suscripcion Stripe existe y tiene estado utilizable,
- si no existe evento, revisar configuracion del webhook y firma `whsec_...`.

Registro pendiente con el mismo email:

- si el `pending_signup` no ha expirado, el sistema bloquea un nuevo intento para evitar duplicados,
- si ha expirado, se marca como `EXPIRED` y se permite iniciar otro registro.

Webhook fallido:

- queda guardado con `processing_status=FAILED` y `error_message`,
- la primera version no incluye reprocesado manual desde admin,
- para resolverlo hay que revisar el payload, corregir la causa y usar herramientas de Stripe para reenviar el evento si procede.

Stripe sin configurar:

- `/registro` muestra error al enviar el formulario,
- `/webhooks/stripe` responde error si falta la configuracion necesaria,
- el owner no puede abrir Customer Portal si no hay clave secreta o cuenta Stripe asociada.

Limite de viviendas alcanzado:

- al crear una vivienda se compara `countByOwnerId` con `paid_accommodation_limit`,
- si se alcanza el limite, se muestra un error global en el formulario de vivienda,
- la accion esperada es ampliar cantidad desde `/bookings/billing` y Customer Portal si esta configurado.

## Privacidad y retencion

Checkpol guarda payloads completos de webhooks en `stripe_event_logs.payload` para diagnostico e idempotencia.

Reglas operativas hasta definir una politica automatizada:

- tratar `stripe_event_logs.payload` como dato sensible,
- no exponer payloads completos en pantallas publicas ni owner,
- limitar el acceso operativo a administradores tecnicos,
- no registrar claves Stripe ni secretos de webhook,
- revisar con asesor/legal el plazo de conservacion necesario para soporte, auditoria y fiscalidad,
- definir antes de produccion una politica de purga o minimizacion si los payloads contienen datos personales no necesarios.

## Checklist de QA

Cobertura manual minima antes de publicar:

- `/registro` valida email, contrasena y cantidad.
- Un email ya existente en `app_users` queda bloqueado.
- Un email con `pending_signup` no expirado queda bloqueado.
- Checkout embebido carga con `publishable_key` y `client_secret`.
- Pago correcto crea owner y billing account por webhook, no por redirect.
- Refresh en `/registro/confirmando/{token}` muestra estado pendiente o completado segun webhook.
- Webhook duplicado se ignora si ya estaba procesado.
- `customer.subscription.updated` actualiza estado, periodo, cantidad y `cancel_at_period_end`.
- `invoice.paid` o `invoice.finalized` crea/actualiza `billing_invoice`.
- `/bookings/billing` abre Customer Portal si el owner tiene cuenta Stripe.
- Crear vivienda se permite bajo limite contratado.
- Crear vivienda se bloquea al alcanzar limite contratado.
- Owner manual sin `BillingAccount` no queda bloqueado por Stripe.

Tests automatizados pendientes recomendados:

- MVC de `/registro` con validaciones y error de configuracion Stripe.
- Servicio de activacion desde `checkout.session.completed`.
- Webhook con firma valida y fixtures Stripe.
- Idempotencia de webhook repetido.
- Sincronizacion de `subscription.updated`.
- Sincronizacion de facturas.

## Pantallas necesarias

Publicas:

- `/registro`
- `/registro/pago/{token}`
- `/registro/confirmando/{token}`

Owner:

- `/bookings/billing`

Admin:

- `/admin/billing`
- `/admin/billing/events`
- `/admin/billing/invoices`

Las pantallas admin no estan implementadas en la primera vertical. Quedan contempladas para soporte y diagnostico en una entrega posterior.

## MVP de implementacion

Primera entrega usable:

1. Configuracion Stripe. Implementado.
2. Migraciones de billing. Implementado.
3. Formulario publico de registro. Implementado.
4. Checkout embebido. Implementado.
5. Webhook con idempotencia. Implementado.
6. Activacion de usuario por webhook. Implementado.
7. Limite de alojamientos segun cantidad pagada. Implementado.
8. Pantalla owner de billing con enlace al portal Stripe. Implementado.
9. Tests de limite de alojamientos y suite completa. Implementado parcialmente; faltan pruebas especificas de webhook con fixtures Stripe.

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
