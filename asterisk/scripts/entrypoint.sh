#!/usr/bin/env bash
set -euo pipefail

# ODBC DSN password lives in .env, not baked into the image — render it here.
envsubst < /etc/odbc.ini.template > /etc/odbc.ini

envsubst < /etc/asterisk/pjsip.conf.template > /etc/asterisk/pjsip.conf

# transport-tls is opt-in: only append it once a cert/key are actually
# provisioned, so systems without one don't fail to load a broken transport.
if [ -n "${SIP_TLS_CERT_PATH:-}" ] && [ -n "${SIP_TLS_KEY_PATH:-}" ]; then
  envsubst < /etc/asterisk/pjsip.conf.tls.template >> /etc/asterisk/pjsip.conf
fi

exec asterisk -f -C /etc/asterisk/asterisk.conf
