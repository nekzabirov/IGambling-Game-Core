# Release Notes - v1.0.0

**Release Date:** December 2025

---

## Overview

iGambling Core Service v1.0.0 is the initial stable release of a production-ready, modular gaming platform built on hexagonal architecture. This release provides comprehensive game aggregator integration, session management, and wallet operations for online gaming platforms.

---

## Features

### Session Management
- **Open Session** - Create player gaming sessions with secure token generation
- **Session Caching** - 5-minute TTL for optimized performance
- Automatic session token validation and management
- Multi-platform support (Desktop, Mobile, Download)
- Locale and currency configuration per session

### Spin (Betting) Operations
- **Place Spin** - Process bets with balance validation and limit checks
- **Settle Spin** - Record win/loss outcomes with automatic fund deposits
- **Rollback** - Reverse transactions with full refund support
- Real + Bonus balance separation for accurate wagering
- Bet limit enforcement per player

### Freespin Management
- **Get Preset** - Retrieve freespin configurations from aggregators
- **Create Freespin** - Award freespin bonuses with customizable parameters
- **Cancel Freespin** - Revoke active freespin offers
- Currency conversion support across aggregators
- Configurable start/end dates for freespin validity

### Game Management
- **List Games** - Advanced filtering (active, bonus settings, platforms, providers, tags)
- **Find Game** - Retrieve individual game details
- **Update Game** - Configure game settings (active status, bonus eligibility)
- **Demo Mode** - Launch games in demo/free-play mode
- **Favorites** - Add/remove games from player favorites
- **Tagging** - Organize games with custom tags
- **Win Recording** - Track and display game wins

### Collection Management
- **Create Collections** - Organize games into themed groups
- **Localized Names** - Multi-language support for collection names
- **Game Assignment** - Add/remove games from collections
- **Ordering** - Custom sort order for games within collections
- Pagination support for large catalogs

### Provider Management
- **List Providers** - Browse game providers with filtering
- **Update Provider** - Configure provider settings
- **Aggregator Assignment** - Link providers to aggregators for sync

### Aggregator Management
- **Add Aggregator** - Register new aggregators with custom configuration
- **List Aggregators** - Browse registered aggregators
- **Sync Games** - Import games from aggregator catalogs
- **Game Variants** - Manage platform/locale-specific game versions

---

## Integrated Aggregators

### 1. Pragmatic Play (`PRAGMATIC`)

Full integration with Pragmatic Play gaming platform.

**Configuration:**
- `secretKey` - API secret key
- `secureLogin` - Secure login identifier
- `gatewayUrl` - Pragmatic API gateway URL

**Supported Operations:**
- Game launch URL generation
- Freespin management with currency conversion
- Game catalog synchronization
- Wallet callbacks (authenticate, balance, bet, result, endRound, refund, adjustment)

---

### 2. OneGameHub (`ONEGAMEHUB`)

Integration with OneGameHub aggregator platform.

**Configuration:**
- `salt` - Encryption salt
- `secret` - API secret
- `partner` - Partner identifier
- `gateway` - OneGameHub API gateway URL

**Supported Operations:**
- Game launch URL generation
- Freespin management
- Game catalog synchronization
- Wallet callbacks (balance, bet, win, cancel)

**Error Mapping:**
- `BetLimitExceededError` → `EXCEED_WAGER_LIMIT`
- `InsufficientBalanceError` → `INSUFFICIENT_FUNDS`
- `SessionInvalidError` → `SESSION_TIMEOUT`
- `GameUnavailableError` → `UNAUTHORIZED`

---

### 3. Pateplay (`PATEPLAY`)

Integration with Pateplay gaming aggregator.

**Configuration:**
- `gatewayUrl` - Pateplay API gateway URL
- `siteCode` - Site identifier
- `gatewayApiKey` - Gateway API key
- `gatewayApiSecret` - Gateway API secret
- `gameLaunchUrl` - Base URL for game launch
- `gameDemoLaunchUrl` - Base URL for demo games
- `walletApiKey` - Wallet API key
- `walletApiSecret` - Wallet API secret

**Supported Operations:**
- Game launch URL generation with secure signing
- Freespin management
- Game catalog synchronization

---

## gRPC API Services

| Service | Operations |
|---------|------------|
| **SessionService** | OpenSession |
| **GameService** | Find, List, Update, AddTag, RemoveTag, AddFavourite, RemoveFavourite, DemoGame |
| **FreespinService** | GetPreset, CreateFreespin, CancelFreespin |
| **CollectionService** | AddCollection, UpdateCollection, AddGameCollection, RemoveGameFromCollection, ChangeGameOrder, List |
| **ProviderService** | List, Update |
| **SyncService** | AddAggregator, ListAggregator, ListVariants, AssignGameVariant, AssignProvider |

---

## Domain Events (RabbitMQ)

| Event | Routing Key |
|-------|-------------|
| SpinPlacedEvent | `spin.placed` |
| SpinSettledEvent | `spin.settled` |
| SessionOpenedEvent | `session.opened` |
| SessionClosedEvent | `session.closed` |
| GameFavouriteAddedEvent | `game.favourite.added` |
| GameFavouriteRemovedEvent | `game.favourite.removed` |
| GameWonEvent | `game.won` |

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin (JVM 21) |
| Framework | Ktor Server |
| Database ORM | Exposed |
| Databases | H2, PostgreSQL |
| Dependency Injection | Koin |
| Messaging | RabbitMQ (AMQP) |
| API Protocol | gRPC + REST |
| Serialization | kotlinx.serialization |

---

## Architecture

The service follows **Hexagonal Architecture** (Ports & Adapters):

```
API Layer (gRPC/REST)
        ↓
Application Layer (Use Cases, Services, Events)
        ↓
Domain Layer (Entities, Repositories, Errors)
        ↓
Infrastructure Layer (Aggregators, Persistence)
```

---

## Required Custom Adapters

The following adapters must be implemented for production deployment:

- **WalletAdapter** - Player balance and transaction operations
- **PlayerAdapter** - Player bet limit retrieval
- **CacheAdapter** - Session and aggregator caching
- **EventPublisherAdapter** - Domain event publication

---

## Known Limitations

- Mock adapters included for development/testing only
- Webhook handlers for Pragmatic and Pateplay require implementation
- H2 database for development; PostgreSQL recommended for production

---

## Getting Started

1. Configure aggregator credentials via `AddAggregator` gRPC call
2. Implement required custom adapters (Wallet, Player, Cache, EventPublisher)
3. Sync games from aggregators using `SyncService`
4. Configure game collections and provider settings
5. Open sessions and process spins via gRPC API

---

## gRPC Client

A standalone gRPC client JAR is published for external integration:

```
com.nekzabirov.igambling:game-core-grpc-client
```

Available via GitHub Maven Packages.
