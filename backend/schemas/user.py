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
    """Returned once, right after a user is created."""

    user: UserRead
    claim_code: str
    claim_code_expires_at: datetime
