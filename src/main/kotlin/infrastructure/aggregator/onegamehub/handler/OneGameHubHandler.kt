package infrastructure.aggregator.onegamehub.handler

import application.port.outbound.WalletAdapter
import application.saga.spin.PlaceSpinContext
import application.saga.spin.PlaceSpinSaga
import application.saga.spin.SettleSpinContext
import application.saga.spin.SettleSpinSaga
import application.service.SessionService
import application.usecase.spin.EndSpinUsecase
import application.usecase.spin.RollbackUsecase
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubBetDto
import domain.common.error.BetLimitExceededError
import domain.common.error.DomainError
import domain.common.error.GameUnavailableError
import domain.common.error.InsufficientBalanceError
import domain.common.error.InvalidPresetError
import domain.common.error.SessionInvalidError
import domain.session.model.Session
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubResponse
import shared.value.SessionToken

class OneGameHubHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val currencyAdapter: OneGameHubCurrencyAdapter,
    private val placeSpinSaga: PlaceSpinSaga,
    private val settleSpinSaga: SettleSpinSaga,
    private val endSpinUsecase: EndSpinUsecase,
    private val rollbackUsecase: RollbackUsecase
) {
    suspend fun balance(token: SessionToken): OneGameHubResponse {
        val session = sessionService.findByToken(token).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun bet(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val session = sessionService.findByToken(token).getOrElse {
            return it.toErrorResponse
        }

        val context = PlaceSpinContext(
            session = session,
            gameSymbol = payload.gameSymbol,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            amount = currencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        )

        placeSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun win(token: SessionToken, payload: OneGameHubBetDto): OneGameHubResponse {
        val session = sessionService.findByToken(token).getOrElse {
            return it.toErrorResponse
        }

        val context = SettleSpinContext(
            session = session,
            extRoundId = payload.roundId,
            transactionId = payload.transactionId,
            freeSpinId = payload.freeSpinId,
            winAmount = currencyAdapter.convertProviderToSystem(payload.amount, session.currency)
        )

        settleSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse
        }

        if (payload.finishRound) {
            endSpinUsecase(
                session = session,
                extRoundId = payload.roundId,
                freeSpinId = payload.freeSpinId
            ).getOrElse {
                return it.toErrorResponse
            }
        }

        return returnSuccess(session)
    }

    suspend fun cancel(token: SessionToken, roundId: String, transactionId: String): OneGameHubResponse {
        val session = sessionService.findByToken(token).getOrElse {
            return it.toErrorResponse
        }

        rollbackUsecase(
            session = session,
            extRoundId = roundId,
            transactionId = transactionId
        ).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    private suspend fun returnSuccess(session: Session): OneGameHubResponse {
        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse
        }

        return OneGameHubResponse.Success(
            balance = currencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toInt(),
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
