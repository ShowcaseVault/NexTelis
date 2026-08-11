.PHONY: help up down build rebuild clean logs psql cli check dev web

# Shows targets and their trailing "## " comments.
help:
	@grep -E '^[a-zA-Z-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

# ── Stack ──────────────────────────────────────────────────
up: ## Start everything (Postgres, Asterisk, backend, site on :8080)
	docker compose up -d

down: ## Stop everything
	docker compose down

build: ## Build images
	docker compose build

rebuild: ## Rebuild and restart, e.g. make rebuild s=frontend
	docker compose build $(s)
	docker compose up -d $(s)

clean: ## Stop everything and delete volumes (destroys the database)
	docker compose down -v

# ── Inspect ────────────────────────────────────────────────
logs: ## Follow logs, e.g. make logs s=backend (all services if omitted)
	docker compose logs -f $(s)

psql: ## Open a database shell
	docker exec -it nextelis-postgres psql -U $${POSTGRES_USER:-nextelis} -d $${POSTGRES_DB:-nextelis}

cli: ## Open the Asterisk console
	docker exec -it nextelis-asterisk asterisk -rvvv

check: ## SIP endpoints, live registrations, active channels, uptime
	@docker exec nextelis-asterisk asterisk -rx "pjsip show endpoints"
	@docker exec nextelis-asterisk asterisk -rx "pjsip show contacts"
	@docker exec nextelis-asterisk asterisk -rx "core show channels"
	@docker exec nextelis-asterisk asterisk -rx "core show uptime"

# ── Local development ──────────────────────────────────────
# Both bind port 8000 / 5173 on the host — stop the matching container first.
dev: ## Run the backend on the host instead of in Docker
	uv run server.py

web: ## Vite dev server with hot reload (does not proxy /api)
	cd frontend && npm run dev
