.PHONY: help up up-all down build rebuild clean logs psql cli check dev web


DC := docker compose -f compose.datastore.yaml -f compose.yaml

# Shows targets and their trailing "## " comments.
help:
	@grep -E '^[a-zA-Z-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

# ── Stack ──────────────────────────────────────────────────
up: ## Start Postgres + Asterisk only (run backend/site on the host)
	docker compose -f compose.datastore.yaml up -d

up-all: ## Start everything, including the containerized backend and site
	$(DC) up -d

down: ## Stop everything
	$(DC) down

build: ## Build images
	$(DC) build

rebuild: ## Rebuild and restart, e.g. make rebuild s=frontend
	$(DC) build $(s)
	$(DC) up -d $(s)

clean: ## Stop everything and delete volumes (destroys the database)
	$(DC) down -v

# ── Inspect ────────────────────────────────────────────────
logs: ## Follow logs, e.g. make logs s=backend (all services if omitted)
	$(DC) logs -f $(s)

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
dev: ## Run the backend on the host (port 8000)
	uv run server.py

web: ## Vite dev server with hot reload, proxies /api to localhost:8000
	cd frontend && npm run dev
