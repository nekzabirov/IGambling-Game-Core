# Game Core gRPC API Reference

Complete API reference for the Game Core gRPC services.

**Protocol:** gRPC
**Package:** `game.service`
**Java Package:** `com.nekgamebling.game.service`

---

## Table of Contents

- [Services](#services)
  - [GameService](#gameservice)
  - [RoundService](#roundservice)
  - [ProviderService](#providerservice)
  - [CollectionService](#collectionservice)
  - [AggregatorService](#aggregatorservice)
- [Enums](#enums)
- [Common DTOs](#common-dtos)
- [Error Handling](#error-handling)

---

## Services

### GameService

Game management, session handling, and freespin operations.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindGameQuery` | `FindGameResult` | Get a single game by identity |
| `FindAll` | `FindAllGameQuery` | `FindAllGameResult` | List games with filters and pagination |
| `Play` | `PlayGameCommand` | `PlayGameResult` | Open a game session (real money) |
| `DemoUrl` | `GameDemoUrlQuery` | `GameDemoUrlResult` | Get demo URL (no real money) |
| `Update` | `UpdateGameCommand` | `UpdateGameResult` | Update game configuration |
| `UpdateImage` | `UpdateGameImageCommand` | `UpdateGameImageResult` | Upload/update game image |
| `AddTag` | `AddGameTagCommand` | `AddGameTagResult` | Add tag to a game |
| `RemoveTag` | `RemoveGameTagCommand` | `RemoveGameTagResult` | Remove tag from a game |
| `GetFreespinPreset` | `GetFreespinPresetQuery` | `GetFreespinPresetResult` | Get freespin preset options |
| `CreateFreespin` | `CreateFreespinCommand` | `CreateFreespinResult` | Create a freespin for player |
| `CancelFreespin` | `CancelFreespinCommand` | `CancelFreespinResult` | Cancel a freespin |

---

#### Find

Get a single game with full details including provider, variant, and aggregator info.

**Request: `FindGameQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Unique game identifier |

**Response: `FindGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `game` | `GameDto` | Game entity |
| `provider` | `ProviderDto` | Provider details |
| `active_variant` | `GameVariantDto` | Active game variant |
| `aggregator` | `AggregatorInfoDto` | Aggregator configuration |
| `collections` | `CollectionDto[]` | Collections containing this game |

---

#### FindAll

List games with pagination and filters.

**Request: `FindAllGameQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by name or identity |
| `active` | `bool` | No | Filter by active status |
| `provider_identities` | `string[]` | No | Filter by provider identities |
| `collection_identities` | `string[]` | No | Filter by collection identities |
| `tags` | `string[]` | No | Filter by tags |
| `bonus_bet_enable` | `bool` | No | Filter by bonus bet support |
| `bonus_wagering_enable` | `bool` | No | Filter by bonus wagering support |
| `free_spin_enable` | `bool` | No | Filter by freespin support |
| `free_chip_enable` | `bool` | No | Filter by free chip support |
| `jackpot_enable` | `bool` | No | Filter by jackpot support |

**Response: `FindAllGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `GameItemDto[]` | List of game items |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `providers` | `ProviderDto[]` | Related providers from results |
| `aggregators` | `AggregatorInfoDto[]` | Related aggregators from results |
| `collections` | `CollectionDto[]` | Related collections from results |

---

#### Play

Open a game session for real money play. Returns a launch URL.

**Request: `PlayGameCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `player_id` | `string` | Yes | Player identifier |
| `currency` | `string` | Yes | Currency code (e.g., "USD", "EUR") |
| `locale` | `string` | Yes | Locale code (e.g., "en", "de") |
| `platform` | `PlatformDto` | Yes | Platform type |
| `lobby_url` | `string` | Yes | URL to return to lobby |

**Response: `PlayGameResult`**

| Field | Type | Description |
|-------|------|-------------|
| `launch_url` | `string` | URL to launch the game |

**Errors:**
- `NOT_FOUND` (1000) - Game not found
- `GAME_UNAVAILABLE` (4000) - Game is disabled or unavailable
- `SESSION_INVALID` (3000) - Session creation failed

---

#### DemoUrl

Get a demo URL for playing without real money.

**Request: `GameDemoUrlQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `currency` | `string` | Yes | Currency code |
| `locale` | `string` | Yes | Locale code |
| `platform` | `PlatformDto` | Yes | Platform type |
| `lobby_url` | `string` | Yes | URL to return to lobby |

**Response: `GameDemoUrlResult`**

| Field | Type | Description |
|-------|------|-------------|
| `launch_url` | `string` | Demo game URL |

---

#### Update

Update game configuration.

**Request: `UpdateGameCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `bonus_bet_enable` | `bool` | No | Enable/disable bonus bet |
| `bonus_wagering_enable` | `bool` | No | Enable/disable bonus wagering |
| `active` | `bool` | No | Enable/disable game |

**Response: `UpdateGameResult`**

Empty response. Success indicated by no error.

---

#### UpdateImage

Upload or update a game image.

**Request: `UpdateGameImageCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `key` | `string` | Yes | Image key (e.g., "thumbnail", "banner") |
| `file` | `bytes` | Yes | Image binary data |
| `extension` | `string` | Yes | File extension (e.g., "png", "jpg") |

**Response: `UpdateGameImageResult`**

Empty response. Success indicated by no error.

---

#### AddTag / RemoveTag

Add or remove a tag from a game.

**Request: `AddGameTagCommand` / `RemoveGameTagCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Game identifier |
| `tag` | `string` | Yes | Tag name |

**Response: `AddGameTagResult` / `RemoveGameTagResult`**

Empty response. Success indicated by no error.

---

#### GetFreespinPreset

Get available freespin preset configuration for a game.

**Request: `GetFreespinPresetQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |

**Response: `GetFreespinPresetResult`**

| Field | Type | Description |
|-------|------|-------------|
| `preset` | `map<string, string>` | Preset field names and their available values |

---

#### CreateFreespin

Create a freespin bonus for a player.

**Request: `CreateFreespinCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |
| `player_id` | `string` | Yes | Player identifier |
| `reference_id` | `string` | Yes | Unique reference ID (for idempotency) |
| `currency` | `string` | Yes | Currency code |
| `start_at` | `TimestampDto` | Yes | Freespin validity start |
| `end_at` | `TimestampDto` | Yes | Freespin validity end |
| `preset_values` | `map<string, int32>` | Yes | Selected preset values |

**Response: `CreateFreespinResult`**

Empty response. Success indicated by no error.

**Errors:**
- `NOT_FOUND` (1000) - Game not found
- `INVALID_PRESET` (4003) - Invalid preset configuration
- `AGGREGATOR_NOT_SUPPORTED` (5000) - Aggregator doesn't support freespins

---

#### CancelFreespin

Cancel an existing freespin.

**Request: `CancelFreespinCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `game_identity` | `string` | Yes | Game identifier |
| `reference_id` | `string` | Yes | Freespin reference ID |

**Response: `CancelFreespinResult`**

Empty response. Success indicated by no error.

---

### RoundService

Query betting rounds with aggregated amounts.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindRoundQuery` | `FindRoundResult` | Get a single round by ID |
| `FindAll` | `FindAllRoundQuery` | `FindAllRoundResult` | List rounds with filters |

---

#### Find

Get a single round by ID.

**Request: `FindRoundQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | Yes | Round UUID |

**Response: `FindRoundResult`**

| Field | Type | Description |
|-------|------|-------------|
| `item` | `RoundItemDto` | Round with aggregated data |

---

#### FindAll

List rounds with pagination and filters.

**Request: `FindAllRoundQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `game_identity` | `string` | No | Filter by game |
| `provider_identity` | `string` | No | Filter by provider |
| `player_id` | `string` | No | Filter by player |
| `free_spin_id` | `string` | No | Filter by freespin ID |
| `finished` | `bool` | No | Filter by completion status |
| `start_at` | `google.protobuf.Timestamp` | No | Filter: created_at >= start_at |
| `end_at` | `google.protobuf.Timestamp` | No | Filter: created_at <= end_at |

**Response: `FindAllRoundResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `RoundItemDto[]` | List of rounds |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `providers` | `ProviderDto[]` | Related providers |
| `games` | `GameDto[]` | Related games |

---

### ProviderService

Manage game providers.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindProviderQuery` | `FindProviderResult` | Get a single provider |
| `FindAll` | `FindAllProviderQuery` | `FindAllProviderResult` | List providers |
| `Update` | `UpdateProviderCommand` | `UpdateProviderResult` | Update provider config |
| `UpdateImage` | `UpdateProviderImageCommand` | `UpdateProviderImageResult` | Upload provider image |

---

#### Find

Get a single provider with game counts.

**Request: `FindProviderQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |

**Response: `FindProviderResult`**

| Field | Type | Description |
|-------|------|-------------|
| `provider` | `ProviderDto` | Provider entity |
| `aggregator` | `AggregatorInfoDto` | Aggregator info |
| `active_games` | `int32` | Count of active games |
| `total_games` | `int32` | Total game count |

---

#### FindAll

List providers with pagination and filters.

**Request: `FindAllProviderQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by name or identity |
| `active` | `bool` | No | Filter by active status |
| `aggregator_identity` | `string` | No | Filter by aggregator |

**Response: `FindAllProviderResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `ProviderItemDto[]` | List of providers |
| `pagination` | `PaginationMetaDto` | Pagination metadata |
| `aggregators` | `AggregatorInfoDto[]` | Related aggregators |

---

#### Update

Update provider configuration.

**Request: `UpdateProviderCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |
| `active` | `bool` | No | Enable/disable provider |
| `order` | `int32` | No | Display order |
| `aggregator_identity` | `string` | No | Assign to aggregator |

**Response: `UpdateProviderResult`**

Empty response. Success indicated by no error.

---

#### UpdateImage

Upload or update a provider image.

**Request: `UpdateProviderImageCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Provider identifier |
| `key` | `string` | Yes | Image key (e.g., "logo") |
| `file` | `bytes` | Yes | Image binary data |
| `extension` | `string` | Yes | File extension |

**Response: `UpdateProviderImageResult`**

Empty response. Success indicated by no error.

---

### CollectionService

Manage game collections/categories.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Find` | `FindCollectionQuery` | `FindCollectionResult` | Get a single collection |
| `FindAll` | `FindAllCollectionQuery` | `FindAllCollectionResult` | List collections |
| `Update` | `UpdateCollectionCommand` | `UpdateCollectionResult` | Update collection config |
| `UpdateGames` | `UpdateCollectionGamesCommand` | `UpdateCollectionGamesResult` | Add/remove games |

---

#### Find

Get a single collection with counts.

**Request: `FindCollectionQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |

**Response: `FindCollectionResult`**

| Field | Type | Description |
|-------|------|-------------|
| `collection` | `CollectionDto` | Collection entity |
| `provider_count` | `int32` | Number of unique providers |
| `game_count` | `int32` | Number of games |

---

#### FindAll

List collections with pagination and filters.

**Request: `FindAllCollectionQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by identity |
| `active` | `bool` | No | Filter by active status |

**Response: `FindAllCollectionResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `CollectionItemDto[]` | List of collections |
| `pagination` | `PaginationMetaDto` | Pagination metadata |

---

#### Update

Update collection configuration.

**Request: `UpdateCollectionCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |
| `active` | `bool` | No | Enable/disable collection |
| `order` | `int32` | No | Display order |

**Response: `UpdateCollectionResult`**

Empty response. Success indicated by no error.

---

#### UpdateGames

Add or remove games from a collection.

**Request: `UpdateCollectionGamesCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Collection identifier |
| `add_games` | `string[]` | No | Game identities to add |
| `remove_games` | `string[]` | No | Game identities to remove |

**Response: `UpdateCollectionGamesResult`**

Empty response. Success indicated by no error.

---

### AggregatorService

Manage aggregator configurations.

#### Methods

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| `Create` | `CreateAggregatorCommand` | `CreateAggregatorResult` | Create new aggregator |
| `Find` | `FindAggregatorQuery` | `FindAggregatorResult` | Get a single aggregator |
| `FindAll` | `FindAllAggregatorQuery` | `FindAllAggregatorResult` | List aggregators |
| `Update` | `UpdateAggregatorCommand` | `UpdateAggregatorResult` | Update aggregator config |

---

#### Create

Create a new aggregator configuration.

**Request: `CreateAggregatorCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Unique identifier |
| `aggregator` | `AggregatorTypeDto` | Yes | Aggregator type |
| `config` | `map<string, string>` | Yes | Configuration key-value pairs |
| `active` | `bool` | No | Initial active status |

**Response: `CreateAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `aggregator` | `AggregatorInfoDto` | Created aggregator |

**Errors:**
- `DUPLICATE_ENTITY` (1002) - Aggregator with this identity exists
- `VALIDATION_ERROR` (1001) - Invalid configuration

---

#### Find

Get a single aggregator by identity.

**Request: `FindAggregatorQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Aggregator identifier |

**Response: `FindAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `aggregator` | `AggregatorInfoDto` | Aggregator details |

---

#### FindAll

List aggregators with pagination and filters.

**Request: `FindAllAggregatorQuery`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pagination` | `PaginationRequestDto` | Yes | Page and size |
| `query` | `string` | No | Search by identity |
| `active` | `bool` | No | Filter by active status |

**Response: `FindAllAggregatorResult`**

| Field | Type | Description |
|-------|------|-------------|
| `items` | `AggregatorInfoDto[]` | List of aggregators |
| `pagination` | `PaginationMetaDto` | Pagination metadata |

---

#### Update

Update aggregator configuration.

**Request: `UpdateAggregatorCommand`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `identity` | `string` | Yes | Aggregator identifier |
| `active` | `bool` | No | Enable/disable aggregator |
| `config` | `map<string, string>` | No | Configuration to update |

**Response: `UpdateAggregatorResult`**

Empty response. Success indicated by no error.

---

## Enums

### PlatformDto

Player platform type.

| Name | Value | Description |
|------|-------|-------------|
| `PLATFORM_UNSPECIFIED` | 0 | Not specified |
| `PLATFORM_DESKTOP` | 1 | Desktop browser |
| `PLATFORM_MOBILE` | 2 | Mobile device |
| `PLATFORM_DOWNLOAD` | 3 | Downloadable client |

### AggregatorTypeDto

Supported game aggregators.

| Name | Value | Description |
|------|-------|-------------|
| `AGGREGATOR_UNSPECIFIED` | 0 | Not specified |
| `AGGREGATOR_ONEGAMEHUB` | 1 | OneGameHub |
| `AGGREGATOR_PRAGMATIC` | 2 | Pragmatic Play |
| `AGGREGATOR_PATEPLAY` | 3 | Pateplay |

### SpinTypeDto

Type of spin/bet transaction.

| Name | Value | Description |
|------|-------|-------------|
| `SPIN_TYPE_UNSPECIFIED` | 0 | Not specified |
| `SPIN_TYPE_PLACE` | 1 | Bet placement |
| `SPIN_TYPE_SETTLE` | 2 | Win settlement |
| `SPIN_TYPE_ROLLBACK` | 3 | Bet rollback/refund |

### RoundStatusDto

Round completion status.

| Name | Value | Description |
|------|-------|-------------|
| `ROUND_STATUS_UNSPECIFIED` | 0 | Not specified |
| `ROUND_STATUS_ACTIVE` | 1 | Round in progress |
| `ROUND_STATUS_FINISHED` | 2 | Round completed |

### ErrorCodeDto

Error codes for structured error handling.

| Name | Value | Category | Description |
|------|-------|----------|-------------|
| `ERROR_CODE_UNSPECIFIED` | 0 | - | Not specified |
| `ERROR_CODE_NOT_FOUND` | 1000 | General | Entity not found |
| `ERROR_CODE_VALIDATION_ERROR` | 1001 | General | Validation failed |
| `ERROR_CODE_DUPLICATE_ENTITY` | 1002 | General | Entity already exists |
| `ERROR_CODE_ILLEGAL_STATE` | 1003 | General | Invalid operation state |
| `ERROR_CODE_INSUFFICIENT_BALANCE` | 2000 | Balance | Insufficient funds |
| `ERROR_CODE_BET_LIMIT_EXCEEDED` | 2001 | Balance | Bet exceeds limit |
| `ERROR_CODE_SESSION_INVALID` | 3000 | Session | Session expired/invalid |
| `ERROR_CODE_GAME_UNAVAILABLE` | 4000 | Game | Game not available |
| `ERROR_CODE_ROUND_FINISHED` | 4001 | Game | Round already completed |
| `ERROR_CODE_ROUND_NOT_FOUND` | 4002 | Game | Round not found |
| `ERROR_CODE_INVALID_PRESET` | 4003 | Game | Invalid freespin preset |
| `ERROR_CODE_AGGREGATOR_NOT_SUPPORTED` | 5000 | Aggregator | Aggregator not supported |
| `ERROR_CODE_AGGREGATOR_ERROR` | 5001 | Aggregator | Aggregator returned error |
| `ERROR_CODE_EXTERNAL_SERVICE_ERROR` | 6000 | External | External service failed |
| `ERROR_CODE_INTERNAL_ERROR` | 9999 | Internal | Internal server error |

---

## Common DTOs

### PaginationRequestDto

Pagination request parameters.

| Field | Type | Description |
|-------|------|-------------|
| `page` | `int32` | Page number (0-indexed) |
| `size` | `int32` | Items per page |

### PaginationMetaDto

Pagination response metadata.

| Field | Type | Description |
|-------|------|-------------|
| `page` | `int32` | Current page number |
| `size` | `int32` | Items per page |
| `total_elements` | `int64` | Total number of items |
| `total_pages` | `int32` | Total number of pages |

### TimestampDto

Timestamp representation.

| Field | Type | Description |
|-------|------|-------------|
| `seconds` | `int64` | Seconds since Unix epoch |
| `nanos` | `int32` | Nanoseconds (0-999,999,999) |

### LocaleNameDto

Localized name map.

| Field | Type | Description |
|-------|------|-------------|
| `values` | `map<string, string>` | Locale code to name (e.g., `{"en": "Slots", "de": "Spielautomaten"}`) |

### ImageMapDto

Image URL map.

| Field | Type | Description |
|-------|------|-------------|
| `images` | `map<string, string>` | Image key to URL (e.g., `{"thumbnail": "https://..."}`) |

### GameDto

Core game entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `string` | Game name |
| `provider_identity` | `string` | Provider reference |
| `images` | `ImageMapDto` | Game images |
| `bonus_bet_enable` | `bool` | Bonus bet support |
| `bonus_wagering_enable` | `bool` | Bonus wagering support |
| `tags` | `string[]` | Game tags |
| `active` | `bool` | Active status |

### GameVariantDto

Game variant for a specific aggregator.

| Field | Type | Description |
|-------|------|-------------|
| `symbol` | `string` | Game symbol/code |
| `game_identity` | `string` | Game reference (optional) |
| `name` | `string` | Variant name |
| `provider_name` | `string` | Provider name from aggregator |
| `aggregator` | `AggregatorTypeDto` | Aggregator type |
| `free_spin_enable` | `bool` | Freespin support |
| `free_chip_enable` | `bool` | Free chip support |
| `jackpot_enable` | `bool` | Jackpot support |
| `demo_enable` | `bool` | Demo mode support |
| `bonus_buy_enable` | `bool` | Bonus buy feature |
| `locales` | `string[]` | Supported locales |
| `platforms` | `PlatformDto[]` | Supported platforms |
| `play_lines` | `int32` | Number of play lines |

### GameItemDto

Game with variant and collection references.

| Field | Type | Description |
|-------|------|-------------|
| `game` | `GameDto` | Game entity |
| `active_variant` | `GameVariantDto` | Active variant |
| `collection_identities` | `string[]` | Collection references |

### ProviderDto

Provider entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `string` | Provider name |
| `images` | `ImageMapDto` | Provider images |
| `order` | `int32` | Display order |
| `aggregator_identity` | `string` | Aggregator reference (optional) |
| `active` | `bool` | Active status |

### ProviderItemDto

Provider with game statistics.

| Field | Type | Description |
|-------|------|-------------|
| `provider` | `ProviderDto` | Provider entity |
| `aggregator_identity` | `string` | Aggregator reference |
| `active_games` | `int32` | Active game count |
| `total_games` | `int32` | Total game count |

### CollectionDto

Collection/category entity.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `name` | `LocaleNameDto` | Localized names |
| `active` | `bool` | Active status |
| `order` | `int32` | Display order |

### CollectionItemDto

Collection with statistics.

| Field | Type | Description |
|-------|------|-------------|
| `collection` | `CollectionDto` | Collection entity |
| `provider_count` | `int32` | Unique provider count |
| `game_count` | `int32` | Game count |

### AggregatorInfoDto

Aggregator configuration.

| Field | Type | Description |
|-------|------|-------------|
| `identity` | `string` | Unique identifier |
| `config` | `map<string, string>` | Configuration key-values |
| `aggregator` | `AggregatorTypeDto` | Aggregator type |
| `active` | `bool` | Active status |

### RoundDto

Round entity.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | UUID |
| `session_id` | `string` | Session UUID |
| `game_id` | `string` | Game UUID |
| `ext_id` | `string` | External round ID |
| `finished` | `bool` | Completion status |
| `created_at` | `TimestampDto` | Creation timestamp |
| `finished_at` | `TimestampDto` | Finish timestamp (optional) |

### RoundItemDto

Round with aggregated amounts.

| Field | Type | Description |
|-------|------|-------------|
| `round` | `RoundDto` | Round entity |
| `provider_identity` | `string` | Provider identifier |
| `game_identity` | `string` | Game identifier |
| `player_id` | `string` | Player identifier |
| `currency` | `string` | Currency code |
| `total_place_real` | `int64` | Total real money placed (minor units) |
| `total_place_bonus` | `int64` | Total bonus money placed |
| `total_settle_real` | `int64` | Total real money won |
| `total_settle_bonus` | `int64` | Total bonus money won |

### SpinDto

Spin/transaction entity.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `string` | UUID |
| `round_id` | `string` | Round UUID |
| `type` | `SpinTypeDto` | Transaction type |
| `amount` | `string` | Total amount (BigInteger string) |
| `real_amount` | `string` | Real money amount |
| `bonus_amount` | `string` | Bonus amount |
| `ext_id` | `string` | External transaction ID |
| `reference_id` | `string` | Reference ID (optional) |
| `free_spin_id` | `string` | Freespin ID (optional) |

---

## Error Handling

All errors are returned as gRPC `StatusException` with structured metadata.

### gRPC Status Mapping

| Error Code | gRPC Status |
|------------|-------------|
| `NOT_FOUND` (1000) | `NOT_FOUND` |
| `ROUND_NOT_FOUND` (4002) | `NOT_FOUND` |
| `VALIDATION_ERROR` (1001) | `INVALID_ARGUMENT` |
| `INVALID_PRESET` (4003) | `INVALID_ARGUMENT` |
| `DUPLICATE_ENTITY` (1002) | `ALREADY_EXISTS` |
| `INSUFFICIENT_BALANCE` (2000) | `FAILED_PRECONDITION` |
| `BET_LIMIT_EXCEEDED` (2001) | `FAILED_PRECONDITION` |
| `ROUND_FINISHED` (4001) | `FAILED_PRECONDITION` |
| `ILLEGAL_STATE` (1003) | `FAILED_PRECONDITION` |
| `SESSION_INVALID` (3000) | `UNAUTHENTICATED` |
| `GAME_UNAVAILABLE` (4000) | `UNAVAILABLE` |
| `EXTERNAL_SERVICE_ERROR` (6000) | `UNAVAILABLE` |
| `AGGREGATOR_NOT_SUPPORTED` (5000) | `UNIMPLEMENTED` |
| `AGGREGATOR_ERROR` (5001) | `INTERNAL` |
| `INTERNAL_ERROR` (9999) | `INTERNAL` |

### Error Metadata Headers

All errors include these metadata headers (gRPC trailers):

| Header | Description |
|--------|-------------|
| `x-error-code` | Error code name (e.g., `NOT_FOUND`) |
| `x-error-code-value` | Numeric code (e.g., `1000`) |

Additional context headers by error type:

| Header | Used By | Description |
|--------|---------|-------------|
| `x-entity-type` | NOT_FOUND, DUPLICATE_ENTITY | Entity type |
| `x-identifier` | NOT_FOUND, ROUND_NOT_FOUND, etc. | Entity ID |
| `x-field` | VALIDATION_ERROR, ILLEGAL_STATE | Field/operation name |
| `x-reason` | Multiple | Additional context |
| `x-player-id` | INSUFFICIENT_BALANCE, BET_LIMIT_EXCEEDED | Player ID |
| `x-required-amount` | INSUFFICIENT_BALANCE | Required amount |
| `x-available-amount` | INSUFFICIENT_BALANCE | Available balance |
| `x-bet-amount` | BET_LIMIT_EXCEEDED | Attempted bet |
| `x-limit` | BET_LIMIT_EXCEEDED | Configured limit |
| `x-service` | EXTERNAL_SERVICE_ERROR | Service name |

### Example: Parsing Errors

**Go:**
```go
status := status.Convert(err)
if status.Code() == codes.NotFound {
    for _, detail := range status.Details() {
        // handle metadata
    }
}
```

**Python:**
```python
try:
    response = stub.Find(request)
except grpc.RpcError as e:
    if e.code() == grpc.StatusCode.NOT_FOUND:
        metadata = dict(e.trailing_metadata())
        error_code = metadata.get('x-error-code')
```

**Node.js:**
```javascript
client.Find(request, (err, response) => {
    if (err) {
        const metadata = err.metadata.getMap();
        const errorCode = metadata['x-error-code'];
    }
});
```

**Java/Kotlin:**
```kotlin
try {
    val response = stub.find(request)
} catch (e: StatusException) {
    val metadata = Status.trailersFromThrowable(e)
    val errorCode = metadata?.get(Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER))
}
```

---

## Connection

**Default Port:** `5050`

### Client Configuration Recommendations

- **Timeout:** Set deadlines (5-30 seconds depending on operation)
- **Keep-alive:** Enable for long-running connections
- **Max message size:** 50 MB recommended for image uploads
- **TLS:** Required for production environments
