import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from backend.models.common_model import CommonModel

if TYPE_CHECKING:
    from backend.models.user import User


class ClaimCode(CommonModel):
    """A short-lived, single-use code that binds one Device to one User.

    Issued out-of-band (admin/seed script for now) and consumed by
    POST /devices/claim to mint a device token, with no password involved.
    """

    __tablename__ = "claim_codes"

    code: Mapped[str] = mapped_column(String(16), unique=True, index=True, nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    consumed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    user: Mapped["User"] = relationship(back_populates="claim_codes")
