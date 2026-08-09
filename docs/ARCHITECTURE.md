# NexTelis — Architecture

> **Version:** Pilot v0.1
> **Status:** Initial architecture
> **Purpose:** Define the minimum architecture for the NexTelis telephony experiment.

---

## 1. Overview

NexTelis is an experimental private Internet-based telephony system.

The system aims to provide users with private NexTelis numbers while allowing communication through the existing Android telephony experience.

The initial system uses:

* Android phones
* Android Telecom APIs
* Wi-Fi / IP connectivity
* Asterisk
* A Linux server

The MVP does **not** attempt to build a cellular network.

---

## 2. Core Architecture

```text
                         NexTelis

                    ┌───────────────┐
                    │   NexTelis    │
                    │    Backend    │
                    │               │
                    │ Users         │
                    │ Numbers       │
                    │ Call Logic    │
                    └───────┬───────┘
                            │
                         Control
                            │
                            ▼
                    ┌───────────────┐
                    │   Asterisk    │
                    │               │
                    │ SIP           │
                    │ Call Routing  │
                    │ RTP / Media   │
                    └───────┬───────┘
                            │
                         IP Network
                            │
             ┌──────────────┴──────────────┐
             ▼                             ▼
        Android A                     Android B
             │                             │
             └──── Android Telecom ────────┘
```

---

# 3. Main Components

## 3.1 Android

The Android device is the user's telephone endpoint.

Our long-term goal is to use Android's existing telephony framework instead of building a completely separate calling application.

Relevant Android components include:

* `TelecomManager`
* `PhoneAccount`
* `ConnectionService`

The NexTelis application is responsible for integrating with this framework.

Ideally, Android continues to provide:

* Native dialer
* Native contacts
* Incoming-call UI
* In-call UI
* Call history

The exact level of integration must be experimentally verified.

---

## 3.2 NexTelis Android Application

The NexTelis application provides the NexTelis-specific functionality.

Initial responsibilities:

```text
Register user
      ↓
Receive private number
      ↓
Register device
      ↓
Configure calling service
```

The application should avoid unnecessarily replacing Android's native phone experience.

---

## 3.3 Android Telecom

Android Telecom acts as the bridge between the system phone experience and NexTelis.

Desired flow:

```text
User
  ↓
Native Dialer
  ↓
Android Telecom
  ↓
NexTelis ConnectionService
  ↓
NexTelis
```

This is one of the most important technical areas of Pilot v0.

We must determine experimentally what Android permits.

---

# 4. Backend

The NexTelis backend is the **control layer**.

It is responsible for application-level information such as:

```text
Users
Private numbers
Devices
Contacts
Permissions
Call requests
Call state
Call records
```

The backend does **not** carry real-time voice media.

For the initial prototype, the backend may be extremely small.

---

# 5. Asterisk

Asterisk is the initial **telephony engine**.

It provides functionality that we do not want to implement ourselves.

Responsibilities include:

```text
SIP
Call establishment
Call routing
Channels
Bridges
RTP/media
Telephony state
```

The backend can tell Asterisk what should happen, while Asterisk handles the actual telephony mechanics.

---

# 6. Control Plane vs Media Plane

This distinction is fundamental to NexTelis.

## Control Plane

Handles decisions and application logic:

```text
Android
   ↓
NexTelis Backend
   ↓
Asterisk control
```

Examples:

* User registration
* Number assignment
* Permission checks
* Call initiation
* Call authorization
* Call state

HTTP/API communication may be used here.

---

## Media Plane

Handles the actual voice communication:

```text
Phone
   ⇅
SIP / RTP
   ⇅
Asterisk
   ⇅
SIP / RTP
   ⇅
Phone
```

We do not want to send the voice stream through ordinary HTTP requests.

Asterisk and appropriate real-time media protocols handle this layer.

---

# 7. Example Call

Assume:

```text
User A = 7001
User B = 7002
```

User A calls `7002`.

The desired high-level process is:

```text
1. User enters 7002

2. Android Dialer processes the call

3. Android Telecom identifies the
   NexTelis calling service

4. NexTelis ConnectionService receives
   the call request

5. NexTelis determines that 7002
   belongs to User B

6. NexTelis/Asterisk establishes the call

7. Asterisk contacts User B

8. User B answers

9. Real-time media is established

10. Users communicate
```

Conceptually:

```text
7001
 │
 ▼
Android Dialer
 │
 ▼
Android Telecom
 │
 ▼
NexTelis
 │
 ▼
Asterisk
 │
 ▼
7002
 │
 ▼
Android B
```

---

# 8. Local Development Architecture

The first implementation does not require cloud infrastructure.

A Linux PC can host the initial server:

```text
Linux PC
│
├── Docker
│    └── Asterisk
│
└── NexTelis Backend
```

Android devices connect through the local network.

```text
               Local Wi-Fi

Android A ───────────────┐
                          │
                          ▼
                    Linux PC
                          │
                    ┌─────┴─────┐
                    │ Asterisk  │
                    │           │
                    │ NexTelis  │
                    │ Backend   │
                    └─────┬─────┘
                          │
                          │
Android B ────────────────┘
```

This is our first target environment.

---


# 9. Privacy Model — Initial

Privacy is a core motivation of the project.

However, Pilot v0 is focused on proving connectivity and telephony.

Initial priorities:

```text
1. Working calls
2. Correct architecture
3. Understanding Android limitations
4. Understanding Asterisk
5. Basic account separation
```

Advanced security will be developed later.

Future work includes:

* Strong authentication
* Secure credentials
* TLS
* Secure SIP
* Secure media
* Key management
* Device authentication
* Authorization
* Abuse prevention
* Hardening

We should not claim production-grade privacy until these areas have been properly designed and tested.

---

# 10. Architecture Principles

### Principle 1 — Keep the MVP small

Do not build infrastructure before it is required.

### Principle 2 — Use existing components

Use:

```text
Android Telecom
Asterisk
Docker
Linux
```

instead of implementing telephony infrastructure ourselves.

### Principle 3 — Separate control and media

Backend logic should not become a real-time audio server.

### Principle 4 — Android remains the phone

The goal is to integrate with Android rather than replace its entire telephony experience.

### Principle 5 — Test assumptions

Especially:

```text
Can Android Telecom do this?
```

should always be answered through a prototype rather than assumption.

### Principle 6 — Defer cellular infrastructure

Open5GS and private LTE/5G are interesting future directions but provide no benefit to the initial proof of concept.

---