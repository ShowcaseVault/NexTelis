# NexTelis v1 — Private Telephony MVP

> **Status:** Delivered
> **Scope:** The v1 target defined in [ROADMAP.md](ROADMAP.md) — a person can
> register, receive a NexTelis number, register their Android device, call
> another NexTelis user, and receive a call from one.
>
> This document is the record of what v1 actually is: what was built, what was
> verified on real hardware, and what is explicitly *not* done yet.

---

## 1. What v1 delivers

The v1 exit criteria from the roadmap, and how each is met:

| # | v1 target | Status | Implementation |
|---|---|---|---|
| 1 | Register | ✅ | `POST /api/v1/users` → user + one-time claim code + one-time recovery code |
| 2 | Receive a NexTelis number | ✅ | `POST /api/v1/users/{id}/number` → assigns a unique number and provisions PJSIP realtime credentials |
| 3 | Register their Android device | ✅ | `POST /api/v1/devices/claim` → consumes the claim code, mints a device bearer token |
| 4 | Call another NexTelis user | ✅ | Native dialer → Android Telecom → `NexTelisConnectionService` → Linphone SIP → Asterisk → callee |
| 5 | Receive a call from another user | ✅ | Asterisk → SIP INVITE → Linphone → `NexTelisConnectionService` → native incoming-call UI |

Beyond the literal target, v1 also ships the backend responsibilities the
roadmap listed under "Backend": user registration, number assignment, device
registration, call authorization (`POST /api/v1/calls/authorize`), and basic
call records (CDR table, written by Asterisk into Postgres).

---

## 2. System shape

```text
Android device                       Linux host
┌────────────────────────┐          ┌───────────────────────────────┐
│ Native dialer          │          │ NexTelis backend (FastAPI)    │
│   ↓                    │  HTTPS   │  users / numbers / devices    │
│ Android Telecom        │─────────▶│  auth / call authorization    │
│   ↓                    │ control  └──────────────┬────────────────┘
│ NexTelisConnection-    │                         │ shares Postgres
│ Service                │                         ▼
│   ↓                    │          ┌───────────────────────────────┐
│ Linphone SDK (SIP/RTP) │◀────────▶│ Asterisk (PJSIP realtime)     │
└────────────────────────┘  media   │  registration / routing / RTP │
                                    └───────────────────────────────┘
```

The control plane (HTTP) and media plane (SIP/RTP) stay separate, per
[ARCHITECTURE.md](ARCHITECTURE.md) §6. The backend never touches audio; it
writes PJSIP endpoint rows that Asterisk reads directly from Postgres.

---

## 3. Backend surface

All routes are under `/api/v1`. Device-authenticated routes take a
`Authorization: Bearer <device_token>` header.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/users` | — | Register. Returns user, claim code, **and the one-time recovery code**. |
| `GET` | `/users/{user_id}` | — | Read a user. |
| `POST` | `/users/claim-code` | recovery code | Re-pair an existing account to a new device. |
| `POST` | `/devices/claim` | claim code | Exchange a claim code for a device token. |
| `POST` | `/users/{user_id}/number` | device | Assign a number (idempotent — returns the existing one if already assigned). |
| `GET` | `/users/{user_id}/number` | device | Fetch the assigned number + SIP password. |
| `GET` | `/numbers/{value}` | device | Caller-ID lookup: number → display name. |
| `POST` | `/calls/authorize` | device | Check a destination number is callable before dialing. |

### Data model

```text
User ──1:1── Number ──provisions──▶ ps_endpoints / ps_auths / ps_aors
 │                                   (PJSIP realtime, read by Asterisk)
 ├──1:N── Device        (name, hashed bearer token)
 └──1:N── ClaimCode     (short-lived, single-use, 15 min TTL)

Cdr                     (written by Asterisk, read for call history)
```

All entities soft-delete via `deleted_at` on the shared `CommonModel` base.

---

## 4. Security model as shipped

v1 is not the "Security" milestone (that is v3), but two properties were
hardened during v1 because shipping without them would have been misleading:

**Device tokens.** Generated with `secrets.token_urlsafe`, stored only as a
SHA-256 hash, returned in plaintext exactly once at claim time. A stolen
database yields no usable device tokens.

**Recovery codes.** The original re-pair flow (`POST /users/claim-code`)
accepted an email alone — meaning anyone who knew a registered email address
could mint a claim code and pair their own device to that account. This was a
real account-takeover vector, documented as an accepted pilot tradeoff, and it
is **fixed in v1**:

* Registration now issues a one-time recovery code (24 random bytes,
  url-safe), shown to the user exactly once and stored only as a SHA-256 hash.
* `POST /users/claim-code` requires `{email, recovery_code}`. A wrong code
  returns `400`, not a claim code.
* The Android onboarding screen shows the code with a copy button and a
  "I've saved my recovery code" checkbox that gates the pairing button, and
  prompts for it when registration returns `409` (email already registered).

This deliberately avoids depending on email-delivery infrastructure, which
NexTelis does not have, while still requiring proof of ownership. It also
preserves the "lost my only phone" recovery case, which a scheme requiring an
existing paired device would have broken.

What is still **not** done (deferred to v3): TLS on the control plane, SIP over
TLS / SRTP media encryption, rate limiting, and abuse prevention.

---

## 5. Android app

Onboarding is a linear flow, each step gated on the previous one:

```text
ServerSetupActivity   enter backend host/port (no hardcoded IP; any APK,
      ↓               any deployment)
PairDeviceActivity    register → save recovery code → claim device
      ↓
PermissionsActivity   request Telecom + phone permissions
      ↓
GetNumberActivity     assign/fetch the NexTelis number
      ↓
HomeActivity          master enable/disable toggle, number display,
                      navigation drawer (Home / Account)
```

**Telecom integration.** `PhoneAccountManager` registers a `PhoneAccount` so
NexTelis appears in Settings › Calling accounts alongside the SIM. Registration
is a no-op when already registered — calling it unconditionally silently resets
the user's enable toggle (see [FINDINGS.md](FINDINGS.md)).

**SIP.** `SipManager` drives a Linphone `Core`, registering against Asterisk
with the credentials from `GET /users/{id}/number`. `SipCallService` runs it as
a `FOREGROUND_SERVICE_TYPE_PHONE_CALL` foreground service so calls survive
backgrounding.

**Caller ID.** Incoming calls from numbers not in the phone's contacts are
resolved through `GET /numbers/{value}` and applied via
`Connection.setCallerDisplayName()`. Results are cached in-memory; a failed
lookup degrades silently to showing the raw number.

---

## 6. Verified on real hardware

* Two-way voice calls between two physical Android phones, routed through a
  local Asterisk instance over Wi-Fi, with audio in both directions.
* NexTelis registering as a calling account on Samsung One UI and appearing in
  the native dialer's "call using" chooser, without disrupting the real SIM.
* Full onboarding flow end-to-end on a clean install.
* Re-pair flow: uninstall → reinstall → same email → recovery-code prompt →
  successful re-pair to the same number.
* Recovery-code enforcement verified directly against the API: wrong code →
  `400 invalid recovery code`; correct code → `200` with a fresh claim code.

---

## 7. Known limitations

These are real and deliberate. v1 is an MVP, not a launch.

**OnePlus / OxygenOS is unsupported.** The ROM replaces the AOSP calling-accounts
settings screen with one that has no per-account toggle, so NexTelis can be
registered but never enabled. There is no third-party workaround — Android
intentionally provides no API to enable a `PhoneAccount` programmatically.
Samsung One UI is the supported target. Details in [FINDINGS.md](FINDINGS.md).

**Asterisk CDR writing is unverified against a real call.** The `cdr` table and
migration exist and Asterisk is configured to write to it, but this has not been
confirmed end-to-end by placing a call and observing the row. Call history in
the app should be treated as unproven until it is.

**Linphone SDK is AGPLv3.** Fine for this stage. If NexTelis is ever distributed
closed-source or commercially, AGPLv3 requires full source disclosure to users —
a commercial license from Belledonne Communications would be needed instead.
This must be resolved before any closed-source launch.

**No TLS anywhere.** Control-plane HTTP and SIP/RTP are both unencrypted. This
is acceptable on a trusted LAN and unacceptable on the open Internet. v3.

**Single-host deployment.** One Linux box runs Postgres, Asterisk, and the
backend. No HA, no load balancing, no monitoring. v5.

**Local-network scope.** Calls have been verified on a shared Wi-Fi network.
NAT traversal, STUN/TURN, and Wi-Fi↔mobile-data handoff are untouched. v4.

**Distribution is sideload-only.** The release APK is properly signed (v2
scheme, RSA-4096, valid to 2053), but NexTelis is not on Google Play and has
no update mechanism — installing a new version means sideloading it. The
signing key is a self-managed local keystore; if it is ever lost, existing
installs cannot be upgraded in place.

---

## 8. Running v1

**Backend + telephony:**

```bash
make up      # Postgres, Asterisk, backend, and the project site on :8080
make check   # SIP endpoints, live registrations, channels, uptime
```

Migrations run automatically as the backend container starts. `make help`
lists every target. To run the backend on the host instead (for a debugger or
reload-on-save), use `make dev` — but stop the container first, since both
bind port 8000.

**Android:**

```bash
bash scripts/build-release.sh             # debug build, for testing
bash scripts/build-release.sh --release   # signed release build
```

Both copy the APK to `releases/` and adb-push it to a connected device.

Release builds need `android/keystore.properties` (gitignored), holding
`storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. The keystore
itself is also gitignored. **Both are unrecoverable if lost** — without them
no future build can upgrade an existing install, so back them up somewhere
outside the repo. Note that PKCS12 keystores do not support a key password
distinct from the store password.

On first launch the app asks for the backend host/port. Both phone and server
must be reachable from each other — typically the same Wi-Fi network.

---

## 9. What comes next

v1 answers "can this work at all as a service?" — yes, on a LAN, on supported
hardware. The next questions, in roadmap order, are v2 (deeper native Android
integration — call history, contacts), v3 (security: TLS, SIP-TLS, SRTP, abuse
prevention), and v4 (reliability on real networks: NAT traversal, network
transitions). See [ROADMAP.md](ROADMAP.md).

---

## 10. Summary

> Written as a LinkedIn-style post.

I built a phone network that does not need a phone company.

The idea started with a simple question. Why did we rebuild chat a hundred
times but never rebuild calling? Text messaging has group chats, threads,
bots, and plenty of servers you can host yourself. Voice calling has two
options. Pay a carrier for a number tied to a SIM, or get everyone you know
to install the same app you use. There is almost nothing in between.

So I built NexTelis. You run the server, you hand out your own numbers, and
people call each other using the dialer that is already on their phone.

The part I like most is that there is no app to open. NexTelis registers with
Android as a calling account, the same way your SIM does. Once you turn it on
in settings, it sits next to your carrier and the normal dialer just offers it
as another way to place a call. You dial a NexTelis number from the usual
dialer. The usual incoming call screen rings. Bluetooth, the in call screen,
the lock screen answer swipe all work, because it is the real system UI and
not a copy of it. It behaves close enough to a SIM service that you stop
thinking about it being an internet call.

It runs as a call provider account rather than a self managed one, which means
it sits beside your real SIM instead of taking over. Your carrier service keeps
working the whole time.

What is under it:

The Android app is Kotlin. It uses the Android Telecom APIs for native call
handling, the Linphone SDK for real SIP and RTP audio, and a foreground service
so calls survive the app going to the background.

The backend is FastAPI and Postgres. It handles users, numbers, device pairing,
and call permission. It never touches audio.

Asterisk is the call engine. It reads its SIP endpoints straight out of Postgres.
DTMF is negotiated as RFC 4733, so keypad tones travel in the audio stream the
proper way instead of being faked.

Control and audio stay on separate paths. Voice never goes through an HTTP
request. That was the most important design decision in the whole project.

One small detail that mattered more than expected was caller ID. If someone
calls from a NexTelis number that is not saved in your contacts, Android has
no name to show. So the app asks the server who owns that number and puts the
name on the call screen while it is still ringing. Small thing, but it makes it
feel real.

One thing taught me a lesson. The first version let you recover an account
using only an email address. Convenient, and also a complete security hole,
because anyone who knew your email could pair their own phone to your number.
I had written it down as an acceptable tradeoff for an early build. It was not.
Fixing it meant a one time recovery code, hashed on the server, shown once when
you register. No email system needed, and it still works if you lose every
device you own.

Being honest about what it is not. It has been tested on a local network, not
the open internet, so NAT traversal is the next hard problem. There is no TLS
yet, so this is a trusted network system for now. And OnePlus phones running
OxygenOS remove the calling accounts settings screen completely, so the app
registers fine and can then never be switched on. There is no way around it,
because Android deliberately has no API to enable a calling account without the
user doing it themselves in settings. Samsung is the supported target for now.

All of that is written in the docs rather than hidden, because the point of a
first version is knowing exactly what you have proven.

The bigger idea is that calling is only a closed system because we stopped
treating it as software. Asterisk, SIP, and an Android API that genuinely lets
an outside app be a calling provider are all sitting there, open and mature,
and hardly anyone uses them this way.

Next up is native call history, contacts integration, and making it survive a
real network instead of a friendly one.

#Android #Kotlin #Telephony #SIP #Asterisk #VoIP #OpenSource #SelfHosted

