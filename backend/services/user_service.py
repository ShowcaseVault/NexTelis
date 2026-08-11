import uuid
from datetime import UTC, datetime, timedelta

from backend.core.exceptions import InvalidClaimCodeError, ConflictError, NotFoundError
from backend.core.security import (
    generate_claim_code,
    generate_recovery_code,
    hash_recovery_code,
)
from backend.models.claim_code import ClaimCode
from backend.models.user import User
from backend.repositories.claim_code_repository import ClaimCodeRepository
from backend.repositories.user_repository import UserRepository
from backend.schemas.user import UserCreate, UserRegisteredResponse, UserWithClaimCode

CLAIM_CODE_TTL = timedelta(minutes=15)


class UserService:
    def __init__(
        self,
        user_repository: UserRepository,
        claim_code_repository: ClaimCodeRepository,
    ) -> None:
        self.user_repository = user_repository
        self.claim_code_repository = claim_code_repository

    async def register_user(self, payload: UserCreate) -> UserRegisteredResponse:
        existing = await self.user_repository.get_by_email(payload.email)
        if existing is not None:
            raise ConflictError(f"a user with email {payload.email!r} already exists")

        recovery_code = generate_recovery_code()
        user = self.user_repository.add(
            User(
                email=payload.email,
                display_name=payload.display_name,
                hashed_recovery_code=hash_recovery_code(recovery_code),
            )
        )
        await self.user_repository.flush()

        issued = await self._issue_claim_code(user)
        return UserRegisteredResponse(
            user=issued.user,
            claim_code=issued.claim_code,
            claim_code_expires_at=issued.claim_code_expires_at,
            recovery_code=recovery_code,
        )

    async def get_user(self, user_id: uuid.UUID) -> User:
        user = await self.user_repository.get(user_id)
        if user is None:
            raise NotFoundError(f"user {user_id} not found")
        return user

    async def reissue_claim_code(self, email: str, recovery_code: str) -> UserWithClaimCode:
        """Re-pair an existing user to a new device (e.g. after a reinstall
        or losing the original device). Requires the recovery code issued
        at registration — email alone is not proof of ownership."""
        user = await self.user_repository.get_by_email(email)
        if user is None:
            raise NotFoundError(f"no user with email {email!r}")

        if hash_recovery_code(recovery_code) != user.hashed_recovery_code:
            raise InvalidClaimCodeError("invalid recovery code")

        return await self._issue_claim_code(user)

    async def _issue_claim_code(self, user: User) -> UserWithClaimCode:
        claim_code = self.claim_code_repository.add(
            ClaimCode(
                code=generate_claim_code(),
                expires_at=datetime.now(UTC) + CLAIM_CODE_TTL,
                user_id=user.id,
            )
        )
        await self.claim_code_repository.commit()

        return UserWithClaimCode(
            user=user,
            claim_code=claim_code.code,
            claim_code_expires_at=claim_code.expires_at,
        )
