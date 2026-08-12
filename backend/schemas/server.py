from typing import Literal

from pydantic import BaseModel


class ServerInfo(BaseModel):
    """
    Where and how a device should reach this deployment's SIP service.

    Devices use these verbatim to build their SIP account, so the host must be
    routable from the handset rather than from the backend.
    """

    sip_host: str
    sip_port: int
    # Signalling transport only — RTP media is UDP regardless.
    sip_transport: Literal["udp", "tcp", "tls"]
