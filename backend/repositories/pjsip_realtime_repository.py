from sqlalchemy.ext.asyncio import AsyncSession

from backend.models.pjsip_realtime import PsAor, PsAuth, PsEndpoint

MAX_CONTACTS_PER_NUMBER = 5
QUALIFY_FREQUENCY_SECONDS = 30


class PjsipRealtimeRepository:
    """Writes the Asterisk-owned ps_endpoints/ps_auths/ps_aors rows.

    Not a BaseRepository: these tables have no CommonModel columns
    (no id/timestamps/soft-delete) since their schema is dictated by
    Asterisk's realtime sorcery, not NexTelis conventions.
    """

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    def upsert_for_number(self, extension: str, sip_password: str) -> None:
        self.session.add(
            PsEndpoint(
                id=extension,
                transport="transport-udp",
                aors=extension,
                auth=extension,
                context="phones",
                disallow="all",
                allow="ulaw,alaw",
                direct_media="no",
                dtmf_mode="rfc4733",
                rtp_symmetric="yes",
                force_rport="yes",
                rewrite_contact="yes",
                callerid=extension,
            )
        )
        self.session.add(
            PsAuth(
                id=extension,
                auth_type="userpass",
                username=extension,
                password=sip_password,
            )
        )
        self.session.add(
            PsAor(
                id=extension,
                max_contacts=MAX_CONTACTS_PER_NUMBER,
                remove_existing="yes",
                support_path="yes",
                qualify_frequency=QUALIFY_FREQUENCY_SECONDS,
            )
        )

    async def flush(self) -> None:
        await self.session.flush()
