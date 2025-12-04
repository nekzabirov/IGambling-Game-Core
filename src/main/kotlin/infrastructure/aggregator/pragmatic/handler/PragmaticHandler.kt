package com.nekgamebling.infrastructure.aggregator.pragmatic.handler

import application.port.outbound.WalletAdapter
import application.service.GameService
import application.service.SessionService
import application.service.SpinService
import application.usecase.spin.PlaceSpinUsecase
import application.usecase.spin.SettleSpinUsecase
import com.nekgamebling.application.usecase.spin.RollbackUsecase
import com.nekgamebling.infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetPayload
import com.nekgamebling.infrastructure.aggregator.pragmatic.handler.dto.PragmaticResponse
import infrastructure.aggregator.pragmatic.adapter.PragmaticCurrencyAdapter
import shared.value.SessionToken
import kotlin.math.abs

class PragmaticHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val providerCurrencyAdapter: PragmaticCurrencyAdapter,
    private val placeSpinUsecase: PlaceSpinUsecase,
    private val settleSpinUsecase: SettleSpinUsecase,
    private val spinService: SpinService,
    private val gameService: GameService,
    private val rollbackUsecase: RollbackUsecase
) {

    suspend fun authenticate(sessionToken: SessionToken): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        val cash = providerCurrencyAdapter.convertSystemToProvider(balance.real, balance.currency)
        val bonus = providerCurrencyAdapter.convertSystemToProvider(balance.bonus, balance.currency)

        return PragmaticResponse.Success(
            cash = cash.toString(),
            bonus = bonus.toString(),
            currency = balance.currency.value,

            userId = session.playerId
        )
    }

    suspend fun balance(sessionToken: SessionToken): PragmaticResponse = authenticate(sessionToken)

    suspend fun bet(sessionToken: SessionToken, payload: PragmaticBetPayload): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        val betAmount = providerCurrencyAdapter.convertProviderToSystem(payload.amount.toBigDecimal(), balance.currency)

        placeSpinUsecase(
            token = sessionToken,
            gameSymbol = payload.gameId,
            extRoundId = payload.roundId,
            transactionId = payload.reference,
            freeSpinId = payload.bonusCode,
            amount = betAmount
        ).getOrElse {
            return it.toErrorResponse()
        }

        val currentBalance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        val usedBonus = balance.bonus - currentBalance.bonus

        return PragmaticResponse.Success(
            cash = providerCurrencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency)
                .toString(),
            bonus = providerCurrencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency)
                .toString(),
            currency = currentBalance.currency.value,

            usedPromo = providerCurrencyAdapter.convertSystemToProvider(usedBonus, currentBalance.currency).toString(),
            transactionId = payload.reference
        )
    }

    suspend fun result(sessionToken: SessionToken, payload: PragmaticBetPayload): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val totalAmount = payload.amount.toBigDecimal() + payload.promoWinAmount.toBigDecimal()

        settleSpinUsecase(
            token = sessionToken,
            extRoundId = payload.roundId,
            transactionId = payload.reference,
            freeSpinId = payload.bonusCode,
            winAmount = providerCurrencyAdapter.convertProviderToSystem(totalAmount, session.currency)
        ).getOrElse {
            return it.toErrorResponse()
        }

        val currentBalance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        return PragmaticResponse.Success(
            cash = providerCurrencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency)
                .toString(),
            bonus = providerCurrencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency)
                .toString(),
            currency = currentBalance.currency.value,

            transactionId = payload.reference
        )
    }

    suspend fun endRound(sessionToken: SessionToken, roundId: String): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        spinService.closeRound(session, roundId).getOrElse {
            return it.toErrorResponse()
        }

        return balance(sessionToken)
    }

    suspend fun refund(sessionToken: SessionToken, roundId: String, transactionId: String): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        rollbackUsecase(sessionToken, roundId, transactionId).getOrElse {
            return it.toErrorResponse()
        }

        return balance(
            sessionToken = sessionToken
        )
    }

    suspend fun adjustment(
        sessionToken: SessionToken,
        roundId: String,
        reference: String,
        amount: String
    ): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val realAmount = amount.toBigDecimal().let {
            providerCurrencyAdapter.convertProviderToSystem(it, session.currency)
        }

        val game = gameService.findById(session.gameId).getOrElse {
            return it.toErrorResponse()
        }

        if (realAmount < 0.toBigInteger()) {
            val betAmount = realAmount.abs()

            placeSpinUsecase(
                token = sessionToken,
                gameSymbol = game.symbol,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                amount = betAmount
            ).getOrElse {
                return it.toErrorResponse()
            }
        } else {
            settleSpinUsecase(
                token = sessionToken,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                winAmount = realAmount
            ).getOrElse {
                return it.toErrorResponse()
            }
        }

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        return PragmaticResponse.Success(
            cash = providerCurrencyAdapter.convertSystemToProvider(balance.real, balance.currency).toString(),
            bonus = providerCurrencyAdapter.convertSystemToProvider(balance.bonus, balance.currency).toString(),
            currency = balance.currency.value,
        )
    }

    private fun Throwable.toErrorResponse(): PragmaticResponse {
        TODO("Not yet implemented")
    }

}