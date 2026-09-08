# UTMASH

## UT Mesh — Offline-First Disaster Communication System

UTMASH is an Android-first disaster communication platform designed for situations where conventional communication infrastructure is unavailable, unreliable, or overloaded.

The primary mobile product is **UT Mesh**, a Bluetooth-based communication system where participating Android devices can automatically operate as network nodes.

The core objective is to enable nearby devices to communicate directly and, when necessary, automatically relay messages through other devices to reach destinations outside direct Bluetooth range.

```text
                         UT MESH NETWORK

                         ┌─────────────┐
                         │    UT-C     │
                         │ Relay Node  │
                         └──────┬──────┘
                               / \
                              /   \
                             /     \
                    ┌───────┘       └───────┐
                    │                       │
               ┌────▼────┐             ┌────▼────┐
               │   UT-A  │             │  UT-B   │
               │ Source  │             │  Dest   │
               └─────────┘             └─────────┘

                    A → C → B
```

If two devices are within direct communication range:

```text
UT-A → UT-B
```

If they are outside direct range but another device can connect to both:

```text
UT-A → UT-C → UT-B
```

For larger networks:

```text
UT-A → UT-C → UT-D → UT-E → UT-B
```

Intermediate devices are automatically selected by the routing system. Users do not manually configure routers, repeaters, or relay nodes.

---

# Core Objectives

UTMASH is being developed around the following principles:

- Offline-first communication
- Automatic Bluetooth networking
- Automatic multi-hop routing
- Distributed network nodes
- Dynamic route discovery
- Automatic relay selection
- Store-and-forward communication
- Disaster and emergency communication
- Local data storage
- Resilient operation when Internet infrastructure is unavailable

Every participating device can act as:

```text
Source
Destination
Relay
Network Node
```

---

# Features

## Communication

- Bluetooth-based nearby communication
- BLE communication
- GATT-based peer-to-peer communication
- Automatic neighboring-node discovery
- Automatic route discovery
- Multi-hop packet forwarding
- Direct communication when possible
- Dynamic relay selection
- Route failure recovery
- Duplicate packet protection
- TTL-based loop protection
- Store-and-forward queues
- Chunked data transfer
- Transmission acknowledgements
- Delivery tracking

---

# Automatic Multi-Hop Networking

UTMASH does not require users to manually configure intermediate devices.

### Direct Communication

```text
UT-A ───────────────── UT-B
```

If A and B are directly reachable:

```text
A → B
```

### One Relay

```text
UT-A       UT-C       UT-B
  \          |          /
   \─────────┴─────────/

A → C → B
```

### Multiple Relays

```text
UT-A → UT-C → UT-D → UT-E → UT-B
```

The routing layer dynamically determines the available communication path.

Potential route-selection metrics include:

- Hop count
- RSSI
- Link quality
- Packet loss
- Latency
- Connection stability
- Route lifetime

---

# Network Node Model

Every running UT Mesh application operates as a network node.

Each node maintains information such as:

```text
Node ID
UT ID
Bluetooth identity
Connection state
Neighbor table
Routing table
Packet cache
Forwarding queue
```

A node can simultaneously:

```text
Generate packets
      ↓
Receive packets
      ↓
Forward packets
      ↓
Discover neighboring nodes
      ↓
Participate in route discovery
```

---

# Architecture

The application follows a layered communication architecture.

```text
┌───────────────────────────────┐
│          Application UI       │
│       Jetpack Compose        │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│        Session Manager        │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│         Routing Layer         │
│                               │
│ Neighbor Discovery            │
│ Route Discovery               │
│ Route Selection               │
│ Multi-Hop Forwarding          │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│    Transmission Scheduler     │
│                               │
│ TX Queue                      │
│ RX Processing                 │
│ Forwarding Queue              │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│        Protocol Engine        │
│                               │
│ Packet Creation               │
│ Packet Parsing                │
│ Validation                    │
│ TTL / Sequence Handling       │
│ Duplicate Detection           │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│      Bluetooth Transport      │
│                               │
│ BLE / GATT / RFCOMM            │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│        Bluetooth Radio        │
└───────────────────────────────┘
```

The routing functionality is intentionally separated from the UI so that packet forwarding can continue even when the communication screen is not currently open.

---

# Neighbor Discovery

When UT Mesh starts, the application discovers compatible nearby nodes.

Nodes exchange lightweight control information such as:

```text
Node ID
UT ID
Protocol Version
Node Status
Capabilities
```

A dynamic neighbor table is maintained.

Example:

```text
Neighbor Table

Node ID    Connection    RSSI    Last Seen    Status
------------------------------------------------------
UT-002     Connected     -48     2 sec        ACTIVE
UT-003     Connected     -71     1 sec        ACTIVE
UT-004     Connected     -83     5 sec        ACTIVE
```

Nodes that leave communication range are eventually marked unavailable.

Previously discovered devices are not assumed to remain permanently reachable.

---

# Routing

When a device wants to communicate with another UT:

```text
Destination Selected
        │
        ▼
Check Neighbor Table
        │
        ▼
Destination Directly Reachable?
       / \
     YES  NO
      │    │
      │    ▼
      │ Route Discovery
      │    │
      │    ▼
      │ Find Available Path
      │    │
      └────┴─────────► Route Established
```

Example:

```text
A → C → D → B
```

A route contains information such as:

```text
Source Node
Destination Node
Route ID
Hop Count
Next Hop
Previous Hop
Route Lifetime
```

Route discovery should be controlled to prevent unnecessary network flooding.

---

# Packet Format

The protocol uses a centralized packet definition designed to support multi-hop communication.

Conceptually:

```text
+---------------------------------------+
| Unique Word                           |
+---------------------------------------+
| Version | Type | Flags                |
+---------------------------------------+
| Source Node ID                        |
+---------------------------------------+
| Destination Node ID                   |
+---------------------------------------+
| Packet ID                             |
+---------------------------------------+
| Sequence Number                       |
+---------------------------------------+
| Hop Count                             |
+---------------------------------------+
| Remaining TTL                         |
+---------------------------------------+
| Payload Length                        |
+---------------------------------------+
| Payload                               |
+---------------------------------------+
| Checksum                              |
+---------------------------------------+
```

Packet creation and parsing should remain centralized inside the protocol layer.

---

# TTL / Hop Limit

Every routed packet contains a TTL or hop limit.

Example:

```text
Initial TTL = 8
```

Every relay performs:

```text
TTL = TTL - 1
```

If:

```text
TTL == 0
```

the packet is discarded.

This prevents routing loops and uncontrolled packet circulation.

---

# Duplicate Packet Protection

Multi-hop networks can produce duplicate packets.

Packets use:

```text
Source Node ID
Packet ID
Sequence Number
```

Each node maintains a bounded recently-seen packet cache.

When a duplicate packet is received:

```text
Already Seen
     ↓
   DROP
```

Old cache entries are expired to prevent unlimited memory usage.

---

# Relay Operation

When a packet arrives at a node:

```text
Receive Packet
      ↓
Validate Packet Structure
      ↓
Validate Checksum
      ↓
Check Destination
      ↓
Destination = Local Node?
      ├── YES → Deliver Packet
      │
      └── NO
           ↓
        Check TTL
           ↓
       Find Next Hop
           ↓
      Forward Packet
```

A relay node must not deliver another node's packet to its own application layer as if it were the final destination.

It forwards the packet toward the destination.

---

# Store-and-Forward

UTMASH is designed to support bounded temporary packet storage.

Example:

```text
A → C

C cannot currently reach B

        ↓

Packet temporarily queued

        ↓

B becomes reachable

        ↓

C → B
```

Forwarding queues must have:

- Maximum size
- Packet expiration
- TTL validation
- Duplicate detection
- Memory limits

Unlimited packet buffering is not permitted.

---

# Communication Types

The protocol is intended to support multiple payload types:

```text
TEXT
DATA
IMAGE
FILE
VOICE
VIDEO
SOS
CONTROL
```

Control packets include:

```text
HELLO
HELLO_ACK
NODE_INFO
ROUTE_REQUEST
ROUTE_RESPONSE
ROUTE_ERROR
ACK
```

Control traffic should be handled separately from application payload traffic.

---

# Full-Duplex Communication

The architecture supports simultaneous communication in both directions.

Example:

```text
A → C → B
B → C → A
```

Relay C must be capable of processing traffic in both directions.

The communication architecture therefore separates:

```text
TX Queue
RX Processing
Forwarding Queue
```

---

# SOS and Emergency Communication

UTMASH is designed for disaster and emergency scenarios.

Planned emergency functionality includes:

- SOS broadcasting
- Nearby SOS discovery
- Emergency message propagation
- Rescue-side communication
- Rescue device discovery
- Location sharing
- Local rescue coordination
- Role-aware rescue workflows

---

# Rescue Module

Rescue-specific functionality is isolated in:

```text
feature_rescue
```

The module is intended to contain rescue operations and emergency coordination functionality while keeping the core communication system modular.

---

# Offline Field Tools

UTMASH contains field-oriented utilities intended to remain useful in low-connectivity environments.

Examples include:

- Offline maps
- Location sharing
- Compass
- Signal finder
- Whistle
- Breadcrumb trail
- CPR assistance
- Flashlight patterns
- Sensor utilities
- Metal-detector-style utilities

Offline mapping is based around local map storage and MapLibre.

---

# Local Storage

The application uses local Android storage technologies for offline operation.

Current architecture includes:

- Room
- SQLCipher
- DataStore
- Encrypted local preferences
- Android Keystore-backed security material

The objective is to keep essential device and communication state available without requiring Internet connectivity.

---

# Security

UTMASH does not assume that Bluetooth pairing alone provides complete communication security.

The architecture is designed to support:

```text
Node Authentication
Message Authentication
Encrypted Payloads
Replay Protection
Route Authorization
```

Packets are validated for:

- Source
- Destination
- Packet structure
- Packet length
- TTL
- Sequence number
- Checksum

Malformed packets must be rejected safely without crashing the application.

---

# Current Firebase Status

The original codebase contains Firebase-related functionality.

Some legacy/current components include:

- Firebase Authentication
- Firestore
- Firebase Functions
- Firebase Storage
- Firebase Messaging
- Crashlytics
- Firebase Analytics
- Firebase Performance
- Firebase App Check

These components are currently being separated from the core offline communication architecture.

The target architecture is:

```text
                  UTMASH
                     │
        ┌────────────┴────────────┐
        │                         │
   Offline Core              Optional Services
        │                         │
   Bluetooth Mesh              Internet
   Local Storage               Firebase
   Local Identity              Cloud APIs
   Local Routing
        │
        ▼
   Disaster Communication
```

The core UT Mesh communication system is intended to operate without depending on Internet connectivity.

---

# Repository Structure

```text
UTMASH/
│
├── app/
│   └── Main Android application
│       ├── Compose UI
│       ├── Messaging
│       ├── Bluetooth
│       ├── Routing
│       ├── Local storage
│       ├── Security
│       ├── Maps
│       ├── Tools
│       ├── Profile
│       └── Communication services
│
├── feature_rescue/
│   └── Rescue operations module
│
├── baselineprofile/
│   └── Android startup/performance profiles
│
├── dashboard/
│   └── Project summary/dashboard
│
├── scripts/
│   └── Project helper scripts
│
├── docs/
│   └── Project documentation
│
├── gradle/
│   └── Gradle configuration
│
├── .github/
│   └── GitHub Actions workflows
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

---

# Technology Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design System | Material 3 |
| Architecture | MVVM / Layered Architecture |
| Minimum SDK | Android API 24 |
| Compile SDK | Android SDK 36 |
| Target SDK | Android SDK 36 |
| Local Database | Room |
| Database Security | SQLCipher |
| Preferences | DataStore |
| Device Security | Android Keystore |
| Connectivity | Bluetooth / BLE / GATT / RFCOMM |
| Background Communication | Android Foreground Services |
| Maps | MapLibre |
| Build System | Gradle / Kotlin DSL |
| Testing | Android Unit / Instrumented Tests |
| CI | GitHub Actions |
| Optional Backend | Firebase |

---

# Development Environment

Recommended development environment:

```text
Android Studio
Android SDK 36
JDK 21
Gradle Wrapper
Windows / Linux / macOS
```

For multi-hop Bluetooth testing, physical Android devices are required.

Emulators are useful for application development but do not replace physical multi-device mesh testing.

---

# Build

From the Android project root:

### Windows PowerShell

```powershell
.\gradlew.bat :app:assembleDebug
```

### Linux / macOS

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Run lint:

```powershell
.\gradlew.bat :app:lintDebug
```

Build the rescue module:

```powershell
.\gradlew.bat :feature_rescue:assembleDebug
```

---

# Multi-Hop Development Milestone

The first major mesh milestone is deliberately small.

Three physical Android devices:

```text
Phone A
   │
   ▼
Phone C
   │
   ▼
Phone B
```

A sends:

```text
HELLO FROM UT-A
```

B must receive:

```text
HELLO FROM UT-A
```

C automatically forwards the packet.

Expected logs:

```text
[A] Destination UT-B unavailable directly
[A] Route discovered: A → C → B

[A] TX Packet ID=104
[C] RX Packet ID=104
[C] Forwarding Packet ID=104
[B] RX Packet ID=104
[B] Packet delivered

Route: A → C → B
Hops: 2
```

Once this works reliably, the same routing engine can be extended to:

```text
Text
  ↓
Data
  ↓
Files
  ↓
Images
  ↓
Voice
  ↓
Video
```

---

# Test Scenarios

## Test 1 — Direct Communication

```text
A ↔ B
```

Expected:

```text
Route = A → B
Hop Count = 1
```

---

## Test 2 — One Relay

```text
A ↔ C ↔ B
```

Expected:

```text
Route = A → C → B
Hop Count = 2
```

C automatically becomes the relay.

---

## Test 3 — Three-Hop Communication

```text
A ↔ C ↔ D ↔ B
```

Expected:

```text
Route = A → C → D → B
Hop Count = 3
```

---

## Test 4 — Relay Failure

Initial route:

```text
A → C → B
```

If C becomes unavailable:

```text
A → C ✕ B
```

and another route exists:

```text
A → D → B
```

the routing system should automatically recover.

---

## Test 5 — Simultaneous Communication

```text
A → C → B
B → C → A
```

Both directions must operate simultaneously.

---

## Test 6 — Node Join

Initial network:

```text
A → C → B
```

Introduce D.

D should automatically become a potential routing node.

---

## Test 7 — Node Leave

Remove an intermediate node and verify automatic route discovery and recovery.

---

# Performance Metrics

The routing layer is intended to expose:

```text
Current Route
Hop Count
Route Changes
Route Discovery Time
Packets Forwarded
Packets Dropped
Packets Expired
Duplicate Packets
Average End-to-End Latency
Per-Hop Latency
Packet Loss
RSSI
Route Stability
```

Example:

```text
Route: UT-001 → UT-003 → UT-007
Hops: 2
Latency: 143 ms
Packet Loss: 1.8%
Route Status: ACTIVE
Forwarded Packets: 428
```

---

# Development Roadmap

## Phase 1 — Offline Foundation

- Remove unnecessary Internet dependencies
- Strengthen local storage
- Establish local device identity
- Stabilize Bluetooth transport
- Separate communication layers
- Remove unnecessary online startup dependencies

## Phase 2 — Automatic Mesh

- Neighbor discovery
- HELLO protocol
- Neighbor table
- Routing table
- Route discovery
- Multi-hop forwarding
- TTL handling
- Duplicate protection

## Phase 3 — Reliable Transport

- ACK handling
- Retransmission
- Packet queues
- Store-and-forward
- Route failure recovery
- Connection management

## Phase 4 — Media Communication

- Image transfer
- File transfer
- Audio transfer
- Voice communication
- Video communication

## Phase 5 — Rescue Network

- SOS propagation
- Rescue discovery
- Rescue mesh
- Emergency coordination
- Role-aware workflows

## Phase 6 — Security Hardening

- Node authentication
- Message authentication
- Encrypted payloads
- Replay protection
- Secure route authorization

## Phase 7 — Network Optimization

- Route scoring
- Link-quality analysis
- Latency optimization
- Battery optimization
- Connection stability
- Large-network testing

---

# Design Principles

## Offline First

Critical communication should not depend on the Internet.

## Automatic

Users select the destination rather than configuring intermediate routers.

## Distributed

Every participating device can contribute to the network.

## Resilient

Nodes can join, leave, disconnect, and reconnect dynamically.

## Modular

Bluetooth transport, routing, protocol processing, storage, UI, and rescue functionality remain separated.

## Secure

Bluetooth connectivity alone is not treated as sufficient application-level security.

## Field Focused

The system is designed for emergency and disaster communication scenarios where conventional infrastructure may not be available.

---

# Current Project Status

UTMASH currently contains a working Android application foundation with:

- Kotlin and Jetpack Compose
- Local profile functionality
- Local storage components
- Bluetooth communication components
- Messaging architecture
- Rescue module
- Offline map and field-tool components
- Android communication services
- Existing Firebase integrations being progressively separated from the offline architecture

The immediate engineering priority is:

```text
Stable Bluetooth Transport
        ↓
Neighbor Discovery
        ↓
Automatic Route Discovery
        ↓
Multi-Hop Forwarding
        ↓
Route Recovery
        ↓
Physical 3-Device Validation
```

The first success criterion is:

```text
UT-A → UT-C → UT-B
```

where UT-C automatically acts as the relay without user configuration.

---

# Repository

GitHub Repository:

https://github.com/ArpitDalal05/UTMASH

---

# Project Goal

UTMASH aims to evolve into a resilient communication platform where a collection of nearby Android devices can form an autonomous local communication network.

```text
                         INTERNET
                            │
                       Optional
                            │
                            ▼

        ┌───────────────────────────────────┐
        │              UTMASH               │
        │                                   │
        │          OFFLINE UT MESH          │
        │                                   │
        │   ┌─────┐       ┌─────┐           │
        │   │ UT-A├───────┤ UT-B│           │
        │   └──┬──┘       └──┬──┘           │
        │      │             │              │
        │      │   ┌─────┐   │              │
        │      └───┤ UT-C├───┘              │
        │          └──┬──┘                  │
        │             │                     │
        │          ┌──▼──┐                  │
        │          │ UT-D│                  │
        │          └─────┘                  │
        │                                   │
        │   Every device can relay data.    │
        │   No dedicated router required.   │
        └───────────────────────────────────┘
```

**UTMASH — communication that does not stop when the network does.**
