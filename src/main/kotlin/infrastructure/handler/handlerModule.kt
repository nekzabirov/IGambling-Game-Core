package com.nekgamebling.infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import application.port.inbound.command.AddGameTagCommand
import application.port.inbound.command.RemoveGameTagCommand
import application.port.inbound.command.UpdateGameCommand
import application.port.inbound.command.UpdateGameImageCommand
import com.nekgamebling.application.port.inbound.game.query.FindAllGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameResponse
import com.nekgamebling.application.port.inbound.game.query.FindGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindGameResponse
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlQuery
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlResponse
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersQuery
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersResponse
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderQuery
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderResponse
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderCommand
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderImageCommand
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionGamesCommand
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsQuery
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionQuery
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionResponse
import com.nekgamebling.infrastructure.handler.provider.command.UpdateProviderCommandHandler
import com.nekgamebling.infrastructure.handler.provider.command.UpdateProviderImageCommandHandler
import com.nekgamebling.infrastructure.handler.collection.command.UpdateCollectionCommandHandler
import com.nekgamebling.infrastructure.handler.collection.command.UpdateCollectionGamesCommandHandler
import com.nekgamebling.application.port.inbound.game.command.PlayGameCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameResponse
import com.nekgamebling.infrastructure.handler.game.command.PlayGameCommandHandler
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import com.nekgamebling.infrastructure.handler.spin.query.FindAllRoundQueryHandler
import com.nekgamebling.infrastructure.handler.spin.query.FindRoundQueryHandler
import com.nekgamebling.infrastructure.handler.collection.query.FindAllCollectionsQueryHandler
import com.nekgamebling.infrastructure.handler.collection.query.FindCollectionQueryHandler
import com.nekgamebling.infrastructure.handler.game.query.FindAllGameQueryHandler
import com.nekgamebling.infrastructure.handler.provider.query.FindAllProvidersQueryHandler
import com.nekgamebling.infrastructure.handler.provider.query.FindaProviderQueryHandler
import com.nekgamebling.infrastructure.handler.game.query.FindGameQueryHandler
import infrastructure.handler.AddGameTagCommandHandler
import infrastructure.handler.GameDemoUrlQueryHandler
import infrastructure.handler.RemoveGameTagCommandHandler
import infrastructure.handler.UpdateGameCommandHandler
import infrastructure.handler.UpdateGameImageCommandHandler
import org.koin.dsl.module

/**
 * Koin module for query and command handlers.
 * Handlers provide direct database access for read/write operations.
 */
val handlerModule = module {
    // ==========================================
    // Query Handlers
    // ==========================================
    single<QueryHandler<FindGameQuery, FindGameResponse>> { FindGameQueryHandler() }
    single<QueryHandler<FindAllGameQuery, FindAllGameResponse>> { FindAllGameQueryHandler() }
    single<QueryHandler<GameDemoUrlQuery, GameDemoUrlResponse>> { GameDemoUrlQueryHandler(get()) }
    single<QueryHandler<FindaProviderQuery, FindaProviderResponse>> { FindaProviderQueryHandler() }
    single<QueryHandler<FindAllProvidersQuery, FindAllProvidersResponse>> { FindAllProvidersQueryHandler() }
    single<QueryHandler<FindCollectionQuery, FindCollectionResponse>> { FindCollectionQueryHandler() }
    single<QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse>> { FindAllCollectionsQueryHandler() }
    single<QueryHandler<FindRoundQuery, FindRoundQueryResult>> { FindRoundQueryHandler() }
    single<QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult>> { FindAllRoundQueryHandler() }

    // ==========================================
    // Command Handlers
    // ==========================================
    single<CommandHandler<UpdateGameCommand, Unit>> { UpdateGameCommandHandler() }
    single<CommandHandler<UpdateGameImageCommand, Unit>> { UpdateGameImageCommandHandler(get()) }
    single<CommandHandler<AddGameTagCommand, Unit>> { AddGameTagCommandHandler() }
    single<CommandHandler<RemoveGameTagCommand, Unit>> { RemoveGameTagCommandHandler() }
    single<CommandHandler<UpdateProviderCommand, Unit>> { UpdateProviderCommandHandler() }
    single<CommandHandler<UpdateProviderImageCommand, Unit>> { UpdateProviderImageCommandHandler(get()) }
    single<CommandHandler<UpdateCollectionCommand, Unit>> { UpdateCollectionCommandHandler() }
    single<CommandHandler<UpdateCollectionGamesCommand, Unit>> { UpdateCollectionGamesCommandHandler() }
    single<CommandHandler<PlayGameCommand, PlayGameResponse>> { PlayGameCommandHandler(get()) }
}
