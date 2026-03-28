# Uso de Stitch MCP en `checkpol`

Esta nota documenta el comportamiento observado del MCP de Stitch en este proyecto para reducir llamadas fallidas y dejar una guia operativa a futuros agentes.

## Objetivo

Cuando una tarea de UI/UX requiera Stitch:

- usar Stitch por defecto si esta disponible,
- minimizar llamadas invalidas,
- saber que herramientas estan funcionando de forma fiable,
- tener una ruta de fallback local cuando Stitch falle.

## Que informacion expone el MCP

El MCP expone la firma basica de cada herramienta:

- nombre,
- parametros obligatorios y opcionales,
- descripcion corta.

No expone de forma fiable:

- todas las restricciones semanticas reales del backend,
- todos los valores realmente aceptados para algunos campos string,
- errores detallados cuando una llamada no gusta al backend.

Consecuencia:

- una llamada puede parecer valida por schema MCP y aun asi fallar con `Request contains an invalid argument.`

## Hallazgos verificados en esta sesion

Proyecto de prueba usado:

- `projects/15339684505409972256`

### Herramientas que han funcionado

- `create_project`
- `get_project`
- `generate_screen_from_text`
- `get_screen`
- `edit_screens`
- `list_design_systems`

### Herramientas/problematicas observadas

- `generate_screen_from_text` falla si `deviceType` se envia en minusculas (`mobile`, `desktop`)
- `generate_variants` devolvio `Request contains an invalid argument` tanto con `deviceType` como sin el
- `list_screens` devolvio `{}` incluso despues de generar pantallas correctamente

## Reglas practicas

### 1. `deviceType`

No asumir que los ejemplos de la descripcion (`mobile`, `desktop`) son los valores reales aceptados por backend.

Observado:

- `deviceType: "mobile"` -> falla
- `deviceType: "desktop"` -> falla
- `deviceType: "MOBILE"` -> funciona
- `deviceType: "DESKTOP"` -> funciona
- omitir `deviceType` tambien puede funcionar

Regla:

- preferir `MOBILE` o `DESKTOP` en mayusculas,
- si falla, reintentar sin `deviceType`.

### 2. `generate_screen_from_text`

Patron fiable observado:

- `projectId` valido
- `prompt` corto y claro
- `deviceType` omitido o en mayusculas

Ejemplo que funciono:

- `projectId: "15339684505409972256"`
- `prompt: "Simple mobile login screen"`
- sin `deviceType`

Ejemplo que tambien funciono:

- `projectId: "15339684505409972256"`
- `prompt: "Simple mobile card list"`
- `deviceType: "MOBILE"`

Regla:

- empezar con un prompt corto y funcional,
- no meter demasiadas condiciones en la primera llamada,
- si hay que explorar, validar primero una llamada minima antes de pedir variantes complejas.

### 3. `list_screens`

No confiar en `list_screens` como fuente unica de verdad.

Observado:

- devolvio `{}` aunque el proyecto ya tenia pantallas generadas y `get_screen` funcionaba con ids concretos.

Regla:

- si `generate_screen_from_text` devuelve un `screen id`, guardarlo,
- usar `get_screen` con:
  - `name`
  - `projectId`
  - `screenId`

Ejemplo que funciono:

- `name: "projects/15339684505409972256/screens/89004b93fc034d159986b3f58047410f"`
- `projectId: "15339684505409972256"`
- `screenId: "89004b93fc034d159986b3f58047410f"`

### 4. `edit_screens`

Funciono sobre una pantalla existente:

- con `deviceType: "MOBILE"`
- y tambien sin `deviceType`

Regla:

- para iterar sobre una pantalla ya creada, `edit_screens` parece mas fiable que `generate_variants`.

### 5. `generate_variants`

Estado actual:

- no se ha encontrado una combinacion fiable en esta sesion.

Probables causas:

- formato real de `variantOptions` mas estricto de lo que indica la firma MCP,
- restriccion interna del backend no expuesta,
- o bug temporal del conector/backend.

Regla:

- no depender de `generate_variants` para una tarea critica sin probar primero una llamada minima,
- como alternativa, generar una pantalla base y luego hacer varias llamadas a `edit_screens` con prompts distintos.

## Flujo recomendado

Para tareas de diseno con Stitch en este proyecto:

1. `create_project`
2. `generate_screen_from_text` con prompt minimo y sin `deviceType`
3. Si funciona, repetir con `deviceType` en mayusculas si hace falta
4. Guardar `projectId`, `screenId`, `name`
5. Usar `get_screen` para recuperar la pantalla
6. Usar `edit_screens` para variantes A/B en vez de `generate_variants` si este ultimo falla

## Estrategia A/B recomendada mientras `generate_variants` no sea fiable

En lugar de:

- una llamada a `generate_variants`

usar:

- una pantalla base con `generate_screen_from_text`
- dos llamadas separadas a `edit_screens` con prompts distintos

Ejemplo:

- Variante A: "Haz la pantalla mas calmada, aireada y amable"
- Variante B: "Haz la pantalla mas compacta, operativa y escaneable"

## Fallback local

Si Stitch falla con `Request contains an invalid argument`:

- reducir la llamada al minimo,
- probar sin `deviceType`,
- probar `deviceType` en mayusculas,
- evitar `generate_variants`,
- continuar con implementacion local si el fallo persiste.

## Limites de esta guia

Esta guia documenta comportamiento observado, no una especificacion oficial de Stitch.

Puede cambiar si:

- cambia el backend de Stitch,
- cambia el MCP,
- cambia la validacion interna de parametros.
