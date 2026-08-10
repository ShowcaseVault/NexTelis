from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from backend.api.deps import CurrentDeviceDep, get_number_service
from backend.core.exceptions import ConflictError, NotFoundError
from backend.schemas.call import CallAuthorizationRequest, CallAuthorizationResponse
from backend.services.number_service import NumberService

router = APIRouter(prefix="/calls", tags=["calls"])

NumberServiceDep = Annotated[NumberService, Depends(get_number_service)]


@router.post("/authorize", response_model=CallAuthorizationResponse)
async def authorize_call(
    payload: CallAuthorizationRequest,
    _caller: CurrentDeviceDep,
    service: NumberServiceDep,
) -> CallAuthorizationResponse:
    try:
        await service.resolve_callable_number(payload.destination)
    except (NotFoundError, ConflictError) as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)
        ) from exc
    return CallAuthorizationResponse(destination=payload.destination, callable=True)
