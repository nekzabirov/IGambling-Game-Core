package infrastructure.api.grpc

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import application.port.inbound.command.AddGameTagCommand
import application.port.inbound.command.RemoveGameTagCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameResponse
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
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import infrastructure.api.grpc.service.CollectionGrpcService
import infrastructure.api.grpc.service.RoundGrpcService
import infrastructure.api.grpc.service.GameGrpcService
import infrastructure.api.grpc.service.ProviderGrpcService
import org.koin.dsl.module

/**
 * Koin module for gRPC services.
 */
val grpcModule = module {
    // ==========================================
    // gRPC Services
    // ==========================================
    single {
        GameGrpcService(
            findGameQueryHandler = get<QueryHandler<FindGameQuery, FindGameResponse>>(),
            findAllGameQueryHandler = get<QueryHandler<FindAllGameQuery, FindAllGameResponse>>(),
            gameDemoUrlQueryHandler = get<QueryHandler<GameDemoUrlQuery, GameDemoUrlResponse>>(),
            playGameCommandHandler = get<CommandHandler<PlayGameCommand, PlayGameResponse>>(),
            updateGameCommandHandler = get<CommandHandler<UpdateGameCommand, Unit>>(),
            updateGameImageCommandHandler = get<CommandHandler<UpdateGameImageCommand, Unit>>(),
            addGameTagCommandHandler = get<CommandHandler<AddGameTagCommand, Unit>>(),
            removeGameTagCommandHandler = get<CommandHandler<RemoveGameTagCommand, Unit>>()
        )
    }

    single {
        ProviderGrpcService(
            findProviderQueryHandler = get<QueryHandler<FindaProviderQuery, FindaProviderResponse>>(),
            findAllProvidersQueryHandler = get<QueryHandler<FindAllProvidersQuery, FindAllProvidersResponse>>(),
            updateProviderCommandHandler = get<CommandHandler<UpdateProviderCommand, Unit>>(),
            updateProviderImageCommandHandler = get<CommandHandler<UpdateProviderImageCommand, Unit>>()
        )
    }

    single {
        CollectionGrpcService(
            findCollectionQueryHandler = get<QueryHandler<FindCollectionQuery, FindCollectionResponse>>(),
            findAllCollectionsQueryHandler = get<QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse>>(),
            updateCollectionCommandHandler = get<CommandHandler<UpdateCollectionCommand, Unit>>(),
            updateCollectionGamesCommandHandler = get<CommandHandler<UpdateCollectionGamesCommand, Unit>>()
        )
    }

    single {
        RoundGrpcService(
            findRoundQueryHandler = get<QueryHandler<FindRoundQuery, FindRoundQueryResult>>(),
            findAllRoundQueryHandler = get<QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult>>()
        )
    }
}

