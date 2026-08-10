import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict


class DeviceClaimRequest(BaseModel):
    claim_code: str
    device_name: str
    push_token: str | None = None


class DeviceRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    is_active: bool
    user_id: uuid.UUID
    created_at: datetime


class DeviceClaimResponse(BaseModel):
    """device_token is the raw bearer token; it is never retrievable again."""

    device: DeviceRead
    device_token: str
