import uuid
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from backend.db.types import EncryptedString
from backend.models.common_model import CommonModel

if TYPE_CHECKING:
    from backend.models.user import User


class Number(CommonModel):
    __tablename__ = "numbers"

    value: Mapped[str] = mapped_column(
        String(32), unique=True, index=True, nullable=False
    )
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    sip_password: Mapped[str] = mapped_column(EncryptedString(64), nullable=False)

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
    )

    user: Mapped["User"] = relationship(back_populates="number")
