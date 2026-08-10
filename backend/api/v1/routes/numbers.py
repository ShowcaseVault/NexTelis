import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from backend.api.deps import get_number_service
from backend.core.exceptions import ConflictError, NotFoundError
from backend.schemas.number import NumberRead
from backend.services.number_service import NumberService

router = APIRouter(prefix="/users/{user_id}/number", tags=["numbers"])

NumberServiceDep = Annotated[NumberService, Depends(get_number_service)]


@router.post("", response_model=NumberRead, status_code=status.HTTP_201_CREATED)
async def assign_number(user_id: uuid.UUID, service: NumberServiceDep) -> NumberRead:
    try:
        return await service.assign_number(user_id)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc
    except ConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(exc)
        ) from exc


@router.get("", response_model=NumberRead)
async def get_number(user_id: uuid.UUID, service: NumberServiceDep) -> NumberRead:
    try:
        return await service.get_number_for_user(user_id)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc
