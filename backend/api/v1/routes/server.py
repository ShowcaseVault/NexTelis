from fastapi import APIRouter

from backend.core.config import get_settings
from backend.schemas.server import ServerInfo

router = APIRouter(prefix="/server", tags=["server"])


@router.get("/info", response_model=ServerInfo)
async def get_server_info() -> ServerInfo:
    """
    Tells a device where to send SIP traffic.

    Unauthenticated on purpose: the device needs this before it has a token,
    and it exposes only an address the device is about to connect to anyway.

    The SIP host is configured separately from the API's address because the
    two do not have to be reachable the same way. The API may sit behind a
    reverse proxy or an HTTP tunnel; SIP and RTP are UDP and cannot traverse
    one, so devices need a directly routable address for Asterisk.
    """
    settings = get_settings()
    return ServerInfo(sip_host=settings.sip_host, sip_port=settings.sip_port)
