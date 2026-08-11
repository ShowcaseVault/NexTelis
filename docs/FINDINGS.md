# NexTelis — Experimental Findings

> Companion to [ROADMAP.md](ROADMAP.md). Each entry records what we tested,
> on which device, and what we learned — per Roadmap's
> "Experiment → Prototype → Understand limitations → Document" cycle.

---

## Android Telecom / PhoneAccount enablement

### Finding: PhoneAccount registration requires manual user enablement

`TelecomManager.registerPhoneAccount()` registers the account, but Android
leaves it **disabled** until the user explicitly enables it in system
Settings. There is no public API to enable a `PhoneAccount` programmatically
from the app itself — this is by design, so a malicious app can't silently
hijack call routing.

The standard system entry point is:

```text
Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
```

which should resolve to `com.android.phone.settings.PhoneAccountSettingsActivity`
(AOSP) and show a screen listing all registered call-provider accounts
(SIM, NexTelis, etc.) with per-account toggles.

### Device results

| Device | Android / ROM | Result |
|---|---|---|
| Samsung Galaxy A50 | Android 11, One UI | **Works.** Settings > Calling accounts lists NexTelis alongside the SIM; toggling it on makes NexTelis appear in the native dialer's "call using" chooser. Real SIM account unaffected. |
| OnePlus Nord (AC2003) | Android 12 (API 31), OxygenOS/ColorOS | **Blocked by OEM.** `ACTION_CHANGE_PHONE_ACCOUNTS` still resolves to the AOSP `PhoneAccountSettingsActivity` component name, but OnePlus has replaced its implementation with `OplusCallFeaturesSetting` — a "Call" settings screen containing only *Answer/End calls*, *Advanced settings*, and a link to *SIM info & settings*. **No per-account toggle list is present anywhere in this screen.** Confirmed via `uiautomator dump` — the account chooser UI does not exist on this ROM, it isn't just relabeled/nested deeper. |

Confirmed via `adb shell dumpsys telecom`: on the Nord, NexTelis's
`PhoneAccount` registers successfully (`CallProvider` capability present)
but shows `[X]` (disabled), and the real SIM account
(`com.android.phone/.../TelephonyConnectionService`, `SimSub` capability)
remains untouched and fully functional throughout testing.

### Why this matters for the roadmap

Roadmap v0.5 asks: *"Can NexTelis register as a calling provider and
receive/initiate calls from the native dialer?"* — the answer is now known
to be **device/OEM-dependent**, not just Android-version-dependent:

* Stock-Settings-like ROMs (Samsung One UI, and presumably stock
  AOSP/Pixel) expose the toggle and the flow works end-to-end for
  registration/enablement.
* Heavily customized ROMs (OxygenOS/ColorOS on this OnePlus unit) can
  remove the standard toggle UI entirely, with no in-app or Settings-app
  workaround available to a third-party app. There is intentionally no
  public API to bypass this — Android's design requires explicit,
  user-driven consent through system UI for call-routing changes.

**Decision:** Samsung (One UI) is the primary supported/target device for
Pilot v0 and v1 development. OnePlus/OxygenOS (and likely other heavily
customized ROMs) should be treated as **unsupported** until/unless an
OEM-specific workaround is found (e.g. becoming the default dialer, which
carries its own tradeoffs and was not attempted here). Do not spend further
effort chasing OnePlus-specific UI paths as part of the MVP.

---

## Crash: `SecurityException` on Android 12+ reading `PhoneAccount`

`TelecomManager.getPhoneAccount()` enforces `READ_PHONE_NUMBERS` on API 31+
(observed on the OnePlus Nord, API 31). The app's original permission list
only requested `READ_PHONE_STATE` on API < 31 and requested nothing
equivalent on 31+, causing a `SecurityException` crash loop on first launch
on any Android 12+ device.

**Fix applied:** request `READ_PHONE_NUMBERS` on API 31+, and wrap all
`TelecomManager` reads in `PhoneAccountManager` with a `SecurityException`
catch so a missing/denied permission degrades to "not enabled" instead of
crashing the app, on any current or future Android version/OEM variant.

---

## SIP/RTP client choice: Linphone SDK over raw PJSIP

The Telecom `ConnectionService`/`Connection` scaffolding (`android/.../telecom/`)
only fakes call states (`setDialing()`, `setRinging()`) — there is no actual
SIP/RTP client in the app, so NexTelis cannot register as a PJSIP endpoint
or carry real audio. Asterisk cannot fix this from the server side: SIP
requires an actual endpoint (client) on the device, exactly the role Zoiper
plays today. Asterisk (server) and the device's SIP client are two
different, necessary halves.

**Options evaluated:**

* **Raw PJSIP (pjsua2):** No maintained Android AAR exists (the last known
  one, `com.pjdroid:pjdroid`, was abandoned in 2021). Would require building
  PJSIP from source with the NDK and hand-writing SWIG bindings — realistically
  weeks of build/toolchain work before a first call connects, with no current
  reference implementation to build from (CSipSimple/sipdroid are 10+ years dead).
* **Linphone SDK (liblinphone):** Official, actively maintained AAR from
  Belledonne Communications, hosted on their own Maven repo (not Central).
  Handles SIP digest auth, INVITE, RTP (ulaw/alaw/Opus), and background
  operation out of the box. Estimated same-day integration to a first working
  call against our existing Asterisk PJSIP-realtime config.

**Decision:** Use **Linphone SDK**
(`org.linphone:linphone-sdk-android`, repo `https://download.linphone.org/maven_repository`).
Existing `NexTelisConnectionService`/`NexTelisConnection` stay as the Android
Telecom-facing layer (needed for native dialer integration, per
`docs/PROJECT.md` §2) but now drive a Linphone `Core` instance instead of a
fake state machine, rather than being replaced by Linphone's own call UI.

**Licensing note:** Linphone SDK is dual-licensed — free under AGPLv3, or a
paid commercial license from Belledonne Communications for closed-source
distribution. Decision for now: accept AGPLv3 for Pilot v0/v1 development
and testing. **This must be revisited before any closed-source or commercial
launch** — AGPLv3 requires that if NexTelis is distributed, its full source
must be made available to users.

**Known Android 12+ gotchas to design around:**
* Foreground service for calls needs `FOREGROUND_SERVICE_TYPE_PHONE_CALL`
  (Android 14 rejects untyped/mismatched foreground service starts).
* Should still register as a `CAPABILITY_SELF_MANAGED` Telecom connection on
  top of Linphone for proper system call UI/Bluetooth routing — this layer
  isn't fully turnkey in Linphone's Android SDK.
* OEM battery managers (Xiaomi/Oppo/Vivo/Huawei, and likely OnePlus per our
  existing OxygenOS findings above) aggressively kill background SIP
  keep-alive; will need battery-optimization exemption prompts.
* Audio focus/routing (Bluetooth/wired/speaker) must be bridged through
  `Connection.onCallAudioStateChanged` even with Linphone managing most of
  it internally.

---

## Two account-lifecycle bugs found during real-device testing

### Bug: re-registering a PhoneAccount silently disables it

Symptom: enabling NexTelis in Settings > Calling accounts, then returning to
the app, showed it as disabled again — and this happened even inside the
Settings screen itself once the app's activity was recreated in the
background.

Root cause: `TelecomManager.registerPhoneAccount()` unconditionally resets
`isEnabled` to `false`, even for an already-registered account the user had
just enabled — this is intentional Android behavior (an app can't silently
re-enable itself). `HomeActivity.onCreate()` was calling
`PhoneAccountManager.register()` unconditionally on every creation, and
returning from the system Settings screen recreates the activity, so every
trip there immediately undid the toggle just flipped.

**Fix:** `PhoneAccountManager.register()` is now a no-op if the account is
already registered.

### Gap: no way to re-pair an existing user to a new device

Symptom: uninstalling and reinstalling the app (which wipes local
`SessionStore`) and registering with the same email returns `409 Conflict`
from `POST /users` — the backend user/number still exist, but the app had
no path to recover the pairing.

**Fix:** added `POST /users/claim-code` (`{email}` → fresh `claim_code` for
the existing user), and the Android app now falls back to it automatically
when registration 409s.

### Follow-up: email alone was an account-takeover vector — closed in v1

The first version of `POST /users/claim-code` had no password or email
verification: knowing a registered email address was sufficient to mint a
claim code and pair an attacker's device to that account. This was initially
accepted as a pilot-stage tradeoff, but it was closed before declaring v1 —
shipping a "service" with a trivial takeover path would have misrepresented
what v1 is.

**Options considered:**

* *Email confirmation link* — rejected: NexTelis has no email-delivery
  infrastructure, and adding one is a disproportionate dependency at this stage.
* *Require proof from an existing paired device* — rejected: it breaks the
  exact case the re-pair flow exists for ("I lost my only phone").
* *One-time recovery code issued at registration* — **chosen.** Self-contained,
  no new infrastructure, and it survives losing every paired device.

**Fix applied:**

* `POST /users` now generates a recovery code (`secrets.token_urlsafe(24)`),
  stores only its SHA-256 hash on the user, and returns the plaintext exactly
  once in the registration response.
* `POST /users/claim-code` requires `{email, recovery_code}` and returns `400`
  on mismatch. Email alone no longer proves anything.
* The Android pairing screen displays the code with a copy button and gates
  the "Pair this device" button behind an "I've saved my recovery code"
  checkbox, and prompts for the code when registration returns `409`.
* Migration `a1b2c3d4e5f6` backfills pre-existing rows with a fresh
  (undisclosed) code — those are seeded test users, so no real user is
  stranded; a real affected account would need an admin-issued code.

**Residual limitation:** a lost recovery code means a lost account, with no
self-service reset. Acceptable at this scale; a proper account-recovery story
belongs to v3 (Security).
