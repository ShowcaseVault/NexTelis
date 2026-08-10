import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict


class NumberRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    value: str
    is_active: bool
    user_id: uuid.UUID
    created_at: datetime
