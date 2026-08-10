from sqlalchemy import select

from backend.models.claim_code import ClaimCode
from backend.repositories.base import BaseRepository


class ClaimCodeRepository(BaseRepository[ClaimCode]):
    model = ClaimCode

    async def get_by_code(self, code: str) -> ClaimCode | None:
        stmt = select(ClaimCode).where(
            ClaimCode.code == code,
            ClaimCode.deleted_at.is_(None),
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()
