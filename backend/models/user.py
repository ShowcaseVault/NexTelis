from typing import TYPE_CHECKING

from sqlalchemy import Boolean, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from backend.models.common_model import CommonModel

if TYPE_CHECKING:
    from backend.models.claim_code import ClaimCode
    from backend.models.device import Device
    from backend.models.number import Number


class User(CommonModel):
    __tablename__ = "users"

    email: Mapped[str] = mapped_column(
        String(255), unique=True, index=True, nullable=False
    )
    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    hashed_recovery_code: Mapped[str] = mapped_column(String(64), nullable=False)

    number: Mapped["Number | None"] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
        uselist=False,
    )
    devices: Mapped[list["Device"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    claim_codes: Mapped[list["ClaimCode"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
