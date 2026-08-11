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

**Release APK is debug-signed.** The v1 build is produced by
`scripts/build-release.sh` via `assembleDebug`. A proper release keystore and
`signingConfigs` block are not yet set up, so there is no Play-Store-ready
signed artifact.

---

## 8. Running v1

**Backend + telephony:**

```bash
docker compose up -d          # Postgres + Asterisk
python -m alembic upgrade head
uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

**Android:**

```bash
bash scripts/build-release.sh   # builds, copies to releases/ and ~/Downloads,
                                # adb-pushes to the connected device
```

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

> Written as a LinkedIn-style post. Everything below is claimed only where it
> is actually implemented — see §6 (verified) and §7 (limitations).

---

**I built a phone network that doesn't need a phone company.**

It started with a question I couldn't stop poking at: *why is calling the one
thing we never got to rebuild?*

Think about how much of messaging got reinvented. Group chats, threads,
reactions, bots, self-hosted Matrix and XMPP servers, teams running their own
Slack alternatives. Text is a solved, open, endlessly remixable space.

Now think about voice calls. Your options are basically: your carrier, or a
walled-garden app. Want to call someone? Either pay a telco for a number tied
to a SIM, or convince the other person to install the same app you have. There
is almost nothing in between — and almost nobody self-hosts a phone number the
way they self-host a chat server.

So I built **NexTelis**: a private, Internet-based telephony system where you
run the infrastructure, hand out your own numbers, and call people using the
phone dialer that's already on the device.

**The part I'm most pleased with: there's no app to open.**

This isn't another calling app with its own contact list and its own in-call
screen. NexTelis registers with Android as a *calling account* — the same
mechanism your SIM uses. Once enabled, it sits in Settings › Calling accounts
right next to your carrier, and the stock dialer simply offers it as another
way to place a call.

```text
Native dialer → Android Telecom → NexTelis → Asterisk → the other person
```

Dial a NexTelis number from the normal dialer. The native incoming-call screen
rings for inbound calls. Bluetooth routing, the in-call UI, the lock-screen
answer swipe — all the system behavior you already know, because it *is* the
system behavior. It simulates a SIM-backed service closely enough that the user
doesn't have to think "I'm making a VoIP call." They're just calling someone.

Technically it registers as a `CALL_PROVIDER` phone account rather than a
self-managed one — meaning it coexists with your real SIM instead of hijacking
it. Your actual carrier service keeps working, untouched, the whole time.

**What's under it:**

- **Android app in Kotlin** — Telecom `ConnectionService`/`Connection`
  integration for native call handling, Linphone SDK for real SIP/RTP media,
  and a foreground service so calls survive backgrounding.
- **FastAPI + Postgres control plane** — users, private numbers, device
  pairing, call authorization. It never touches audio.
- **Asterisk** as the telephony engine, reading its PJSIP endpoints straight
  out of Postgres via realtime config. DTMF is negotiated as RFC 4733, so
  tones ride the RTP stream properly rather than being faked in-band.
- **Control plane and media plane kept strictly separate** — voice never goes
  through an HTTP request. That split is the single most important
  architectural decision in the project.

**A detail I didn't expect to matter:** caller ID. If someone calls you from a
NexTelis number that isn't in your phone's contacts, Android has nothing to
show. So the app asks the backend who owns that number and injects the name
into the native call screen before it finishes ringing. Small thing. Makes it
feel real.

**And one that taught me the most:** the original device re-pairing flow let
you recover an account using only an email address. Convenient — and a
complete account-takeover hole. Anyone who knew your email could pair their
phone to your number. Fixing it properly meant a one-time recovery code,
hashed server-side, shown exactly once at registration. No email
infrastructure needed, and it still works if you lose every device you own.
I'd written it down as an "acceptable pilot tradeoff." It wasn't. Shipping a
thing called v1 with that in it would have been lying about what v1 was.

**What it isn't, honestly:**

Verified on a LAN, not the open Internet — NAT traversal is the next hard
problem. No TLS yet, so this is a trusted-network system today. And OnePlus's
OxygenOS strips out the calling-accounts settings screen entirely, so the app
registers correctly and can then never be switched on. There's no workaround —
Android deliberately offers no API to enable a calling account without the
user doing it in system Settings. Samsung One UI is the supported target.

Every one of those is written down in the docs rather than glossed over,
because the point of a v1 is to know exactly what you've proven.

**The bigger idea:** telephony is only a walled garden because we stopped
treating it as software. The tools to run your own voice network — Asterisk,
SIP, and an Android API surface that will genuinely let a third party be a
calling provider — are all sitting there, open, mature, and largely unused for
this.

Next up: native call history, contacts integration, and making it survive a
real network instead of a friendly one.

*#Android #Kotlin #Telephony #SIP #Asterisk #VoIP #OpenSource #SelfHosted*
