"""add hashed_recovery_code to users

Revision ID: a1b2c3d4e5f6
Revises: 6e5b03508905
Create Date: 2026-08-11 00:00:00.000000

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

from backend.core.security import generate_recovery_code, hash_recovery_code

# revision identifiers, used by Alembic.
revision: str = "a1b2c3d4e5f6"
down_revision: Union[str, Sequence[str], None] = "6e5b03508905"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column(
        "users",
        sa.Column("hashed_recovery_code", sa.String(length=64), nullable=True),
    )

    # Pre-existing rows (e.g. the seeded test users) never had a recovery
    # code — backfill each with a fresh one-off code. There's no way to
    # hand these to their (fixture) owners, but they're test data; a real
    # affected user would need to re-register or be issued a new code
    # out-of-band by an admin.
    connection = op.get_bind()
    user_ids = connection.execute(sa.text("SELECT id FROM users")).scalars().all()
    for user_id in user_ids:
        connection.execute(
            sa.text(
                "UPDATE users SET hashed_recovery_code = :hashed WHERE id = :id"
            ),
            {"hashed": hash_recovery_code(generate_recovery_code()), "id": user_id},
        )

    op.alter_column("users", "hashed_recovery_code", nullable=False)


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_column("users", "hashed_recovery_code")
