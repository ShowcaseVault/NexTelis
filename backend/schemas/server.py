from pydantic import BaseModel


class ServerInfo(BaseModel):
    """Deployment details a device needs before it can register with SIP."""

    # None means "not configured" — the device should fall back to the host
    # it uses for the API, which is correct when both share an address.
    sip_host: str | None
    sip_port: int
