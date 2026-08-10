.PHONY: up down restart build logs status phones check shell clean

# ── Server ────────────────────────────────────────────────
run:
	uv run server.py

# ── Compose ────────────────────────────────────────────────
up:
	docker compose up -d

build:
	docker compose build

down:
	docker compose down

restart-asterisk:
	docker compose restart asterisk

prune:
	docker system prune -a --volumes

# ── Logs ───────────────────────────────────────────────────
logs-postgres:
	docker compose logs -f postgres

# ── Postgres ───────────────────────────────────────────────
psql:
	docker exec -it nextelis-postgres psql -U $${POSTGRES_USER:-nextelis} -d $${POSTGRES_DB:-nextelis}

pg-ready:
	docker exec nextelis-postgres pg_isready -U $${POSTGRES_USER:-nextelis} -d $${POSTGRES_DB:-nextelis}

logs-asterisk:
	docker compose logs -f asterisk

logs-tail:
	docker exec nextelis-asterisk tail -f /var/log/asterisk/asterisk.log

# ── Asterisk checks ────────────────────────────────────────
phones:
	docker exec nextelis-asterisk asterisk -rx "pjsip show endpoints"

status:
	docker exec nextelis-asterisk asterisk -rx "core show uptime"

channels:
	docker exec nextelis-asterisk asterisk -rx "core show channels"

check:
	docker exec nextelis-asterisk asterisk -rx "pjsip show endpoints"
	docker exec nextelis-asterisk asterisk -rx "core show channels"
	docker exec nextelis-asterisk asterisk -rx "core show uptime"

# ── Shell ──────────────────────────────────────────────────
shell:
	docker exec -it nextelis-asterisk bash

asterisk-cli:
	docker exec -it nextelis-asterisk asterisk -rvvv

# ── Clean ──────────────────────────────────────────────────
clean:
	docker compose down -v