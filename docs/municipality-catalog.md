# Catálogo de municipios

`checkpol` mantiene un catálogo local de municipios de España y su relación con códigos postales para validar direcciones españolas.

El catálogo ya se puede gestionar desde `/admin/municipalities`.

## Fuentes y recursos

- recurso semilla local de municipios:
  - `src/main/resources/municipality-catalog/spanish-municipalities.csv`
- recurso semilla local de mappings postales:
  - `src/main/resources/municipality-catalog/spanish-postal-code-municipalities.csv`
- URL oficial por defecto para municipios:
  - `https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx`
- URL oficial por defecto para el callejero:
  - `https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip`

El módulo admin permite descargar datasets remotos y no obliga a reconstruir la aplicación para cada actualización.

## Formato esperado

Los CSV usan `;` como separador y deben incluir exactamente estas cabeceras.

`spanish-municipalities.csv`

```text
provinceCode;provinceName;municipalityCode;municipalityName
28;Madrid;28079;Madrid
```

`spanish-postal-code-municipalities.csv`

```text
postalCode;municipalityCode
28001;28079
```

Restricciones validadas por el importador:

- `provinceCode`: 2 dígitos
- `municipalityCode`: 5 dígitos
- `postalCode`: 5 dígitos
- no se aceptan códigos de municipio duplicados dentro del mismo CSV
- no se aceptan mappings `postalCode + municipalityCode` duplicados
- no se aceptan mappings a municipios no presentes en el CSV de municipios
- no se aceptan ficheros vacíos o con cabecera distinta

## Formatos remotos admitidos

### Municipios

La carga remota admite:

- `XLSX` oficial del INE con la estructura actual `CODAUTO / CPRO / CMUN / DC / NOMBRE`
- o `CSV` ya normalizado con el formato interno

Cuando se usa el XLSX oficial del INE, la aplicación lo transforma automáticamente al CSV interno.

### Mapping postal

La carga remota admite:

- `ZIP` oficial del callejero del INE que contenga el fichero `TRAM.*`
- `CSV` con formato interno `postalCode;municipalityCode`
- o `ZIP` que contenga un único CSV con ese mismo formato

Cuando se usa el ZIP oficial del callejero, la aplicación:

- localiza el fichero `TRAM.*`,
- extrae de cada tramo el `municipalityCode` y el `postalCode`,
- deduplica pares `postalCode + municipalityCode`,
- y lo transforma automáticamente al CSV interno.

Compatibilidad actual:

- el parser directo está preparado para el diseño actual del callejero publicado desde julio de 2025,
- el CSV normalizado sigue admitiéndose como respaldo o para pruebas controladas.

## Importación

La importación está preparada para ser idempotente:

- inserta nuevos registros,
- actualiza cambios de nombre o provincia,
- reactiva registros existentes si vuelven a aparecer,
- desactiva registros del mismo origen que desaparezcan del CSV.

Además, cada importación administrativa queda registrada en la tabla de histórico con:

- origen,
- versión,
- usuario que la lanzó,
- resultado,
- y contadores básicos de municipios y mappings afectados.

## Flujo administrativo

Desde `/admin/municipalities` el `SUPER_ADMIN` puede:

1. indicar la URL del fichero oficial de municipios,
2. indicar la URL del ZIP oficial del callejero o del CSV postal equivalente,
3. fijar `source` y `sourceVersion`,
4. ejecutar una previsualización sin escribir en BD,
5. y después confirmar la importación.

La importación repite la validación antes de escribir en base de datos.

## Arranque opcional

Por defecto el catálogo **no** se importa automáticamente al arrancar.

Para activarlo:

```yaml
checkpol:
  municipality:
    catalog:
      import-on-startup: true
```

Configuración disponible:

```yaml
checkpol:
  municipality:
    catalog:
      import-on-startup: false
      source: classpath-csv
      source-version: example-v1
      municipalities-resource: classpath:municipality-catalog/spanish-municipalities.csv
      postal-mappings-resource: classpath:municipality-catalog/spanish-postal-code-municipalities.csv
    admin:
      default-source: ine-open-data
      default-municipalities-url: https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx
      default-postal-mappings-url: https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip
```

## Reimportación

La reimportación usa `source` y `sourceVersion`:

- actualiza registros existentes del mismo origen,
- reactiva los que vuelven a aparecer,
- desactiva los que desaparecen del CSV de ese mismo origen.

El comportamiento es idempotente para el mismo contenido del mismo origen.

## Nota para desarrollo

La historia de migraciones de municipios se ha limpiado para dejar solo el modelo final basado en catálogo local.

Si tu base local viene de iteraciones anteriores de este bloque, recréala o limpia su historial de Flyway antes de arrancar con esta versión del código.

## Qué pasa si el catálogo no está cargado

- las direcciones de países distintos de España siguen funcionando con municipio libre,
- las direcciones de España no se pueden guardar porque el municipio debe salir del catálogo local,
- el formulario mostrará un mensaje indicando que no ha podido cargar municipios para ese código postal.

En formularios:

- si hay catálogo para un CP español, se ofrecen municipios válidos,
- si solo hay uno, queda seleccionado automáticamente,
- si no lo hay o falla la consulta, no se permite resolver España por texto libre.

## Importante

Los CSV incluidos ahora son solo una semilla de ejemplo para dejar el mecanismo operativo y testeable.

## Qué falta para producción real

Para dejar esto listo a escala nacional hace falta, como mínimo:

1. sustituir la semilla de ejemplo por un dataset nacional completo y verificado
2. validar periódicamente que la URL oficial del callejero sigue manteniendo el diseño compatible esperado
3. decidir la política operativa de refresco del catálogo desde `/admin/municipalities`
4. versionar cada carga real con `sourceVersion` trazable
5. validar el dataset completo con pruebas sobre cobertura por CP y municipios compartidos entre códigos postales

Para España el sistema depende por completo del catálogo local: código postal, lista de municipios válida para ese CP y código oficial de municipio. La calidad final depende de que el catálogo nacional cargado sea correcto y completo.
