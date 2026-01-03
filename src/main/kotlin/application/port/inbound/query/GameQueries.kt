package application.port.inbound.query

import application.port.inbound.Query
import application.query.game.GameDetailsReadModel
import application.query.game.GameListReadModel
import application.query.game.GameSummaryReadModel
import domain.game.repository.GameFilter
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Query to find a single game by ID.
 */
data class FindGameByIdQuery(
    val id: UUID
) : Query<GameDetailsReadModel?>

/**
 * Query to find a single game by identity.
 */
data class FindGameByIdentityQuery(
    val identity: String
) : Query<GameDetailsReadModel?>

/**
 * Query to find a single game by symbol.
 */
data class FindGameBySymbolQuery(
    val symbol: String
) : Query<GameDetailsReadModel?>

/**
 * Query to list games with pagination and filtering.
 */
data class ListGamesQuery(
    val pageable: Pageable,
    val filter: GameFilter = GameFilter.EMPTY
) : Query<Page<GameListReadModel>>

/**
 * Query to search games by name or identity.
 */
data class SearchGamesQuery(
    val query: String,
    val limit: Int = 20
) : Query<List<GameSummaryReadModel>>
