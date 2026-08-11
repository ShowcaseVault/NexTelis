from fastapi import APIRouter

from backend.core.config import get_settings
from backend.schemas.server import ServerInfo

router = APIRouter(prefix="/server", tags=["server"])


@router.get("/info", response_model=ServerInfo)
async def get_server_info() -> ServerInfo:
    """
    Tells a device where to send SIP traffic and over which transport.

    Unauthenticated on purpose: a device needs this before it has a token,
    and it reveals only an address the device is about to connect to anyway.

    Configured rather than derived from the request, because the API and
    Asterisk don't have to be reachable the same way — the API can sit behind
    a proxy or tunnel that SIP cannot traverse.
    """
    settings = get_settings()
    return ServerInfo(
        sip_host=settings.sip_host,
        sip_port=settings.sip_port,
        sip_transport=settings.sip_transport,
    )
