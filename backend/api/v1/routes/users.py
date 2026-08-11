import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from backend.api.deps import get_user_service
from backend.core.exceptions import ConflictError, InvalidClaimCodeError, NotFoundError
from backend.schemas.user import (
    ClaimCodeRequest,
    UserCreate,
    UserRead,
    UserRegisteredResponse,
    UserWithClaimCode,
)
from backend.services.user_service import UserService

router = APIRouter(prefix="/users", tags=["users"])

UserServiceDep = Annotated[UserService, Depends(get_user_service)]


@router.post(
    "", response_model=UserRegisteredResponse, status_code=status.HTTP_201_CREATED
)
async def register_user(
    payload: UserCreate, service: UserServiceDep
) -> UserRegisteredResponse:
    try:
        return await service.register_user(payload)
    except ConflictError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(exc)
        ) from exc


@router.get("/{user_id}", response_model=UserRead)
async def get_user(user_id: uuid.UUID, service: UserServiceDep) -> UserRead:
    try:
        return await service.get_user(user_id)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc


@router.post("/claim-code", response_model=UserWithClaimCode)
async def reissue_claim_code(
    payload: ClaimCodeRequest, service: UserServiceDep
) -> UserWithClaimCode:
    """Re-pair an existing user to a new device — e.g. after reinstalling
    the app, when POST /users 409s because the email already exists.
    Requires the recovery code issued at registration."""
    try:
        return await service.reissue_claim_code(payload.email, payload.recovery_code)
    except NotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc
    except InvalidClaimCodeError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)
        ) from exc
