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
               │   UT-A  │             │   UT-B  │
               │ Source  │             │   Dest  │
               └─────────┘             └─────────┘

                    A → C → B
