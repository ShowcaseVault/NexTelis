"""add qualify_frequency to ps_aors

Revision ID: b2c3d4e5f6a7
Revises: a1b2c3d4e5f6
Create Date: 2026-08-11 00:00:00.000000

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

from backend.repositories.pjsip_realtime_repository import QUALIFY_FREQUENCY_SECONDS

# revision identifiers, used by Alembic.
revision: str = "b2c3d4e5f6a7"
down_revision: Union[str, Sequence[str], None] = "a1b2c3d4e5f6"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # Without qualify_frequency Asterisk never OPTIONS-pings contacts, so
    # they sit as "NonQual" forever and dead ones are never removed. Since
    # each app restart binds a new UDP port (a new contact rather than a
    # refresh), stale registrations accumulate to max_contacts and inbound
    # calls fork to devices that are long gone.
    op.add_column(
        "ps_aors",
        sa.Column("qualify_frequency", sa.Integer(), nullable=True),
    )

    # Existing AORs were provisioned before this column existed — backfill
    # them so already-registered numbers start being qualified without
    # needing to be re-provisioned.
    op.execute(
        sa.text("UPDATE ps_aors SET qualify_frequency = :freq").bindparams(
            freq=QUALIFY_FREQUENCY_SECONDS
        )
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_column("ps_aors", "qualify_frequency")
