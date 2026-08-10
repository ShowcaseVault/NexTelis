"""Asterisk CDR (Call Detail Record) realtime table.

Written directly by Asterisk's cdr_odbc backend after every call — column
names follow Asterisk's standard CDR field set exactly (see cdr_odbc.conf),
not NexTelis conventions. NexTelis code only ever reads this table, never
writes it.
"""

from sqlalchemy import DateTime, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from backend.db.base import Base


class Cdr(Base):
    __tablename__ = "cdr"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    calldate: Mapped[str] = mapped_column(DateTime, nullable=True)
    clid: Mapped[str] = mapped_column(String(80), nullable=True)
    src: Mapped[str] = mapped_column(String(80), nullable=True)
    dst: Mapped[str] = mapped_column(String(80), nullable=True)
    dcontext: Mapped[str] = mapped_column(String(80), nullable=True)
    channel: Mapped[str] = mapped_column(String(80), nullable=True)
    dstchannel: Mapped[str] = mapped_column(String(80), nullable=True)
    lastapp: Mapped[str] = mapped_column(String(80), nullable=True)
    lastdata: Mapped[str] = mapped_column(String(80), nullable=True)
    duration: Mapped[int] = mapped_column(Integer, nullable=True)
    billsec: Mapped[int] = mapped_column(Integer, nullable=True)
    disposition: Mapped[str] = mapped_column(String(45), nullable=True)
    amaflags: Mapped[int] = mapped_column(Integer, nullable=True)
    accountcode: Mapped[str] = mapped_column(String(80), nullable=True)
    uniqueid: Mapped[str] = mapped_column(String(150), nullable=True)
    userfield: Mapped[str] = mapped_column(String(255), nullable=True)
