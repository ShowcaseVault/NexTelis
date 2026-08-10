FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    asterisk \
    asterisk-modules \
    odbc-postgresql \
    unixodbc \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

COPY config/odbcinst.ini /etc/odbcinst.ini
COPY config/odbc.ini.template /etc/odbc.ini.template

COPY scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 5060/udp 5060/tcp 10000-20000/udp

ENTRYPOINT ["/entrypoint.sh"]