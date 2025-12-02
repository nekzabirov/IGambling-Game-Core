package infrastructure.aggregator.pragmatic.handler

import application.port.outbound.WalletAdapter
import application.service.SessionService
import application.usecase.spin.PlaceSpinUsecase
import application.usecase.spin.SettleSpinUsecase
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
import infrastructure.aggregator.pragmatic.adapter.PragmaticCurrencyAdapter
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetDto
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticResponse
import shared.value.SessionToken

class PragmaticHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val placeSpinUsecase: PlaceSpinUsecase,
    private val settleSpinUsecase: SettleSpinUsecase,
    private val providerCurrencyAdapter: PragmaticCurrencyAdapter
) {
    suspend fun balance(token: SessionToken): PragmaticResponse {
        val session = sessionService.findByToken(token = token).getOrElse {
            return it.toErrorResponse
        }

        return returnSuccess(session)
    }

    suspend fun bet(token: SessionToken, payload: PragmaticBetDto): PragmaticResponse {
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

    suspend fun win(token: SessionToken, payload: PragmaticBetDto): PragmaticResponse {
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

        return returnSuccess(session)
    }

    private suspend fun returnSuccess(session: Session): PragmaticResponse {
        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse
        }

        return PragmaticResponse.Success(
            balance = providerCurrencyAdapter.convertSystemToProvider(balance.totalAmount, balance.currency).toLong(),
            currency = balance.currency.value
        )
    }

    private val Throwable.toErrorResponse: PragmaticResponse.Error
        get() = when (this) {
            is DomainError -> toErrorResponse
            else -> PragmaticResponse.Error.PragmaticInvalidRequest
        }

    private val DomainError.toErrorResponse: PragmaticResponse.Error
        get() = when (this) {
            is AggregatorError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is AggregatorNotSupportedError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is BetLimitExceededError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is DuplicateEntityError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is ExternalServiceError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is GameUnavailableError -> PragmaticResponse.Error.PragmaticGameNotFound
            is IllegalStateError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is InsufficientBalanceError -> PragmaticResponse.Error.PragmaticInsufficientBalance
            is InvalidPresetError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is NotFoundError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is RoundFinishedError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is RoundNotFoundError -> PragmaticResponse.Error.PragmaticInvalidRequest
            is SessionInvalidError -> PragmaticResponse.Error.PragmaticTokenExpired
            is ValidationError -> PragmaticResponse.Error.PragmaticInvalidRequest
        }
}
