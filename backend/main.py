from fastapi import FastAPI

from backend.api.v1.router import api_router

app = FastAPI(title="NexTelis Backend")
app.include_router(api_router)


@app.get("/health", tags=["health"])
async def health() -> dict[str, str]:
    return {"status": "ok"}
