#!/usr/bin/env bash
set -euo pipefail

# ODBC DSN password lives in .env, not baked into the image — render it here.
envsubst < /etc/odbc.ini.template > /etc/odbc.ini

exec asterisk -f -C /etc/asterisk/asterisk.conf
