# Release Notes - v1.0.1

**Release Date:** January 2026

---

## Overview

iGambling Core Service v1.0.1 introduces new features for round history querying, image management, and significant architectural improvements with the Saga pattern reorganization and production-ready adapter integrations.

---

## New Features

### Round Details Query
- **GetRoundsDetails** - New gRPC endpoint to retrieve round history with aggregated amounts and game details
- Advanced filtering by player ID, game ID, provider ID, aggregator, date range, and round status
- Pagination support with `offset` and `limit` parameters
- Returns comprehensive round information including:
  - Round status (in-progress, finished)
  - Total bet and win amounts
  - Associated game and provider details
  - Spin transaction history

### Game & Provider Image Management
- **UpdateGameImage** - Upload and update game thumbnail images via gRPC
- **UpdateProviderImage** - Upload and update provider logo images via gRPC
- S3-based file storage with automatic path management
- Supports image replacement and deletion

### gRPC Configuration
- Configurable message size limits (default: 50 MB) for handling large payloads
- Enhanced client integration documentation with configuration examples

---

## Architectural Improvements

### Saga Pattern Reorganization

All spin operations now use a fully modular Saga architecture with dedicated packages and step files:

```
application/saga/spin/
├── place/           # PlaceSpinSaga (6 steps)
│   ├── PlaceSpinSaga.kt
│   └── step/
│       ├── ValidateGameStep.kt
│       ├── FindOrCreateRoundStep.kt
│       ├── ValidateBalanceStep.kt
│       ├── WalletWithdrawStep.kt
│       ├── SavePlaceSpinStep.kt
│       └── PublishSpinPlacedEventStep.kt
│
├── settle/          # SettleSpinSaga (6 steps)
├── end/             # EndSpinSaga (3 steps)
└── rollback/        # RollbackSpinSaga (5 steps)
```

**Benefits:**
- Individual step files for better maintainability and testing
- Clear separation of concerns per operation
- Easier debugging and modification of specific steps
- Consistent compensation handling across all sagas

### Turbo Adapter Integration

Production-ready adapters replacing mock implementations:

| Adapter | Description |
|---------|-------------|
| **TurboWalletAdapter** | HTTP client integration for wallet operations (balance, withdraw, deposit, rollback) |
| **TurboPlayerAdapter** | HTTP client integration for player limit retrieval |

**Wallet DTOs Added:**
- `BalanceType` - Real and bonus balance separation
- `BetTransactionRequest` - Structured bet withdrawal requests
- `SettleTransactionRequest` - Structured win deposit requests
- `AccountDto`, `AccountRequest` - Account management

**Player DTOs Added:**
- `PlayerLimitDto` - Bet limit configuration
- `PlayerResponse` - Player data response

---

## Bug Fixes

- Fixed event publishing for spin operations
- Improved currency conversion with proper rounding in `UnitCurrencyAdapter` and `OneGameHubCurrencyAdapter`
- Simplified S3 file handling by returning keys directly instead of CDN URLs

---

## gRPC API Changes

### New Service: RoundService

| Operation | Description |
|-----------|-------------|
| **GetRoundsDetails** | Query round history with filtering and pagination |

### Updated Services

| Service | New Operations |
|---------|----------------|
| **GameService** | `UpdateImage` - Upload game thumbnail |
| **ProviderService** | `UpdateImage` - Upload provider logo |

---

## Domain Events

New event added:

| Event | Routing Key |
|-------|-------------|
| SpinRollbackEvent | `spin.rollback` |

---

## File Adapter

New `FileAdapter` port for file storage operations:

```kotlin
interface FileAdapter {
    suspend fun upload(path: String, content: ByteArray, contentType: String): String
    suspend fun delete(path: String)
    suspend fun exists(path: String): Boolean
}
```

S3 implementation provided in `infrastructure/external/s3/S3FileAdapter.kt`.

---

## Migration Notes

### From v1.0.0

1. **Saga Imports** - If extending sagas, update imports from:
   - `application/saga/spin/PlaceSpinSaga` → `application/saga/spin/place/PlaceSpinSaga`
   - `application/saga/spin/SettleSpinSaga` → `application/saga/spin/settle/SettleSpinSaga`

2. **Removed UseCases** - The following have been converted to Sagas:
   - `PlaceSpinUsecase` → `PlaceSpinSaga`
   - `SettleSpinUsecase` → `SettleSpinSaga`
   - `EndSpinUsecase` → `EndSpinSaga`
   - `RollbackUsecase` → `RollbackSpinSaga`

3. **Adapter Registration** - If using custom adapters, ensure they're registered in `DependencyInjection.kt` to override the default Turbo adapters.

4. **Collection Model** - `ImageMap` has been removed from collections. Image handling is now separate.

---

## Configuration

### S3 File Storage (Required for Image Features)

```kotlin
// Environment variables
S3_ENDPOINT=your-s3-endpoint
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_BUCKET=your-bucket-name
S3_REGION=your-region
```

### gRPC Message Size

```kotlin
// Server configuration (ApiPlugin.kt)
maxInboundMessageSize = 50 * 1024 * 1024  // 50 MB

// Client configuration
ManagedChannelBuilder.forAddress(host, port)
    .maxInboundMessageSize(50 * 1024 * 1024)
    .build()
```

---

## Dependencies

No new external dependencies added. Internal module organization improved.

---

## Known Issues

- S3 adapter requires proper endpoint configuration for non-AWS S3-compatible storage
- Turbo adapters require external Turbo service to be running

---

## Contributors

- nekzabirov
- Claude Code (AI-assisted development)
