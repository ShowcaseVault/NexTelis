import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr


class UserCreate(BaseModel):
    email: EmailStr
    display_name: str


class UserRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    email: str
    display_name: str
    is_active: bool
    created_at: datetime


class UserWithClaimCode(BaseModel):
    """Returned once, right after a user is created or a claim code is reissued."""

    user: UserRead
    claim_code: str
    claim_code_expires_at: datetime


class UserRegisteredResponse(UserWithClaimCode):
    """Registration response only — includes the one-time recovery code.

    The recovery code is never returned again after this call. The user
    (or app, on their behalf) must store it to re-pair a future device.
    """

    recovery_code: str


class ClaimCodeRequest(BaseModel):
    """Re-pair an existing user to a new device (e.g. after reinstalling
    the app or losing the original device).

    Requires the recovery code issued at registration, alongside email —
    email alone is not proof of ownership. See docs/FINDINGS.md.
    """

    email: EmailStr
    recovery_code: str
