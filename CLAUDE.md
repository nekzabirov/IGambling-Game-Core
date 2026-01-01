# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate gRPC stubs from .proto files
./gradlew generateProto

# Build gRPC client JAR (for external service consumers)
./gradlew grpcClientJar

# Run the application
./gradlew run
```

## Architecture Overview

This is a **Kotlin iGambling Core Service** following **Hexagonal Architecture** (Ports & Adapters) with DDD patterns.

### Layer Structure

```
src/main/kotlin/
├── application/           # Use cases, services, sagas (orchestration)
│   ├── port/outbound/    # Adapter interfaces (WalletAdapter, PlayerAdapter, CacheAdapter)
│   ├── saga/spin/        # Distributed transaction sagas (PlaceSpinSaga, SettleSpinSaga, etc.)
│   └── usecase/          # Application use cases organized by domain
│
├── domain/               # Pure business logic, no external dependencies
│   ├── */                # Bounded contexts: session, game, provider, collection, aggregator
│   └── common/           # Shared: DomainError sealed classes, events, value objects
│
├── infrastructure/       # Technical implementations
│   ├── aggregator/       # Game aggregator integrations (pragmatic, onegamehub, pateplay)
│   ├── api/grpc/         # gRPC service implementations
│   ├── messaging/        # RabbitMQ event publishing
│   └── persistence/      # Exposed ORM repositories
│
└── shared/               # Extensions, serializers, common value objects
```

### Key Architectural Patterns

**Saga Pattern for Spin Operations**: All betting operations (place, settle, end, rollback) use sagas for distributed transactions with automatic compensation on failure. Each saga is in `application/saga/spin/` with dedicated step files.

- `PlaceSpinSaga` (6 steps): validates game → creates round → validates balance → withdraws → saves spin → publishes event
- `SettleSpinSaga` (6 steps): finds round → finds spin → calculates amounts → deposits → saves settle → publishes event
- `EndSpinSaga` (3 steps): finds round → marks finished → publishes event
- `RollbackSpinSaga` (5 steps): finds round → finds original spin → refunds → saves rollback → publishes event

**Aggregator Factory Pattern**: Each game aggregator (Pragmatic, OneGameHub, Pateplay) has its own module in `infrastructure/aggregator/` with:
- `*AdapterFactory` - creates port implementations
- `*Handler` - processes aggregator callbacks using sagas
- `*Config` - aggregator-specific configuration

**Domain Errors**: Type-safe error handling via sealed classes in `domain/common/error/`. Errors include: `NotFoundError`, `InsufficientBalanceError`, `BetLimitExceededError`, `SessionInvalidError`, etc.

**Dependency Injection**: Koin modules in `infrastructure/DependencyInjection.kt`. Key modules: `coreModule()`, `adapterModule`, `sagaModule`, `AggregatorModule`.

## Technology Stack

- Kotlin 2.0.21 (JVM 21)
- Ktor Server 3.0.3
- Exposed ORM 0.57.0 (H2 for dev, PostgreSQL for prod)
- Koin 4.0.3 (DI)
- gRPC + Protocol Buffers (API)
- RabbitMQ (messaging)

## Proto Files

gRPC service definitions are in `src/main/proto/`:
- `service/` - Service definitions (Session, Game, Freespin, Collection, Provider, Sync)
- `dto/` - Data transfer objects

After modifying `.proto` files, run `./gradlew generateProto`.

## Required Custom Adapters

The service ships with mock adapters. For production, implement:

- `WalletAdapter` - balance queries, withdrawals, deposits, rollbacks
- `PlayerAdapter` - bet limit retrieval
- `CacheAdapter` - session caching
- `EventPublisherAdapter` - domain event publishing (RabbitMQ implementation provided)

Register custom implementations in `infrastructure/DependencyInjection.kt`.

## Adding a New Aggregator

1. Add enum value to `shared/value/Enums.kt`
2. Create package `infrastructure/aggregator/youraggregator/` with:
   - `model/YourConfig.kt` - configuration class
   - `adapter/YourLaunchUrlAdapter.kt`, `YourFreespinAdapter.kt`, `YourGameSyncAdapter.kt`
   - `YourAdapterFactory.kt` - implements `AggregatorAdapterFactory`
   - `YourHandler.kt` - callback handler using sagas
3. Create Koin module and register in `AggregatorModule`
4. Add REST routes for callbacks

## Event Routing Keys

Events published to RabbitMQ:
- `spin.placed`, `spin.settled`, `spin.end`, `spin.rollback`
- `session.opened`
- `game.favourite.added`, `game.favourite.removed`, `game.won`
