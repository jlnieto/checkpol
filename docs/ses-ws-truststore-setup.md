# Configurar Truststore SES Muy Facil

Si quieres presentar en SES por servicio web, necesitas 2 cosas:

1. tu usuario y clave del WS,
2. un archivo `.p12` para que Java confie en la web de SES.

Ese archivo se llama `truststore`.

## Lo mas facil en local

Desde la raiz del proyecto:

```bash
bash ops/dev-java-projects/create-ses-pre-truststore.sh
```

Eso te crea dos ficheros en:

- `~/.config/checkpol/ssl/pre-ses.crt`
- `~/.config/checkpol/ssl/checkpol-ses-pre.p12`

Y te imprime las lineas exactas que debes copiar en `.env`.

## Que poner en `.env`

Despues de ejecutar el script, añade estas variables:

```dotenv
CHECKPOL_SES_WS_URL=https://hospedajes.pre-ses.mir.es/hospedajes-web/ws/v1/comunicacion
CHECKPOL_SES_WS_TRUSTSTORE_HOST_DIR=/home/jose/.config/checkpol/ssl
CHECKPOL_SES_WS_TRUSTSTORE_PATH=file:/run/checkpol/ssl/checkpol-ses-pre.p12
CHECKPOL_SES_WS_TRUSTSTORE_PASSWORD=changeit
CHECKPOL_SES_WS_TRUSTSTORE_TYPE=PKCS12
```

## Que significa cada una

- `CHECKPOL_SES_WS_TRUSTSTORE_HOST_DIR`
  Carpeta real de tu maquina donde esta el `.p12`.
- `CHECKPOL_SES_WS_TRUSTSTORE_PATH`
  Ruta que vera la aplicacion dentro del contenedor Docker.
- `CHECKPOL_SES_WS_TRUSTSTORE_PASSWORD`
  La contrasena que pusiste al crear el `.p12`.
- `CHECKPOL_SES_WS_TRUSTSTORE_TYPE`
  El tipo del archivo. Aqui es `PKCS12`.

## Reiniciar la app

Despues de guardar `.env`:

```bash
dev stop checkpol
dev up checkpol
```

## Comprobacion mental rapida

Para que funcione, estas 3 piezas tienen que encajar:

1. el fichero existe en tu maquina:
   `~/.config/checkpol/ssl/checkpol-ses-pre.p12`
2. Docker monta esa carpeta en:
   `/run/checkpol/ssl`
3. Spring apunta a:
   `file:/run/checkpol/ssl/checkpol-ses-pre.p12`

Si una de esas 3 no coincide, fallara.

## Nota practica

Este script usa el certificado actual de `pre-ses`.
Para local va bien y es la forma mas simple de arrancar.
Si mas adelante quieres una configuracion mas robusta para produccion, conviene confiar en la CA o cadena oficial que entregue SES.
