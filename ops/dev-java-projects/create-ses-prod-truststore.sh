#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${1:-${HOME}/.config/checkpol/ssl}"
TRUSTSTORE_PASSWORD="${2:-changeit}"
TRUSTSTORE_TYPE="PKCS12"
ENDPOINT_HOST="hospedajes.ses.mir.es"
CERT_FILE="${OUTPUT_DIR}/ses-prod.crt"
TRUSTSTORE_FILE="${OUTPUT_DIR}/checkpol-ses-prod.p12"

mkdir -p "${OUTPUT_DIR}"

openssl s_client -showcerts -connect "${ENDPOINT_HOST}:443" -servername "${ENDPOINT_HOST}" </dev/null 2>/dev/null \
  | awk 'BEGIN { printing = 0 } /-----BEGIN CERTIFICATE-----/ { printing = 1 } printing { print } /-----END CERTIFICATE-----/ { exit }' \
  > "${CERT_FILE}"

if [[ ! -s "${CERT_FILE}" ]]; then
  echo "No he podido descargar el certificado de ${ENDPOINT_HOST}." >&2
  exit 1
fi

rm -f "${TRUSTSTORE_FILE}"
keytool -importcert \
  -alias ses-prod \
  -file "${CERT_FILE}" \
  -keystore "${TRUSTSTORE_FILE}" \
  -storetype "${TRUSTSTORE_TYPE}" \
  -storepass "${TRUSTSTORE_PASSWORD}" \
  -noprompt >/dev/null

cat <<EOF
Truststore creado:
- certificado: ${CERT_FILE}
- truststore: ${TRUSTSTORE_FILE}

Pon estas variables en tu .env para docker-compose:
CHECKPOL_SES_WS_URL=https://hospedajes.ses.mir.es/hospedajes-web/ws/v1/comunicacion
CHECKPOL_SES_WS_TRUSTSTORE_HOST_DIR=${OUTPUT_DIR}
CHECKPOL_SES_WS_TRUSTSTORE_PATH=file:/run/checkpol/ssl/$(basename "${TRUSTSTORE_FILE}")
CHECKPOL_SES_WS_TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD}
CHECKPOL_SES_WS_TRUSTSTORE_TYPE=${TRUSTSTORE_TYPE}
EOF
