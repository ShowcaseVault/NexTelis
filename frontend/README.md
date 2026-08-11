# NexTelis — Frontend

The public project site: what NexTelis is, how it works, and where the source
lives. React + TypeScript, built with Vite, served by nginx.

It also acts as the **reverse proxy**: everything is reachable on port 8080,
with `/api/` forwarded to the FastAPI backend on port 8000.

```text
:8080 ──┬── /          → static React bundle
        └── /api/...   → backend:8000 (FastAPI, same compose network)
```

## Running it

Day to day, run the infrastructure in Docker and this site on the host:

```bash
make up      # Postgres + Asterisk
make dev     # backend on :8000
make web     # Vite dev server, hot reload, proxies /api to :8000
```

To run the whole thing containerized instead — how it actually ships:

```bash
make up-all               # adds the backend and this site on :8080
make logs s=frontend      # tail nginx logs
make rebuild s=frontend   # rebuild + restart after editing the site
```

The two modes both bind ports 8000 and 8080, so use one or the other. In
containerized mode nginx reaches the backend by service name on the compose
network; in dev mode Vite proxies to `localhost:8000` instead
(`vite.config.ts`), so `/api` behaves identically either way.

## Pages

Two routes, hash-based so no router dependency is needed:

- `#/` — overview: what NexTelis is, the stack, verified status, limitations.
- `#/guide` — setup guide: server bring-up, per-phone onboarding, how a call
  travels, and the Makefile command reference.

## Theme

Brand tokens in `src/index.css` mirror
`android/app/src/main/res/values/colors.xml` exactly, so the site and the app
stay visually identical. Tokens are named by role (`--brand-primary`,
`--surface-card`, `--text-secondary`), not by hue — a rebrand means editing
hex values in those two files only.

The logo comes from `assets/NexTelis_Logo_NOBG.png` at the repo root.

## Content

Page copy is grounded in [`docs/NEXTELIS-V1.md`](../docs/NEXTELIS-V1.md),
including the Known Limitations section. That section is deliberately given
equal visual weight to the verified-features list rather than buried — the
site should not claim more than v1 has actually proven.
