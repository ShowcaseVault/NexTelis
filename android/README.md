# NexTelis Android App

See [../docs/PROJECT.md](../docs/PROJECT.md), [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md),
and [../docs/FINDINGS.md](../docs/FINDINGS.md) for project context.

## Setup

1. Copy `local.properties.example` to `local.properties`.
2. Set `nextelis.api.baseUrl` to your backend server's LAN address
   (e.g. `http://192.168.1.13:8000/`). Both devices must be on the same
   Wi-Fi network as the server — see `docs/ARCHITECTURE.md` §8.
3. `local.properties` is never committed (see root `.gitignore`) — every
   developer points it at their own server.

## Where the backend URL is used

`NexTelisApiClient` (`app/src/main/java/.../network/NexTelisApiClient.kt`)
is the single place the app builds its network client. It reads
`BuildConfig.API_BASE_URL`, which Gradle generates from
`local.properties` at build time (`app/build.gradle.kts`). No other file
should hardcode a host/IP — add new endpoints to `NexTelisApi.kt` and call
them through `NexTelisApiClient.api`.

## Supported devices

Samsung (One UI) is the current target for Android Telecom integration —
see `docs/FINDINGS.md` for why OnePlus/OxygenOS is currently unsupported.
