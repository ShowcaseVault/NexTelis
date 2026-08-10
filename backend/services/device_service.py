from datetime import UTC, datetime

from backend.core.exceptions import InvalidClaimCodeError
from backend.core.security import generate_device_token, hash_device_token
from backend.models.device import Device
from backend.repositories.claim_code_repository import ClaimCodeRepository
from backend.repositories.device_repository import DeviceRepository
from backend.schemas.device import DeviceClaimRequest, DeviceClaimResponse


class DeviceService:
    def __init__(
        self,
        device_repository: DeviceRepository,
        claim_code_repository: ClaimCodeRepository,
    ) -> None:
        self.device_repository = device_repository
        self.claim_code_repository = claim_code_repository

    async def claim_device(self, payload: DeviceClaimRequest) -> DeviceClaimResponse:
        claim_code = await self.claim_code_repository.get_by_code(payload.claim_code)
        if claim_code is None:
            raise InvalidClaimCodeError("claim code not found")
        if claim_code.consumed_at is not None:
            raise InvalidClaimCodeError("claim code already used")
        if claim_code.expires_at < datetime.now(UTC):
            raise InvalidClaimCodeError("claim code expired")

        raw_token = generate_device_token()
        device = self.device_repository.add(
            Device(
                name=payload.device_name,
                push_token=payload.push_token,
                hashed_token=hash_device_token(raw_token),
                user_id=claim_code.user_id,
            )
        )
        claim_code.consumed_at = datetime.now(UTC)
        await self.device_repository.commit()

        return DeviceClaimResponse(device=device, device_token=raw_token)

    async def authenticate(self, raw_token: str) -> Device | None:
        return await self.device_repository.get_by_hashed_token(
            hash_device_token(raw_token)
        )
