import uuid
from datetime import UTC, datetime
from typing import Generic, TypeVar

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.models.common_model import CommonModel

ModelT = TypeVar("ModelT", bound=CommonModel)


class BaseRepository(Generic[ModelT]):
    """Thin async CRUD wrapper around a single ORM model.

    Reads exclude soft-deleted rows by default. Holds no state beyond the
    session; callers are responsible for commit/rollback so services can
    group multiple repository calls into one transaction.
    """

    model: type[ModelT]

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get(self, id_: uuid.UUID) -> ModelT | None:
        stmt = select(self.model).where(
            self.model.id == id_,
            self.model.deleted_at.is_(None),
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    def add(self, instance: ModelT) -> ModelT:
        self.session.add(instance)
        return instance

    async def soft_delete(self, instance: ModelT) -> None:
        instance.deleted_at = datetime.now(UTC)
        await self.flush()

    async def flush(self) -> None:
        await self.session.flush()

    async def commit(self) -> None:
        await self.session.commit()
