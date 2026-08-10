"""seed two hardcoded test users for asterisk testing

Revision ID: eb5c725922c5
Revises: de97b38e1af6
Create Date: 2026-08-10 22:57:04.672641

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "eb5c725922c5"
down_revision: Union[str, Sequence[str], None] = "de97b38e1af6"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

# Fixed test fixtures — same UUIDs/tokens every time this migration runs,
# so integration tests and manual curl/SIP-client testing can rely on them.
# Device raw tokens are only ever known here; devices table stores only the
# sha256 hash (see backend/core/security.py hash_device_token).
TEST_USERS = [
    {
        "user_id": "00000000-0000-0000-0000-000000000001",
        "email": "test.user1@nextelis.local",
        "display_name": "Test User One",
        "number_id": "00000000-0000-0000-0000-000000000011",
        "number_value": "9001",
        "sip_password": "test-sip-password-7001",
        "device_id": "00000000-0000-0000-0000-000000000021",
        "device_name": "Test Device One",
        # sha256("test-device-token-phone1-fixed")
        "device_hashed_token": "866ffdcc84fffe9de63133ed3226feabad64e85756b5cd44cb470465c079c5c",
    },
    {
        "user_id": "00000000-0000-0000-0000-000000000002",
        "email": "test.user2@nextelis.local",
        "display_name": "Test User Two",
        "number_id": "00000000-0000-0000-0000-000000000012",
        "number_value": "9002",
        "sip_password": "test-sip-password-7002",
        "device_id": "00000000-0000-0000-0000-000000000022",
        "device_name": "Test Device Two",
        # sha256("test-device-token-phone2-fixed")
        "device_hashed_token": "fdf8f887f5abf8c3e06f5e39c5f48f31afed9460632537f5797fd8d6bf14fed",
    },
]

MAX_CONTACTS_PER_NUMBER = 5


def upgrade() -> None:
    """Upgrade schema."""
    for fixture in TEST_USERS:
        op.execute(
            sa.text("""
                INSERT INTO users (id, email, display_name, is_active)
                VALUES (CAST(:user_id AS uuid), :email, :display_name, true)
                """).bindparams(
                user_id=fixture["user_id"],
                email=fixture["email"],
                display_name=fixture["display_name"],
            )
        )
        op.execute(
            sa.text("""
                INSERT INTO numbers (id, value, is_active, user_id, sip_password)
                VALUES (CAST(:number_id AS uuid), :number_value, true, CAST(:user_id AS uuid), :sip_password)
                """).bindparams(
                number_id=fixture["number_id"],
                number_value=fixture["number_value"],
                user_id=fixture["user_id"],
                sip_password=fixture["sip_password"],
            )
        )
        op.execute(
            sa.text("""
                INSERT INTO devices (id, name, is_active, hashed_token, user_id)
                VALUES (CAST(:device_id AS uuid), :device_name, true, :device_hashed_token, CAST(:user_id AS uuid))
                """).bindparams(
                device_id=fixture["device_id"],
                device_name=fixture["device_name"],
                device_hashed_token=fixture["device_hashed_token"],
                user_id=fixture["user_id"],
            )
        )
        op.execute(sa.text("""
                INSERT INTO ps_endpoints (
                    id, transport, aors, auth, context, disallow, allow,
                    direct_media, dtmf_mode, rtp_symmetric, force_rport,
                    rewrite_contact, callerid
                )
                VALUES (
                    :extension, 'transport-udp', :extension, :extension, 'phones',
                    'all', 'ulaw,alaw', 'no', 'rfc4733', 'yes', 'yes', 'yes', :extension
                )
                """).bindparams(extension=fixture["number_value"]))
        op.execute(
            sa.text("""
                INSERT INTO ps_auths (id, auth_type, username, password)
                VALUES (:extension, 'userpass', :extension, :sip_password)
                """).bindparams(
                extension=fixture["number_value"],
                sip_password=fixture["sip_password"],
            )
        )
        op.execute(
            sa.text("""
                INSERT INTO ps_aors (id, max_contacts, remove_existing, support_path)
                VALUES (:extension, :max_contacts, 'yes', 'yes')
                """).bindparams(
                extension=fixture["number_value"],
                max_contacts=MAX_CONTACTS_PER_NUMBER,
            )
        )


def downgrade() -> None:
    """Downgrade schema."""
    for fixture in TEST_USERS:
        op.execute(
            sa.text("DELETE FROM ps_aors WHERE id = :extension").bindparams(
                extension=fixture["number_value"]
            )
        )
        op.execute(
            sa.text("DELETE FROM ps_auths WHERE id = :extension").bindparams(
                extension=fixture["number_value"]
            )
        )
        op.execute(
            sa.text("DELETE FROM ps_endpoints WHERE id = :extension").bindparams(
                extension=fixture["number_value"]
            )
        )
        op.execute(
            sa.text(
                "DELETE FROM devices WHERE id = CAST(:device_id AS uuid)"
            ).bindparams(device_id=fixture["device_id"])
        )
        op.execute(
            sa.text(
                "DELETE FROM numbers WHERE id = CAST(:number_id AS uuid)"
            ).bindparams(number_id=fixture["number_id"])
        )
        op.execute(
            sa.text("DELETE FROM users WHERE id = CAST(:user_id AS uuid)").bindparams(
                user_id=fixture["user_id"]
            )
        )
