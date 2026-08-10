import uuid
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from backend.models.common_model import CommonModel

if TYPE_CHECKING:
    from backend.models.user import User


class Device(CommonModel):
    __tablename__ = "devices"

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    push_token: Mapped[str | None] = mapped_column(String(255), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # SHA-256 hex digest of the opaque device token. The raw token is
    # returned to the device exactly once, at claim time, and never stored.
    hashed_token: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    user: Mapped["User"] = relationship(back_populates="devices")
