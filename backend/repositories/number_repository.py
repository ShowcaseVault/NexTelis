import uuid

from sqlalchemy import select

from backend.models.number import Number
from backend.repositories.base import BaseRepository


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
