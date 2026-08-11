# NexTelis Android App

See [../docs/PROJECT.md](../docs/PROJECT.md), [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md),
and [../docs/FINDINGS.md](../docs/FINDINGS.md) for project context.

## Setup

1. Copy `local.properties.example` to `local.properties` and set `sdk.dir`.
2. There is no build-time server config. On first launch the app shows a
   "Connect to server" screen (`ServerSetupActivity`) where the user enters
   the backend server's host/IP and port — this is stored on-device via
   `SessionStore` and can be changed later from Home ("Change server"). The
   same APK works against any NexTelis deployment; both the phone and the
   server just need to be reachable from each other (typically the same
   Wi-Fi network) — see `docs/ARCHITECTURE.md` §8.

## Where the backend URL is used

`NexTelisApiClient` (`app/src/main/java/.../network/NexTelisApiClient.kt`)
is the single place the app builds its network client. It reads the base
URL from `SessionStore.getServerBaseUrl`, which is set at runtime by
`ServerSetupActivity` — there is no `BuildConfig`/`local.properties`
fallback. No other file should hardcode a host/IP — add new endpoints to
`NexTelisApi.kt` and call them through `NexTelisApiClient.api`.

## Supported devices

Samsung (One UI) is the current target for Android Telecom integration —
see `docs/FINDINGS.md` for why OnePlus/OxygenOS is currently unsupported.
