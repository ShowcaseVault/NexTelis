# NexTelis

**NexTelis** is an experimental **private, Internet-based telephony system**.

The idea: give users their own private phone numbers and let them call each
other using the **native Android dialer, contacts, and in-call experience**
— not a separate WhatsApp/Viber-style app. Android's Telecom framework
handles the UI; NexTelis and Asterisk handle the actual call.

```text
Android Dialer → Android Telecom → NexTelis → Asterisk → Internet → other NexTelis user
```

The Internet is treated purely as a transport layer for the MVP — this is
not an attempt to build a private cellular network (see [Open5GS /
private-network notes](docs/PROJECT.md#11-open5gs) for why that's explicitly
out of scope right now).

## What's in this repo

```text
nextelis/
├── backend/     FastAPI control-plane service — users, devices, numbers, auth
├── android/     Android app — pairing, permissions, Telecom integration, SIP calling
├── asterisk/    Asterisk config (PJSIP realtime via Postgres/ODBC)
├── docs/        Living project docs (vision, architecture, roadmap, findings)
├── releases/    Local APK builds (not committed — see GitHub Releases instead)
└── tests/
```

* **backend/** — FastAPI service that owns users, private numbers, device
  pairing/auth, and hands out SIP credentials for Asterisk. Does **not**
  carry voice media — see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for
  the control-plane/media-plane split.
* **android/** — The NexTelis Android app: pairs a device to a NexTelis
  account, requests the Android Telecom permissions needed to register as a
  calling provider, and places/receives real SIP/RTP calls (via the Linphone
  SDK) that surface through Android's native dialer.
* **asterisk/** — Asterisk is the telephony engine: SIP registration, call
  routing, and RTP media. Configured with PJSIP realtime, reading endpoints
  straight from the backend's Postgres database.
* **docs/** — Start with [PROJECT.md](docs/PROJECT.md) for the why,
  [ARCHITECTURE.md](docs/ARCHITECTURE.md) for the how,
  [ROADMAP.md](docs/ROADMAP.md) for what's next, and
  [FINDINGS.md](docs/FINDINGS.md) for what we've actually tested and learned
  on real devices (including OEM/Android-version quirks).

## Status

Early/experimental (Pilot v0). Verified so far:

* Two-way SIP calling between real Android devices through a local Asterisk
  instance (PJSIP realtime, Postgres-backed).
* Android Telecom integration on stock-ish ROMs (Samsung One UI) — NexTelis
  shows up as a calling account alongside the SIM, toggled from system
  Settings, without disrupting the real SIM service. Heavily customized ROMs
  (e.g. OxygenOS) can hide this UI entirely — see FINDINGS.md.
* Full app onboarding flow: register → pair device → grant permissions → get
  assigned a number → home screen with a one-tap enable/disable toggle.

Not yet hardened: authentication/security is intentionally minimal for the
pilot stage (see FINDINGS.md for the specific tradeoffs accepted and what
must change before any real launch).

## Running it

* **Backend:** see [backend/](backend) — FastAPI app, `docker-compose.yaml`
  brings up Postgres + Asterisk, Alembic manages migrations.
* **Android:** see [android/README.md](android/README.md) for local config
  (the backend base URL is set via `android/local.properties`, not
  hardcoded).
* **Prebuilt APKs:** attached to [GitHub
  Releases](../../releases) rather than committed to the repo.
