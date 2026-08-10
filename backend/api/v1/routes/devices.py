from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from backend.api.deps import get_device_service
from backend.core.exceptions import InvalidClaimCodeError
from backend.schemas.device import DeviceClaimRequest, DeviceClaimResponse
from backend.services.device_service import DeviceService

router = APIRouter(prefix="/devices", tags=["devices"])

DeviceServiceDep = Annotated[DeviceService, Depends(get_device_service)]


@router.post(
    "/claim", response_model=DeviceClaimResponse, status_code=status.HTTP_201_CREATED
)
async def claim_device(
    payload: DeviceClaimRequest, service: DeviceServiceDep
) -> DeviceClaimResponse:
    try:
        return await service.claim_device(payload)
    except InvalidClaimCodeError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)
        ) from exc
