# NexTelis — Asterisk

> **Status:** Realtime (ARA) wired up and verified locally; CDR-to-Postgres wired up, not yet
> verified against a real call (needs live SIP registration first)
> **Purpose:** Explain what Asterisk is, what it does for NexTelis, and exactly how it's configured in this repo.

---

## 1. What Asterisk actually is

Asterisk is an open-source **telephony engine** (PBX). It is not a web server or database —
it is purpose-built software for:

- **SIP** — the signaling protocol phones use to register ("I'm online, here's how to reach me")
  and to set up/tear down calls (INVITE, ACK, BYE, etc.)
- **RTP** — the actual real-time audio media stream, once a call is connected
- **Dialplan** — the logic that decides, given a dialed number, what to do (ring who, play what,
  hang up, etc.)
- **Channels/Bridges** — Asterisk's internal representation of "a leg of a call" and "two legs
  joined together"

In NexTelis, Asterisk is the **media/telephony plane**. The FastAPI backend is the **control
plane** — it owns Users/Numbers/Devices and business logic, but never touches voice packets.
See `docs/ARCHITECTURE.md` §6 for the full control-vs-media distinction.

---

## 2. Why we need it at all

We are not writing a SIP stack, a jitter buffer, an RTP mixer, or call-routing state machines by
hand. Asterisk already solves all of that reliably. NexTelis's job is just to tell Asterisk
*who exists and what number they own* — Asterisk does the rest.

---

## 3. PJSIP — the module that matters here

Asterisk ships several SIP implementations; we use **`chan_pjsip`** (the modern one — `chan_sip`
is deprecated and present in the image only because it ships with the default `asterisk` package,
we don't configure it).

PJSIP models everything as **sorcery objects**:

| Object | Meaning |
|---|---|
| `endpoint` | A SIP identity — codecs, context, which `auth`/`aor` it uses |
| `auth` | Credentials used to authenticate that endpoint (username/password) |
| `aor` ("Address of Record") | Where the endpoint's registered contacts (actual IP:port) are stored |

An incoming registration from a phone matches an `endpoint` by id, checks it against `auth`, and
stores the phone's current contact URI under the matching `aor`. An incoming call to that
endpoint's id then rings whatever contacts are currently registered under its `aor`.

---

## 4. Where we started: static config (the old way)

Originally (pre this refactor) `pjsip.conf` hardcoded two phones directly in the file:

```ini
[phone1](endpoint-template)
auth=phone1_auth
aors=phone1
...
[phone1_auth](auth-template)
username=phone1
password=secret123
```

Adding a new user meant hand-editing `pjsip.conf` and reloading `res_pjsip`. There was also a
half-built `dynamic/` directory + `inotifywait` watcher script that regenerated an `#include`d
config fragment whenever the backend dropped a `user_*.conf` file in — a working but fragile
file-based approximation of realtime, since removed.

This does not scale to "any user can self-register and get a working number immediately."

---

## 5. Where we ended up: Asterisk Realtime Architecture (ARA)

**ARA lets Asterisk query the same Postgres database the backend uses, live, instead of reading
static `.conf` files for `endpoint`/`auth`/`aor` objects.** When the backend writes a row, the
very next SIP REGISTER or INVITE sees it — no reload, no file generation, no restart.

### 5.1 The pieces, and how they connect

```text
Asterisk (res_pjsip / sorcery)
        │  "give me endpoint '9001'"
        ▼
sorcery.conf            → "endpoints are 'realtime', backed by config family 'ps_endpoints'"
        │
        ▼
extconfig.conf          → "config family 'ps_endpoints' comes from engine 'odbc', DSN 'nextelis-pgsql', table 'ps_endpoints'"
        │
        ▼
res_odbc.conf           → defines the 'nextelis-pgsql' ODBC connection Asterisk keeps pooled
        │
        ▼
/etc/odbc.ini           → the actual DSN: host/port/db/user/password (rendered from .env at container start)
        │
        ▼
/etc/odbcinst.ini       → which ODBC driver .so implements "PostgreSQL"
        │
        ▼
unixODBC → odbc-postgresql driver → Postgres (same DB the FastAPI backend uses)
```

Every layer is required — skip any one and Asterisk falls back to "no realtime engine available"
and reports zero endpoints (this happened during setup — see §8, Finding 2).

### 5.2 Config files (all in `asterisk/config/`)

| File | Role |
|---|---|
| `pjsip.conf` | Only defines the UDP transport now. No static endpoints. |
| `sorcery.conf` | Maps PJSIP's `endpoint`/`auth`/`aor` sorcery wizards to `realtime,<table>` |
| `extconfig.conf` | Maps each realtime config family (`ps_endpoints`, `ps_auths`, `ps_aors`) to `odbc,<dsn>,<table>` |
| `res_odbc.conf` | Declares the `nextelis-pgsql` ODBC connection pool (`pre-connect => yes`) |
| `odbcinst.ini` | Registers the `PostgreSQL` ODBC driver (points at `psqlodbcw.so`) |
| `odbc.ini.template` | DSN template (`Servername`/`Port`/`Database`/`UserName`/`Password` as `${VAR}`), rendered to `/etc/odbc.ini` by the entrypoint via `envsubst`, so the real Postgres password (from root `.env`) is never baked into the image or committed to git |
| `modules.conf` | Forces `res_odbc.so`, `res_config_odbc.so`, and `cdr_odbc.so` to load (see Finding 2) |
| `cdr.conf` | Enables CDR logging generally (`enable=yes`, logs unanswered calls too) |
| `cdr_odbc.conf` | Points the CDR engine at the same `nextelis-pgsql` DSN, table `cdr` |
| `extensions.conf` | Dialplan — see §6 |
| `logger.conf` | Unchanged — Asterisk log verbosity/output targets |

### 5.3 The database side (owned by the backend's Alembic migrations)

Three Postgres tables, with **exactly** the column names/types PJSIP's realtime engine expects
(these are Asterisk's schema, not NexTelis conventions — see `backend/models/pjsip_realtime.py`):

- **`ps_endpoints`** — `id` (= the extension number, e.g. `"9001"`), `context`, `allow`/`disallow`
  codecs, `auth`, `aors`, transport, etc.
- **`ps_auths`** — `id`, `auth_type=userpass`, `username`, `password` (plaintext — see §7)
- **`ps_aors`** — `id`, `max_contacts` (currently `5`), `remove_existing=yes`, `support_path=yes`

`Number.value` (e.g. `"9001"`) is reused directly as the `id`/`username`/`callerid` in all three
tables — one Number *is* one PJSIP endpoint id. There is no separate "extension" concept.

### 5.4 Who writes these rows

`NumberService.assign_number()` (`backend/services/number_service.py`) does this transactionally,
right after creating the `Number` row:

1. Generate a random SIP password (`generate_sip_password()` in `backend/core/security.py`)
2. Insert the `Number` row (with that password stored in `numbers.sip_password`)
3. Call `PjsipRealtimeRepository.upsert_for_number(extension, sip_password)`
   (`backend/repositories/pjsip_realtime_repository.py`), which inserts the matching
   `ps_endpoints`/`ps_auths`/`ps_aors` rows
4. Commit — Asterisk can now authenticate and route that extension immediately

No Asterisk restart, no config reload, no AMI call needed for this to take effect.

### 5.5 Test fixtures

Two permanent test users are seeded by Alembic migration
`eb5c725922c5_seed_two_hardcoded_test_users_for_.py` (not created through the API — inserted
directly, matching the real shape the service produces):

| User | Number/Endpoint | SIP password | Device raw token (only ever printed here) |
|---|---|---|---|
| Test User One | `9001` | `test-sip-password-7001`* | `test-device-token-phone1-fixed` |
| Test User Two | `9002` | `test-sip-password-7002`* | `test-device-token-phone2-fixed` |

*(sic — password strings kept the original `7001`/`7002` naming from before the extensions were
renumbered to `9001`/`9002` to avoid colliding with manually-created test data already in the dev
DB; cosmetic only, doesn't affect functionality.)*

Only the SHA-256 hash of each device token is stored in `devices.hashed_token` — the raw values
above exist only in the migration source, for whoever needs to simulate a claimed device in tests.

`9001`/`9002` were picked instead of the doc's illustrative `7001`/`7002` (see
`docs/ARCHITECTURE.md` §7) purely to avoid a unique-constraint collision with a number a developer
had already created by hand through the live API during backend testing.

### 5.6 CDR (call detail records)

For debugging — "did this call actually happen, who was on it, how long, did it fail" — Asterisk
writes one row per call to the `cdr` table via `cdr_odbc.so`, using the same `nextelis-pgsql` DSN
as PJSIP realtime. No new ODBC/driver setup was needed, only `cdr.conf` + `cdr_odbc.conf` +
loading `cdr_odbc.so` in `modules.conf`.

Schema (`backend/models/cdr.py`, Asterisk's standard CDR field set, not a NexTelis convention):
`id`, `calldate`, `clid`, `src`, `dst`, `dcontext`, `channel`, `dstchannel`, `lastapp`, `lastdata`,
`duration`, `billsec`, `disposition` (`ANSWERED`/`NO ANSWER`/`BUSY`/`FAILED`), `amaflags`,
`accountcode`, `uniqueid`, `userfield`.

This is intentionally the *least*-effort version: no retention policy, no indexes beyond the
primary key, no correlation back to `numbers`/`users` (`src`/`dst` are just the raw extension
strings — join manually against `numbers.value` if needed). Good enough to answer "what happened
on this call" during development; would need hardening (indexes on `src`/`dst`/`calldate`,
retention/archival, maybe a proper FK-friendly view) before being relied on for anything
production-facing like billing or analytics.

---

## 6. Dialplan (`extensions.conf`)

```ini
[phones]
exten => _X.,1,Dial(PJSIP/${EXTEN},30)
 same => n,Hangup()
```

`_X.` matches any one-or-more-digit extension. Because `Number.value` is used as-is as the PJSIP
endpoint id, `Dial(PJSIP/${EXTEN})` always resolves to the right realtime endpoint — **new numbers
work immediately with zero dialplan edits.** This replaces the old approach of one hardcoded
`exten =>` line per phone.

---

## 7. Security model — what's hashed, what isn't, and why

Two different secrets exist in this system and they're deliberately handled differently:

- **Device bearer token** (`devices.hashed_token`) — only the SHA-256 hash is ever stored; the raw
  token is shown to the app exactly once, at claim time, and is used by our own backend to verify
  `Authorization: Bearer` headers. Nothing reads it back in plaintext, ever, so hashing is correct
  and cheap.
- **SIP password** (`numbers.sip_password` / `ps_auths.password`) — stored **in plaintext**. This
  is not an oversight: PJSIP's `userpass` digest authentication requires Asterisk to compute an
  MD5 challenge/response using the actual shared secret on every REGISTER/INVITE. A one-way hash
  can't be used for that math — this is the same reason every traditional SIP/PBX system stores
  SIP secrets in cleartext. It's protected by DB access control, not by hashing, and it is never
  exposed through any backend API.

AMI (`ASTERISK_AMI_*` in `.env`) is configured but **completely unused** — no code anywhere calls
it. It would become relevant if the backend ever needs to *originate calls itself*, subscribe to
live call/channel events, or query live registration state beyond what's already in Postgres —
none of which is needed for basic register-and-dial, which works without it.

---

## 8. Findings from wiring this up (things that weren't obvious going in)

1. **`network_mode: host` on the Asterisk container means it reaches Postgres at
   `127.0.0.1:<POSTGRES_PORT>`, not the compose service name `postgres`.** Host networking takes
   Asterisk off the compose bridge network entirely, so Docker's internal DNS doesn't apply to it.
   Since Postgres also publishes to the host on `POSTGRES_PORT` (5433), `Servername=${POSTGRES_HOST}`
   (`localhost`) in the ODBC DSN happens to work correctly for exactly this reason.

2. **`res_odbc.so` loading does *not* imply `res_config_odbc.so` is loaded.** These are two
   separate modules: `res_odbc` only manages the pooled ODBC connection itself (and it loaded and
   connected fine — `odbc show all` showed an active connection); `res_config_odbc` is the bridge
   that actually lets Asterisk's config/sorcery system query through that connection. Without it,
   every realtime lookup silently returns "no rows found" even though the DB connection is healthy
   and the data is really there — the actual error only shows up in the Asterisk log as:
   `Realtime mapping for 'ps_endpoints' found to engine 'odbc', but the engine is not available`.
   It wasn't autoloading on this Ubuntu 22.04 asterisk package by default, so `modules.conf` now
   force-loads it explicitly. This was the one real bug hit during setup — worth remembering if
   realtime ever silently "stops working" again after an image rebuild.

3. **ODBC driver paths are architecture-specific.** `odbc-postgresql` on Ubuntu 22.04 only ships
   `psqlodbcw.so` under `/usr/lib/x86_64-linux-gnu/odbc/` — confirmed by installing the package in
   a throwaway container and checking `dpkg -L odbc-postgresql`. If this ever needs to run on
   arm64 (e.g. Apple Silicon Docker, Raspberry Pi), `odbcinst.ini`'s `Driver=` path will need a
   matching `aarch64-linux-gnu` entry, or `Driver64=`.

4. **Storing the DB password in the image was avoided on purpose.** The first draft of
   `odbc.ini` had literal credentials in a file meant to be `COPY`'d into the image (and
   potentially committed to git). Instead, `odbc.ini.template` (with `${POSTGRES_PASSWORD}`
   placeholders) is committed, and the entrypoint renders the real `/etc/odbc.ini` from live
   environment variables at container start via `envsubst` — the actual secret only ever exists
   inside the running container, sourced from the gitignored root `.env`.

5. **`pg_hba.conf` in the stock `postgres:16` image trusts `127.0.0.1/32` and `::1/128`
   unconditionally** (`host all all 127.0.0.1/32 trust`), regardless of the password in
   `odbc.ini`/`DATABASE_URL`. This isn't something we configured — it's Postgres's own container
   default — but it's worth knowing: on this box, any process that can reach `127.0.0.1:5433` can
   authenticate as any Postgres role without a correct password. Fine for a single-developer local
   setup; would need real `pg_hba.conf` hardening (or at minimum binding Postgres off `0.0.0.0`)
   before this ever runs on a shared or internet-facing host.

6. **Verifying "realtime is really live," not just "config parsed,"** requires checking the CLI
   output, not just that Asterisk starts cleanly:
   - `asterisk -rx "odbc show all"` — confirms the ODBC pool connected (this alone is *not* proof
     realtime works — see Finding 2)
   - `asterisk -rx "realtime load ps_endpoints id <value>"` — directly exercises the realtime
     engine and prints the row Asterisk actually sees, or the "engine is not available" error
   - `asterisk -rx "pjsip show endpoints"` / `pjsip show endpoint <id>` — confirms PJSIP itself
     resolved the sorcery object end-to-end, with full merged config (defaults + realtime overrides)

---

## 9. What's intentionally *not* done yet

- No TLS/SRTP — signaling and media are plaintext UDP, consistent with `docs/ARCHITECTURE.md` §9
  ("Advanced security will be developed later")
- No AMI usage (see §7)
- No dialplan beyond "ring the matching PJSIP endpoint" — no voicemail, no IVR, no call forwarding
- `max_contacts=5` per AOR is a fixed constant (`MAX_CONTACTS_PER_NUMBER` in
  `pjsip_realtime_repository.py`), not user-configurable

These are all reasonable v1 gaps, not oversights — see `docs/ROADMAP.md` for where they're
expected to land.
