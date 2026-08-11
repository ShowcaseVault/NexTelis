# Backend control plane. Built from the repo root (not backend/) because
# pyproject.toml, uv.lock, and alembic.ini all live there.
FROM python:3.13-slim

# uv resolves from the committed lockfile, so container and host installs
# stay identical.
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH"

WORKDIR /app

# Dependency layer first, so application edits don't reinstall the world.
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-install-project

COPY alembic.ini ./
COPY backend ./backend

RUN uv sync --frozen

EXPOSE 8000

# Migrations run at start rather than in the image: the database only exists
# at runtime, and this keeps `docker compose up` a single step.
CMD ["sh", "-c", "alembic upgrade head && uvicorn backend.main:app --host 0.0.0.0 --port 8000"]
