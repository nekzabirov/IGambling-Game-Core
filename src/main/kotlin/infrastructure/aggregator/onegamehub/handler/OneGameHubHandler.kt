package infrastructure.aggregator.onegamehub.handler

import application.port.outbound.WalletAdapter
import application.service.SessionService
import application.service.SpinService
import application.usecase.spin.PlaceSpinUsecase
import application.usecase.spin.SettleSpinUsecase
import com.nekgamebling.application.usecase.spin.RollbackUsecase
import com.nekgamebling.infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import com.nekgamebling.infrastructure.aggregator.onegamehub.handler.dto.OneGameHubBetDto
import domain.common.error.AggregatorError
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.BetLimitExceededError
import domain.common.error.DomainError
import domain.common.error.DuplicateEntityError
import domain.common.error.ExternalServiceError
import domain.common.error.GameUnavailableError
import domain.common.error.IllegalStateError
import domain.common.error.InsufficientBalanceError
import domain.common.error.InvalidPresetError
import domain.common.error.NotFoundError
import domain.common.error.RoundFinishedError
import domain.common.error.RoundNotFoundError
import domain.common.error.SessionInvalidError
import domain.common.error.ValidationError
import domain.session.model.Session
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubResponse
import shared.value.SessionToken

class OneGameHubHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val placeSpinUsecase: PlaceSpinUsecase,
    private val settleSpinUsecase: SettleSpinUsecase,
    private val providerCurrencyAdapter: OneGameHubCurrencyAdapter,
    private val spinService: SpinService,
    private val rollbackUsecase: RollbackUsecase
) {
    suspend fun balance(token: SessionToken): OneGameHubResponse {
        val session = sessionService.findByToken(token = token).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun bet(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val session = sessionService.findByToken(token = token).getOrElse {
            return it.toErrorResponse
        }

        placeSpinUsecase(
            token = token,
            gameSymbol = payload.gameSymbol,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            amount = providerCurrencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        ).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun win(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val session = sessionService.findByToken(token = token).getOrElse {
            return it.toErrorResponse
        }

        settleSpinUsecase(
            token = token,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            winAmount = providerCurrencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        ).getOrElse {
            return it.toErrorResponse
        }

        if (payload.finishRound) {
            spinService.closeRound(session, payload.roundId).getOrElse {
                return it.toErrorResponse
            }
        }

        return returnSuccess(session)
    }

    suspend fun cancel(token: SessionToken, roundId: String, transactionId: String): OneGameHubResponse {
        val session = sessionService.findByToken(token = token).getOrElse {
            return it.toErrorResponse
        }

        rollbackUsecase(session, roundId, transactionId).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    private suspend fun returnSuccess(session: Session): OneGameHubResponse {
        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse
        }

        return OneGameHubResponse.Success(
            balance = providerCurrencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toInt(),
            currency = balance.currency.value
        )
    }

    private val Throwable.toErrorResponse: OneGameHubResponse.Error
        get() = when (this) {
            is DomainError -> toErrorResponse
            else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }

    private val DomainError.toErrorResponse: OneGameHubResponse.Error
        get() = when (this) {
            is BetLimitExceededError -> OneGameHubResponse.Error.EXCEED_WAGER_LIMIT
            is GameUnavailableError -> OneGameHubResponse.Error.UNAUTHORIZED
            is InsufficientBalanceError -> OneGameHubResponse.Error.INSUFFICIENT_FUNDS
            is InvalidPresetError -> OneGameHubResponse.Error.BONUS_BET_MAX_RESTRICTION
            is SessionInvalidError -> OneGameHubResponse.Error.SESSION_TIMEOUT
           else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }
}