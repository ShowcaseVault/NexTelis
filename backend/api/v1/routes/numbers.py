import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from backend.api.deps import CurrentDeviceDep, get_number_service
from backend.core.exceptions import ConflictError, NotFoundError
from backend.schemas.number import NumberLookupResult, NumberRead
from backend.services.number_service import NumberService

router = APIRouter(prefix="/users/{user_id}/number", tags=["numbers"])
lookup_router = APIRouter(prefix="/numbers", tags=["numbers"])

NumberServiceDep = Annotated[NumberService, Depends(get_number_service)]


def _require_own_user(user_id: uuid.UUID, caller: CurrentDeviceDep) -> None:
    """NumberRead now includes sip_password — these endpoints must only
    ever be reachable by the device that owns user_id, never by user_id
    alone as a path parameter."""
    if caller.user_id != user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="device does not belong to this user",
        )


@router.post("", response_model=NumberRead, status_code=status.HTTP_201_CREATED)
async def assign_number(
    user_id: uuid.UUID, caller: CurrentDeviceDep, service: NumberServiceDep
) -> NumberRead:
    _require_own_user(user_id, caller)
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
async def get_number(
    user_id: uuid.UUID, caller: CurrentDeviceDep, service: NumberServiceDep
) -> NumberRead:
    _require_own_user(user_id, caller)
    try:
        return await service.get_number_for_user(user_id)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc


@lookup_router.get("/{value}", response_model=NumberLookupResult)
async def lookup_number(
    value: str, caller: CurrentDeviceDep, service: NumberServiceDep
) -> NumberLookupResult:
    """Resolves a NexTelis number to its owner's display name — used by
    clients to show a caller-ID name for incoming calls even when the
    number isn't saved in the local phone contacts."""
    try:
        display_name = await service.lookup_display_name(value)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc
    return NumberLookupResult(value=value, display_name=display_name)
