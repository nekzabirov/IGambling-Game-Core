package infrastructure.persistence.exposed.mapper

import domain.aggregator.model.AggregatorInfo
import domain.collection.model.Collection
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.game.model.GameWithDetails
import domain.provider.model.Provider
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import infrastructure.persistence.exposed.table.*
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform
import org.jetbrains.exposed.sql.ResultRow

/**
 * Mapper functions to convert database rows to domain models.
 */

fun ResultRow.toGame(): Game = Game(
    id = this[GameTable.id].value,
    identity = this[GameTable.identity],
    name = this[GameTable.name],
    providerId = this[GameTable.providerId].value,
    images = this[GameTable.images],
    bonusBetEnable = this[GameTable.bonusBetEnable],
    bonusWageringEnable = this[GameTable.bonusWageringEnable],
    tags = this[GameTable.tags],
    active = this[GameTable.active]
)

fun ResultRow.toGameVariant(): GameVariant = GameVariant(
    id = this[GameVariantTable.id].value,
    gameId = this[GameVariantTable.gameId]?.value,
    symbol = this[GameVariantTable.symbol],
    name = this[GameVariantTable.name],
    providerName = this[GameVariantTable.providerName],
    aggregator = this[GameVariantTable.aggregator],
    freeSpinEnable = this[GameVariantTable.freeSpinEnable],
    freeChipEnable = this[GameVariantTable.freeChipEnable],
    jackpotEnable = this[GameVariantTable.jackpotEnable],
    demoEnable = this[GameVariantTable.demoEnable],
    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],
    locales = this[GameVariantTable.locales].map { Locale(it) },
    platforms = this[GameVariantTable.platforms].map { Platform.valueOf(it) },
    playLines = this[GameVariantTable.playLines]
)

fun ResultRow.toProvider(): Provider = Provider(
    id = this[ProviderTable.id].value,
    identity = this[ProviderTable.identity],
    name = this[ProviderTable.name],
    images = this[ProviderTable.images],
    order = this[ProviderTable.order],
    aggregatorId = this[ProviderTable.aggregatorId]?.value,
    active = this[ProviderTable.active],
)

fun ResultRow.toAggregatorInfo(): AggregatorInfo = AggregatorInfo(
    id = this[AggregatorInfoTable.id].value,
    identity = this[AggregatorInfoTable.identity],
    config = this[AggregatorInfoTable.config],
    aggregator = this[AggregatorInfoTable.aggregator],
    active = this[AggregatorInfoTable.active]
)

fun ResultRow.toCollection(): Collection = Collection(
    id = this[CollectionTable.id].value,
    identity = this[CollectionTable.identity],
    name = this[CollectionTable.name],
    active = this[CollectionTable.active],
    order = this[CollectionTable.order]
)

fun ResultRow.toSession(): Session = Session(
    id = this[SessionTable.id].value,
    gameId = this[SessionTable.gameId].value,
    aggregatorId = this[SessionTable.aggregatorId].value,
    playerId = this[SessionTable.playerId],
    token = this[SessionTable.token],
    externalToken = this[SessionTable.externalToken],
    currency = Currency(this[SessionTable.currency]),
    locale = Locale(this[SessionTable.locale]),
    platform = this[SessionTable.platform]
)

fun ResultRow.toRound(): Round = Round(
    id = this[RoundTable.id].value,
    sessionId = this[RoundTable.sessionId].value,
    gameId = this[RoundTable.gameId].value,
    extId = this[RoundTable.extId],
    finished = this[RoundTable.finished],
    createdAt = this[RoundTable.createdAt],
    finishedAt = this[RoundTable.finishedAt]
)

fun ResultRow.toSpin(): Spin = Spin(
    id = this[SpinTable.id].value,
    roundId = this[SpinTable.roundId]?.value ?: throw IllegalStateException("Spin must have a round"),
    type = this[SpinTable.type],
    amount = (this[SpinTable.amount] ?: 0L).toBigInteger(),
    realAmount = (this[SpinTable.realAmount] ?: 0L).toBigInteger(),
    bonusAmount = (this[SpinTable.bonusAmount] ?: 0L).toBigInteger(),
    extId = this[SpinTable.extId],
    referenceId = this[SpinTable.referenceId]?.value,
    freeSpinId = this[SpinTable.freeSpinId]
)

/**
 * Map a row from joined tables to GameWithDetails.
 */
fun ResultRow.toGameWithDetails(): GameWithDetails = GameWithDetails(
    id = this[GameTable.id].value,
    identity = this[GameTable.identity],
    name = this[GameTable.name],
    images = this[GameTable.images],
    bonusBetEnable = this[GameTable.bonusBetEnable],
    bonusWageringEnable = this[GameTable.bonusWageringEnable],
    tags = this[GameTable.tags],
    symbol = this[GameVariantTable.symbol],
    freeSpinEnable = this[GameVariantTable.freeSpinEnable],
    freeChipEnable = this[GameVariantTable.freeChipEnable],
    jackpotEnable = this[GameVariantTable.jackpotEnable],
    demoEnable = this[GameVariantTable.demoEnable],
    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],
    locales = this[GameVariantTable.locales].map { Locale(it) },
    platforms = this[GameVariantTable.platforms].map { Platform.valueOf(it) },
    playLines = this[GameVariantTable.playLines],
    provider = this.toProvider(),
    aggregator = this.toAggregatorInfo()
)
