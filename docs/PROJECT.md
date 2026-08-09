# NexTelis — Project Context

## 1. Vision

NexTelis is an experimental **private Internet-based telephony system**.

The goal is to let users have their own **private/custom telephone numbers** and communicate with other NexTelis users using the phone's **native Android telephony experience**, rather than requiring a WhatsApp/Viber-style calling application.

The system should eventually feel like:

```text
User
  ↓
Android Contacts / Dialer
  ↓
Android Telecom
  ↓
NexTelis
  ↓
Asterisk
  ↓
Internet
  ↓
Other NexTelis user
```

The Internet is currently treated as the **transport layer**. We are NOT trying to build our own physical cellular network in the MVP.

---

# 2. Core Idea

A user installs the NexTelis Android application.

The application:

* Registers the user with NexTelis
* Gives them a private/custom number
* Registers/configures their device
* Integrates with Android's Telecom framework

The user should ideally continue using:

* Android's built-in Phone/Dialer
* Android's Contacts
* Android's native incoming-call UI
* Android's native in-call experience
* Android's call history, where the Telecom framework permits it

NexTelis should **not become another standalone dialer application** unless Android's APIs force us to do so.

---

# 3. Example

Suppose:

```text
User A → NexTelis number 7001
User B → NexTelis number 7002
```

User A enters:

```text
7002
```

in the normal Android phone dialer.

The desired flow is:

```text
Android Dialer
      ↓
Android Telecom
      ↓
NexTelis ConnectionService
      ↓
NexTelis calling layer
      ↓
Asterisk
      ↓
Internet
      ↓
Asterisk / NexTelis
      ↓
User B's Android device
```

The important technical question is whether Android Telecom allows us to achieve this degree of native integration.

We **do not assume that it will**. This must be experimentally verified.

---

# 4. Asterisk

Asterisk will initially be our **telephony engine**.

It handles things such as:

* SIP
* Call establishment
* Call routing
* Telephony channels
* Bridges
* RTP/media
* Real-time voice communication
* Call state

Asterisk is NOT our entire application.

Our own backend will eventually control the higher-level NexTelis logic.

Conceptually:

```text
              NexTelis Backend
                     │
              Call/business logic
                     │
                     ▼
                 Asterisk
                     │
              SIP / RTP / Voice
                     │
                     ▼
                  Phones
```

---

# 5. First Technical Milestone

Before writing our own Android calling integration, we want to prove the basic telephony infrastructure.

### Stage 1

Run Asterisk locally, preferably in Docker.

```text
Linux PC
   │
   └── Docker
        └── Asterisk
```

Create two simple SIP users/numbers:

```text
7001
7002
```

Then use two Android devices on the same Wi-Fi network.

```text
Android A
   │
   │ Wi-Fi
   ▼
Asterisk
   │
   │ Wi-Fi
   ▼
Android B
```

The first milestone is simply:

> **Make two real Android phones successfully call each other through our local Asterisk server.**

Initially we can use an existing SIP client to prove Asterisk works.

We should NOT build the Android Telecom integration at the same time, because that would make debugging unnecessarily difficult.

---

# 6. After Asterisk Works

Once:

```text
Phone A → Asterisk → Phone B
```

works, we investigate Android Telecom.

Important Android concepts to investigate:

* `TelecomManager`
* `PhoneAccount`
* `ConnectionService`
* Android call integration
* Permissions
* Default calling-provider behavior
* Native dialer integration
* Native call history
* Contact interaction
* Incoming-call UI

The goal is:

```text
Android built-in Phone
        ↓
Android Telecom
        ↓
NexTelis ConnectionService
        ↓
Asterisk
```

rather than:

```text
NexTelis App
   ↓
Custom Dialer
   ↓
Custom Call UI
```

---

# 7. Contacts and Call History

We want Android to remain responsible for the normal phone experience where possible.

NexTelis should NOT initially build its own replacement contacts application.

There are two types of information:

### NexTelis information

Our backend can maintain:

* Private numbers
* NexTelis users
* Friend relationships
* Permissions
* NexTelis call records

### Android information

Android may provide:

* Native contacts
* Native dialer
* Native call history
* Native incoming-call UI
* Native in-call UI

Exactly how much NexTelis can integrate with these will depend on Android's Telecom APIs and permissions.

This is an area we need to test rather than assume.

---

# 8. Backend

The backend represents the **NexTelis control/application layer**.

It will eventually manage:

```text
Users
Private numbers
Devices
Contacts/friends
Permissions
Call requests
Call state
Authentication
```

It should NOT carry real-time voice media.

We want:

```text
Backend
   │
   │ control
   ▼
Asterisk
   │
   │ real-time media
   ▼
Phones
```

rather than routing voice through ordinary HTTP requests.

---

# 9. HTTP / Latency

Our original idea involved simple HTTP-based communication.

We concluded that HTTP should be used for:

```text
Control
API
Account management
Call requests
Configuration
```

but **not for the actual real-time voice stream**.

Real-time voice should use proper telephony/media protocols handled by Asterisk.

The basic separation is:

```text
CONTROL PLANE
HTTP / API / backend
        │
        ▼
     Asterisk

MEDIA PLANE
SIP / RTP / codec/media
        │
        ▼
     Phones
```

This avoids trying to reinvent real-time voice transport.

---

# 10. Networking Assumption

For the MVP:

> **Wi-Fi is our initial transport.**

We are NOT currently trying to create:

* Private LTE
* Private 5G
* Cellular radio
* Custom SIM
* SDR-based network
* Wi-Fi mesh
* Bluetooth mesh
* Satellite network
* Custom RF network

Those are future possibilities.

The current system simply uses an existing IP connection.

For example:

```text
Phone
  ↓
Wi-Fi
  ↓
Internet
  ↓
NexTelis/Asterisk
```

Eventually the same service could work over:

```text
Wi-Fi
4G
5G
Other IP connectivity
```

The transport should ideally be interchangeable.

---

# 11. Open5GS

Open5GS was investigated as a possible future component.

Open5GS is a cellular core implementation for LTE/5G networks.

It could eventually be part of something like:

```text
Phone
   ↓
Private LTE/5G RAN
   ↓
Open5GS
   ↓
NexTelis
```

However, Open5GS itself does not provide the cellular radio.

A real private LTE/5G deployment would require appropriate RAN/radio infrastructure and would introduce regulatory/spectrum considerations.

Therefore:

> **Open5GS is explicitly outside the current MVP.**

It remains future research.

---

# 12. No-Hardware Constraint

Current project constraint:

> **No additional cellular/radio hardware.**

We want to use:

* Existing Linux PC/server
* Existing Android phones
* Existing Wi-Fi/network
* Docker
* Asterisk
* Android APIs
* Open-source software

The project should prove as much as possible before requiring specialized hardware.

---

# 13. "Can We Work Without Internet?"

We discussed whether a phone could connect to our server without Internet.

Conclusion:

> A tunnel cannot create connectivity where no physical/network path exists.

If the phone has:

* no Wi-Fi
* no mobile data
* no satellite
* no mesh
* no other network

then it has no path to the remote server.

Future alternatives could include mesh/radio systems, but they are outside the current project.

Therefore the current design is:

> **Our telecom service is independent of the underlying access network, but it still requires some IP connectivity.**

---

# 14. Privacy

Privacy is one of the project's motivations.

The desired architecture is:

```text
Phone
  ↓
Encrypted/private communication
  ↓
NexTelis infrastructure
  ↓
Other user
```

However, advanced:

* cryptographic design
* authentication hardening
* key management
* anti-abuse systems
* production security
* carrier-grade privacy

are **future stages**.

The MVP should first prove that the communication architecture works.

---

# 15. What We Are NOT Building

At least for the MVP, NexTelis is NOT:

* A cellular carrier
* A SIM replacement
* A private LTE network
* A private 5G network
* A WhatsApp clone
* A custom dialer
* A custom contacts application
* A custom radio network
* A mesh network
* A replacement for the Internet

It is:

> **A private Internet-based telephony service integrated as deeply as Android Telecom allows.**

---

# 16. Current Architecture

The current conceptual architecture is:

```text
                         NEX TELIS

                  ┌───────────────────┐
                  │   NexTelis        │
                  │   Backend         │
                  │                   │
                  │ Users             │
                  │ Numbers           │
                  │ Contacts         │
                  │ Devices           │
                  │ Call logic        │
                  └─────────┬─────────┘
                            │
                         Control
                            │
                            ▼
                     ┌────────────┐
                     │  Asterisk  │
                     │            │
                     │ SIP        │
                     │ RTP        │
                     │ Routing    │
                     │ Media      │
                     └─────┬──────┘
                           │
                       Internet
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
        Android Phone A           Android Phone B
              │                         │
              └──── Android Telecom ───┘
```

---

# 17. Current Repository

Keep the repository intentionally simple.

```text
nextelis/
│
├── README.md
├── setup.py
│
├── docs/
│   ├── PROJECT.md
│   ├── ARCHITECTURE.md
│   ├── ROADMAP.md
│   └── DECISIONS.md
│
├── backend/
├── android/
├── asterisk/
└── tests/
```

Do NOT prematurely create large nested package structures.

We will create additional files/folders only when implementation actually requires them.

---

# 18. Development Philosophy

The project should evolve experimentally.

Do not assume that a technology can provide a capability before testing it.

Especially for Android Telecom:

```text
Hypothesis
    ↓
Small prototype
    ↓
Test on real Android device
    ↓
Confirm limitation/capability
    ↓
Architecture decision
```

This is especially important because Android controls access to system telephony functionality.

---

# 19. Immediate Roadmap

### Phase 0 — Documentation

Define:

* Vision
* Architecture
* Constraints
* MVP
* Decisions

### Phase 1 — Local Asterisk

Set up:

```text
Docker
  ↓
Asterisk
```

### Phase 2 — Two-phone test

```text
Android A
    ↓
Wi-Fi
    ↓
Asterisk
    ↓
Wi-Fi
    ↓
Android B
```

Make a successful voice call.

### Phase 3 — Understand Android Telecom

Investigate:

```text
ConnectionService
PhoneAccount
TelecomManager
Native dialer
Call history
Contacts
Incoming calls
```

Build the smallest possible Android prototype.

### Phase 4 — Connect Android Telecom to Asterisk

Target:

```text
Native Android Dialer
        ↓
NexTelis
        ↓
Asterisk
        ↓
Remote phone
```

### Phase 5 — NexTelis backend

Add:

```text
Users
Numbers
Devices
Contacts
Call authorization
Call records
```

### Phase 6 — Security and reliability

Later:

```text
Authentication
Encryption
Credential management
NAT traversal
Latency
Jitter
Packet loss
Abuse prevention
Hardening
```

### Phase 7 — Future research

Potentially investigate:

```text
Open5GS
Private LTE
Private 5G
RAN
SDR
Alternative transport
Mesh networking
```

These are NOT MVP requirements.

---

# 20. Current Primary Question

The most important unanswered question in the entire project is:

> **Can an Android application register as a calling provider through Android Telecom and make NexTelis private-number calls appear/use the native Android phone experience without requiring us to replace the normal dialer?**

Everything else should be developed around that answer.

---