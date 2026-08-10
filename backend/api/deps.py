from typing import Annotated

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from backend.db.session import get_db
from backend.repositories.claim_code_repository import ClaimCodeRepository
from backend.repositories.device_repository import DeviceRepository
from backend.repositories.number_repository import NumberRepository
from backend.repositories.user_repository import UserRepository
from backend.services.device_service import DeviceService
from backend.services.number_service import NumberService
from backend.services.user_service import UserService

SessionDep = Annotated[AsyncSession, Depends(get_db)]


def get_user_repository(session: SessionDep) -> UserRepository:
    return UserRepository(session)


def get_number_repository(session: SessionDep) -> NumberRepository:
    return NumberRepository(session)


def get_device_repository(session: SessionDep) -> DeviceRepository:
    return DeviceRepository(session)


def get_claim_code_repository(session: SessionDep) -> ClaimCodeRepository:
    return ClaimCodeRepository(session)


def get_user_service(
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
    claim_code_repository: Annotated[
        ClaimCodeRepository, Depends(get_claim_code_repository)
    ],
) -> UserService:
    return UserService(user_repository, claim_code_repository)


def get_number_service(
    number_repository: Annotated[NumberRepository, Depends(get_number_repository)],
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
) -> NumberService:
    return NumberService(number_repository, user_repository)


def get_device_service(
    device_repository: Annotated[DeviceRepository, Depends(get_device_repository)],
    claim_code_repository: Annotated[
        ClaimCodeRepository, Depends(get_claim_code_repository)
    ],
) -> DeviceService:
    return DeviceService(device_repository, claim_code_repository)
