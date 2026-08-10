import uuid

from sqlalchemy import select

from backend.models.device import Device
from backend.repositories.base import BaseRepository


class DeviceRepository(BaseRepository[Device]):
    model = Device

    async def get_by_hashed_token(self, hashed_token: str) -> Device | None:
        stmt = select(Device).where(
            Device.hashed_token == hashed_token,
            Device.deleted_at.is_(None),
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_by_user_id(self, user_id: uuid.UUID) -> list[Device]:
        stmt = select(Device).where(
            Device.user_id == user_id, Device.deleted_at.is_(None)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())
