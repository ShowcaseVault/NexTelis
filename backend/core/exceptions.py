class NexTelisError(Exception):
    """Base class for domain errors raised by services."""


class NotFoundError(NexTelisError):
    pass


class ConflictError(NexTelisError):
    pass


class InvalidClaimCodeError(NexTelisError):
    pass
