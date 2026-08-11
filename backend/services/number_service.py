import uuid

from backend.core.exceptions import ConflictError, NotFoundError
from backend.core.security import generate_sip_password
from backend.models.number import Number
from backend.repositories.number_repository import NumberRepository
from backend.repositories.pjsip_realtime_repository import PjsipRealtimeRepository
from backend.repositories.user_repository import UserRepository


class NumberService:
    def __init__(
        self,
        number_repository: NumberRepository,
        user_repository: UserRepository,
        pjsip_realtime_repository: PjsipRealtimeRepository,
    ) -> None:
        self.number_repository = number_repository
        self.user_repository = user_repository
        self.pjsip_realtime_repository = pjsip_realtime_repository

    async def assign_number(self, user_id: uuid.UUID) -> Number:
        user = await self.user_repository.get(user_id)
        if user is None:
            raise NotFoundError(f"user {user_id} not found")

        existing = await self.number_repository.get_by_user_id(user_id)
        if existing is not None:
            return existing

        value = await self.number_repository.generate_unique_value()

        number = self.number_repository.add(
            Number(
                value=value,
                user_id=user_id,
                sip_password=generate_sip_password(),
            )
        )
        await self.number_repository.flush()

        self.pjsip_realtime_repository.upsert_for_number(
            number.value, number.sip_password
        )
        await self.number_repository.commit()
        return number

    async def get_number_for_user(self, user_id: uuid.UUID) -> Number:
        number = await self.number_repository.get_by_user_id(user_id)
        if number is None:
            raise NotFoundError(f"user {user_id} has no number assigned")
        return number

    async def resolve_callable_number(self, value: str) -> Number:
        number = await self.number_repository.get_by_value(value)
        if number is None:
            raise NotFoundError(f"number {value!r} not found")
        if not number.is_active:
            raise ConflictError(f"number {value!r} is not active")
        return number

    async def lookup_display_name(self, value: str) -> str:
        number = await self.number_repository.get_by_value(value)
        if number is None:
            raise NotFoundError(f"number {value!r} not found")
        user = await self.user_repository.get(number.user_id)
        if user is None:
            raise NotFoundError(f"number {value!r} has no owning user")
        return user.display_name
