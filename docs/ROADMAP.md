# NexTelis — Development Roadmap

> **Project status:** v1 delivered — Private Telephony MVP
> **Next:** v2 — Native Android Integration
> **Purpose:** Experimental private Internet telephony using existing Android devices, Asterisk, and Android Telecom APIs.

---

## Version Philosophy

NexTelis is developed incrementally. Each version answers a specific technical
question and produces a working milestone. We do **not** implement future
complexity before the current version is proven.

```text
Experiment → Working prototype → Understand limitations → Document → Next version
```

Findings from each version live in [FINDINGS.md](FINDINGS.md). The delivered
v1 system is documented in [NEXTELIS-V1.md](NEXTELIS-V1.md).

---

# ✅ Pilot v0 — Feasibility & Foundation (complete)

**Question:** Is the NexTelis concept technically feasible with existing
hardware — no SIM, no cellular hardware, no SDR, no private LTE/5G?

**Answer: yes, with device-dependent caveats.**

| Version | Goal | Result |
|---|---|---|
| v0.1 | Project foundation | Repository, architecture, terminology, docs |
| v0.2 | Asterisk foundation | Working local Asterisk (Docker, PJSIP) |
| v0.3 | First telephone call | Real voice call `7001 → 7002` between two physical phones over Wi-Fi |
| v0.4 | Understand the stack | Documented SIP/RTP/PJSIP/codec/NAT call path |
| v0.5 | Android Telecom feasibility | `PhoneAccount`/`ConnectionService` proof-of-concept |

**Key limitation discovered:** Android Telecom integration is **OEM-dependent**,
not just Android-version-dependent. Stock-like ROMs (Samsung One UI) expose the
calling-accounts toggle and work end-to-end; heavily customized ROMs
(OxygenOS/ColorOS) can remove that UI entirely, with no third-party workaround.
See [FINDINGS.md](FINDINGS.md).

---

# ✅ v1 — NexTelis Private Telephony MVP (complete)

**Question:** Can the experiment become an actual minimal service?

**Target — a person can:**

1. ✅ Register
2. ✅ Receive a NexTelis number
3. ✅ Register their Android device
4. ✅ Call another NexTelis user
5. ✅ Receive a call from another NexTelis user

**Delivered:**

* **Backend** (FastAPI + Postgres) — user registration, number assignment,
  device registration/auth, call authorization, CDR table, PJSIP realtime
  provisioning read directly by Asterisk.
* **Android app** — runtime server configuration (no hardcoded IP), full
  onboarding flow, Telecom `PhoneAccount` registration, real SIP/RTP calling
  via the Linphone SDK, backend-backed caller-ID resolution.
* **Security hardening** — device tokens and account recovery codes are stored
  only as SHA-256 hashes and shown exactly once. Re-pairing a device requires
  the recovery code, closing the email-only account-takeover vector found
  during v0.

**Full detail — what was built, what was verified, and what is explicitly not
done: [NEXTELIS-V1.md](NEXTELIS-V1.md).**

**Carried into later versions:** OnePlus/OxygenOS unsupported (no fix
available), no TLS on control or media plane (v3), single-host deployment (v5),
LAN-only verification (v4), Linphone AGPLv3 licensing decision pending before
any closed-source launch.

---

# ▶ v2 — Native Android Integration (next)

**Objective:** Make NexTelis feel like a native phone service rather than a
VoIP app.

```text
Android Contacts → Android Dialer → Android Telecom → NexTelis → Asterisk
```

Investigate and support:

* Native call history integration (verify Asterisk CDR writing end-to-end first)
* Contacts integration
* Call notifications
* Multiple phone accounts / account state
* Polishing native incoming and in-call UI

**Goal:** the user should never need to think *"I'm using a VoIP application."*

---

# v3 — Privacy & Security

**Objective:** Move from functional prototype toward a trustworthy system.
Security architecture designed deliberately, not added piecemeal.

* TLS on the control plane; SIP over TLS; SRTP for media
* Strong user and device authentication; key management
* Secure credential handling and authorization
* Account protection, rate limiting, abuse prevention

---

# v4 — Reliability & Network Engineering

**Objective:** Calls that work outside a simple local Wi-Fi environment.

* NAT traversal — STUN, TURN, SIP NAT behavior, RTP routing
* Network transitions (Wi-Fi ↔ 4G/5G), reconnection
* Packet loss, jitter, latency, call-quality monitoring

The transport should become transparent to the user.

---

# v5 — Service Architecture

**Objective:** Move from a single-machine prototype toward a scalable service.
**Only necessary if the project grows.**

```text
Load Balancer → Backend A/B → Telephony Layer → Asterisk A/B
```

Service separation, monitoring, logging, metrics, backups, high availability,
multiple telephony nodes.

---

# v6 — Advanced Telecom Research

Only after the Internet-based system works well. **Not part of the MVP.**

* **Open5GS / private LTE/5G** — EPC, 5G core, RAN, eNodeB/gNodeB, subscriber
  management, SIM/eSIM, spectrum and regulatory requirements
* **Hardware** — SDR, cellular RAN hardware, test networks

---

# Future / Experimental Branches

Possibilities, not committed roadmap versions.

* **Alternative transport** — separating the NexTelis service from the
  transport network entirely (Wi-Fi, 4G, 5G, satellite, mesh, device-to-device)
* **Mesh communication** — phone-to-phone relay to an Internet gateway. Would
  require significant additional research and possibly dedicated hardware.

---

# Version Summary

| Version | Purpose | Result | Status |
|---|---|---|---|
| **Pilot v0** | Feasibility | Concept proven; OEM limits understood | ✅ Complete |
| **v1** | MVP | Private numbers + real calling service | ✅ Complete |
| **v2** | Native experience | Dialer / contacts / call-history integration | ▶ Next |
| **v3** | Security | TLS, SIP-TLS, SRTP, authentication, abuse prevention | Planned |
| **v4** | Reliability | NAT traversal, real-world networks | Planned |
| **v5** | Scale | Production-style architecture | If needed |
| **v6** | Cellular research | Open5GS / private LTE/5G | Research |

---

# Current Position

```text
                    NEXTELIS

              ┌──────────────┐
              │   PILOT v0   │   feasibility proven
              └──────┬───────┘
                     │
              ┌──────▼───────┐
              │      v1      │   ◀── you are here
              │ Private      │       MVP delivered
              │ Telephony    │
              │ MVP          │
              └──────┬───────┘
                     │
              ┌──────▼───────┐
              │      v2      │   native Android integration
              └──────────────┘
```
