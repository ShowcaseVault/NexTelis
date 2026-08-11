"""PJSIP Asterisk Realtime Architecture (ARA) tables.

These tables are read directly by Asterisk's res_config_odbc via sorcery —
column names and types follow Asterisk's expected realtime schema exactly
(see pjsip.conf realtime family mappings), not NexTelis conventions. Rows are
written by NumberService whenever a Number is assigned, never by Asterisk.
"""

from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from backend.db.base import Base


class PsEndpoint(Base):
    __tablename__ = "ps_endpoints"

    id: Mapped[str] = mapped_column(String(40), primary_key=True)
    transport: Mapped[str] = mapped_column(String(40), nullable=True)
    aors: Mapped[str] = mapped_column(String(200), nullable=True)
    auth: Mapped[str] = mapped_column(String(40), nullable=True)
    context: Mapped[str] = mapped_column(String(40), nullable=True)
    disallow: Mapped[str] = mapped_column(String(200), nullable=True)
    allow: Mapped[str] = mapped_column(String(200), nullable=True)
    direct_media: Mapped[str] = mapped_column(String(3), nullable=True)
    dtmf_mode: Mapped[str] = mapped_column(String(40), nullable=True)
    rtp_symmetric: Mapped[str] = mapped_column(String(3), nullable=True)
    force_rport: Mapped[str] = mapped_column(String(3), nullable=True)
    rewrite_contact: Mapped[str] = mapped_column(String(3), nullable=True)
    callerid: Mapped[str] = mapped_column(String(40), nullable=True)


class PsAuth(Base):
    __tablename__ = "ps_auths"

    id: Mapped[str] = mapped_column(String(40), primary_key=True)
    auth_type: Mapped[str] = mapped_column(String(40), nullable=True)
    username: Mapped[str] = mapped_column(String(40), nullable=True)
    password: Mapped[str] = mapped_column(String(80), nullable=True)


class PsAor(Base):
    __tablename__ = "ps_aors"

    id: Mapped[str] = mapped_column(String(40), primary_key=True)
    max_contacts: Mapped[int] = mapped_column(Integer, nullable=True)
    remove_existing: Mapped[str] = mapped_column(String(3), nullable=True)
    support_path: Mapped[str] = mapped_column(String(3), nullable=True)
    qualify_frequency: Mapped[int] = mapped_column(Integer, nullable=True)
