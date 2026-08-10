import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict


class NumberRead(BaseModel):
    """
    Includes sip_password so the device can register directly with Asterisk
    as this number's PJSIP endpoint. Authorization for this is the existing
    device_token bearer auth — the caller already owns this number.
    """

    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    value: str
    is_active: bool
    user_id: uuid.UUID
    created_at: datetime
    sip_password: str
