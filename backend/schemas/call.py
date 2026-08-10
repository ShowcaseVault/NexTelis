from pydantic import BaseModel, field_validator


class CallAuthorizationRequest(BaseModel):
    destination: str

    @field_validator("destination")
    @classmethod
    def digits_only(cls, value: str) -> str:
        if not value.isdigit():
            raise ValueError("destination must contain digits only")
        return value


class CallAuthorizationResponse(BaseModel):
    """Confirms `destination` is a live NexTelis number the caller's device may dial.

    The actual call setup happens over SIP directly against Asterisk (see
    docs/ASTERISK.md) — this only tells the app whether it's allowed to try.
    """

    destination: str
    callable: bool
