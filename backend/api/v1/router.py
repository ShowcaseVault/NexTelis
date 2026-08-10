from fastapi import APIRouter

from backend.api.v1.routes import calls, devices, numbers, users

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(users.router)
api_router.include_router(numbers.router)
api_router.include_router(devices.router)
api_router.include_router(calls.router)
