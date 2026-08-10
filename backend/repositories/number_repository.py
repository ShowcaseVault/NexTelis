import uuid

from sqlalchemy import select

from backend.core.exceptions import ConflictError
from backend.core.security import generate_number_value
from backend.models.number import Number
from backend.repositories.base import BaseRepository

_MAX_GENERATION_ATTEMPTS = 5


class NumberRepository(BaseRepository[Number]):
    model = Number

    async def get_by_value(self, value: str) -> Number | None:
        stmt = select(Number).where(Number.value == value, Number.deleted_at.is_(None))
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_user_id(self, user_id: uuid.UUID) -> Number | None:
        stmt = select(Number).where(
            Number.user_id == user_id, Number.deleted_at.is_(None)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def generate_unique_value(self) -> str:
        for _ in range(_MAX_GENERATION_ATTEMPTS):
            value = generate_number_value()
            if await self.get_by_value(value) is None:
                return value
        raise ConflictError("could not generate a unique number, try again")
