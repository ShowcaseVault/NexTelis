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
