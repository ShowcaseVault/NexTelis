# NexTelis — Development Roadmap

> **Project status:** Pilot v0
> **Purpose:** Experimental private Internet telephony using existing Android devices, Asterisk, and Android Telecom APIs.

---

## Version Philosophy

NexTelis will be developed incrementally.

Each version should answer a specific technical question and produce a working milestone.

We do **not** implement future complexity before the current version is proven.

```text
Experiment
    ↓
Working prototype
    ↓
Understand limitations
    ↓
Document result
    ↓
Next version
```

---

# Pilot v0 — Feasibility & Foundation

### Objective

Determine whether the basic NexTelis concept is technically feasible using existing hardware and software.

### Constraints

* Existing Linux PC/server
* Existing Android phones
* Wi-Fi / existing IP connectivity
* No SIM
* No cellular hardware
* No SDR
* No private LTE/5G
* No Open5GS requirement
* No production security requirements yet

### Work

#### v0.1 — Project Foundation

* Create repository
* Create basic project structure
* Create documentation
* Define architecture
* Define terminology
* Record technical decisions

**Deliverable:**

```text
NexTelis repository
+
project documentation
```

---

#### v0.2 — Asterisk Foundation

Set up Asterisk locally.

Preferred environment:

```text
Linux
  ↓
Docker
  ↓
Asterisk
```

Learn:

* SIP
* RTP
* PJSIP
* Extensions
* Dialplan
* Channels
* Bridges
* Basic call routing

**Deliverable:**

A working local Asterisk server.

---

#### v0.3 — First Telephone Call

Create two test endpoints:

```text
7001
7002
```

Connect two Android phones over Wi-Fi.

Target:

```text
Phone A
  ↓
Wi-Fi
  ↓
Asterisk
  ↓
Wi-Fi
  ↓
Phone B
```

Make:

```text
7001 → 7002
```

work.

**Deliverable:**

A real voice call between two physical phones through our server.

---

#### v0.4 — Understand the Telephony Stack

Document what actually happened.

Study:

```text
SIP
RTP
PJSIP
Codecs
Call setup
Call teardown
NAT
Ports
Latency
Jitter
```

Trace:

```text
CALL REQUEST
     ↓
SIP
     ↓
Asterisk
     ↓
SIP
     ↓
REMOTE PHONE
     ↓
RTP
     ↓
VOICE
```

**Deliverable:**

A documented understanding of our basic telephony path.

---

#### v0.5 — Android Telecom Feasibility

Investigate the Android side.

Study and prototype:

```text
TelecomManager
PhoneAccount
ConnectionService
```

Determine whether NexTelis can:

* Register as a calling provider
* Receive calls from the native dialer
* Initiate calls from the native dialer
* Use custom/private numbers
* Integrate with native call UI
* Integrate with native call history
* Work with Android Contacts
* Handle incoming calls

**Deliverable:**

A small Android Telecom proof-of-concept.

---

### Pilot v0 Exit Criteria

Pilot v0 is successful if we can demonstrate:

```text
Android native telephony
        ↓
NexTelis service
        ↓
Asterisk
        ↓
Internet/Wi-Fi
        ↓
Another Android device
```

AND we understand exactly what Android allows and prevents us from doing.

---

# v1 — NexTelis Private Telephony MVP

### Objective

Turn the experiment into an actual minimal NexTelis service.

### Features

#### Users

```text
User
 └── NexTelis account
```

#### Private Numbers

Example:

```text
7001
7002
7003
```

Each number belongs to a NexTelis user.

#### Devices

Associate:

```text
User
  ↓
Android device
  ↓
NexTelis service
```

#### Calling

```text
User A
  ↓
7002
  ↓
NexTelis
  ↓
Asterisk
  ↓
User B
```

#### Backend

Introduce the first real backend.

Responsibilities:

* User registration
* Number assignment
* Device registration
* Call authorization
* Basic call state
* Basic call records

### v1 Target

A person can:

1. Register
2. Receive a NexTelis number
3. Register their Android device
4. Call another NexTelis user
5. Receive a call from another NexTelis user

---

# v2 — Native Android Integration

### Objective

Make NexTelis feel like a native phone service.

Focus heavily on Android Telecom.

Target experience:

```text
Android Contacts
       ↓
Android Dialer
       ↓
Android Telecom
       ↓
NexTelis
       ↓
Asterisk
```

Investigate/support:

* Native outgoing calls
* Native incoming calls
* Native call UI
* Native call history
* Contacts integration
* Call notifications
* Multiple phone accounts
* Device/account state

### Goal

The user should not need to think:

> "I'm using a VoIP application."

It should feel as close as Android permits to using another telephone service.

---

# v3 — Privacy & Security

### Objective

Move from functional prototype toward a trustworthy communication system.

Implement:

* Strong authentication
* Device authentication
* Secure credential handling
* TLS
* Secure SIP configuration
* Secure media where appropriate
* Key management
* Authorization
* Account protection
* Abuse prevention
* Secure backend APIs

Security architecture should be designed deliberately rather than added randomly.

---

# v4 — Reliability & Network Engineering

### Objective

Make calls reliable outside a simple local Wi-Fi environment.

Investigate:

* NAT traversal
* STUN
* TURN
* SIP NAT behavior
* RTP routing
* Network changes
* Wi-Fi → mobile data transitions
* Packet loss
* Jitter
* Latency
* Reconnection
* Call quality monitoring

Target:

```text
Wi-Fi
   │
   ├──→ NexTelis
   │
4G/5G
   │
   └──→ NexTelis
```

The transport should become transparent to the user.

---

# v5 — Service Architecture

### Objective

Move from a single-machine prototype toward a scalable service.

Potential components:

```text
                 Load Balancer
                      │
              ┌───────┴───────┐
              ▼               ▼
          Backend A       Backend B
              │               │
              └───────┬───────┘
                      │
                 Telephony
                   Layer
                      │
              ┌───────┴───────┐
              ▼               ▼
          Asterisk A       Asterisk B
```

Potential work:

* Persistent database
* Service separation
* Monitoring
* Logging
* Metrics
* Backups
* High availability
* Multiple telephony nodes

This is only necessary if the project grows.

---

# v6 — Advanced Telecom Research

Only after the Internet-based system is working well.

Investigate:

### Open5GS

```text
Android
   ↓
LTE/5G RAN
   ↓
Open5GS
   ↓
NexTelis
```

### Private LTE/5G

Study:

* EPC
* 5G Core
* RAN
* eNodeB
* gNodeB
* Subscriber management
* SIM/eSIM
* Spectrum/regulatory requirements

### Hardware

Potential future research:

* SDR
* Cellular RAN hardware
* Test networks

This is **not part of the current MVP**.

---

# Future / Experimental Branches

These are possibilities, not committed roadmap versions.

## Alternative Transport

Investigate whether NexTelis could eventually work over:

```text
Wi-Fi
4G
5G
Satellite
Mesh
Local device-to-device networks
```

The goal would be to separate:

```text
NexTelis service
        +
Transport network
```

---

## Mesh Communication

Possible future research:

```text
Phone A
   ↓
Phone B
   ↓
Phone C
   ↓
Internet Gateway
   ↓
NexTelis
```

This would require significant additional research and potentially hardware/software constraints.

Not part of the MVP.

---

# Version Summary

| Version      | Purpose           | Main Result                         |
| ------------ | ----------------- | ----------------------------------- |
| **Pilot v0** | Feasibility       | Prove the concept                   |
| **v0.1**     | Foundation        | Repository + docs                   |
| **v0.2**     | Asterisk          | Local telephony core                |
| **v0.3**     | First call        | Phone → Asterisk → Phone            |
| **v0.4**     | Understanding     | SIP/RTP/network knowledge           |
| **v0.5**     | Android Telecom   | Native integration feasibility      |
| **v1**       | MVP               | Private numbers + calling           |
| **v2**       | Native experience | Android dialer/contacts integration |
| **v3**       | Security          | Privacy + authentication            |
| **v4**       | Reliability       | Real-world networks                 |
| **v5**       | Scale             | Production-style architecture       |
| **v6**       | Cellular research | Open5GS/private LTE/5G              |

---

# Current Position

```text
                NEX TELIS

                    │
                    ▼
             ┌─────────────┐
             │  PILOT v0  │
             └──────┬──────┘
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
   Asterisk      Android      Architecture
     v0.2        Telecom        v0.1
       │            │
       ▼            ▼
  First call     Feasibility
     v0.3          v0.5
       │            │
       └──────┬─────┘
              ▼
          NexTelis v1
              │
              ▼
       Private Telephony
             MVP
```