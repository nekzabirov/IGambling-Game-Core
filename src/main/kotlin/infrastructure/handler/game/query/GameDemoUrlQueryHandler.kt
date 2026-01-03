package infrastructure.handler

import application.port.inbound.QueryHandler
import application.service.GameService
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlQuery
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlResponse

class GameDemoUrlQueryHandler(
    private val gameService: GameService
) : QueryHandler<GameDemoUrlQuery, GameDemoUrlResponse> {

    override suspend fun handle(query: GameDemoUrlQuery): Result<GameDemoUrlResponse> {
        return gameService.launchDemo(
            gameIdentity = query.identity,
            currency = query.currency,
            locale = query.locale,
            platform = query.platform,
            lobbyUrl = query.lobbyUrl
        ).map { result ->
            GameDemoUrlResponse(launchUrl = result.launchUrl)
        }
    }
}
