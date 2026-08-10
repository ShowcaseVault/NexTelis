import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, field_validator


class NumberCreate(BaseModel):
    value: str

    @field_validator("value")
    @classmethod
    def digits_only(cls, value: str) -> str:
        if not value.isdigit():
            raise ValueError("number value must contain digits only")
        return value


class NumberRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    value: str
    is_active: bool
    user_id: uuid.UUID
    created_at: datetime
