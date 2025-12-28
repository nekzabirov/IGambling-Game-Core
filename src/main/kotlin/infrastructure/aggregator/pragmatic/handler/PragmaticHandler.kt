package infrastructure.aggregator.pragmatic.handler

import application.port.outbound.WalletAdapter
import application.saga.spin.end.EndSpinContext
import application.saga.spin.end.EndSpinSaga
import application.saga.spin.place.PlaceSpinContext
import application.saga.spin.place.PlaceSpinSaga
import application.saga.spin.rollback.RollbackSpinContext
import application.saga.spin.rollback.RollbackSpinSaga
import application.saga.spin.settle.SettleSpinContext
import application.saga.spin.settle.SettleSpinSaga
import application.service.GameService
import application.service.SessionService
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetPayload
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticResponse
import infrastructure.aggregator.shared.ProviderCurrencyAdapter
import shared.value.SessionToken

class PragmaticHandler(
    private val sessionService: SessionService,
    private val walletAdapter: WalletAdapter,
    private val currencyAdapter: ProviderCurrencyAdapter,
    private val placeSpinSaga: PlaceSpinSaga,
    private val settleSpinSaga: SettleSpinSaga,
    private val endSpinSaga: EndSpinSaga,
    private val rollbackSpinSaga: RollbackSpinSaga,
    private val gameService: GameService
) {

    suspend fun authenticate(sessionToken: SessionToken): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        val cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency)
        val bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency)

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

        val betAmount = currencyAdapter.convertProviderToSystem(payload.amount.toBigDecimal(), balance.currency)

        val context = PlaceSpinContext(
            session = session,
            gameSymbol = payload.gameId,
            extRoundId = payload.roundId,
            transactionId = payload.reference,
            freeSpinId = payload.bonusCode,
            amount = betAmount
        )

        placeSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse()
        }

        val currentBalance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        val usedBonus = balance.bonus - currentBalance.bonus

        return PragmaticResponse.Success(
            cash = currencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency).toString(),
            bonus = currencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency).toString(),
            currency = currentBalance.currency.value,
            usedPromo = currencyAdapter.convertSystemToProvider(usedBonus, currentBalance.currency).toString(),
            transactionId = payload.reference
        )
    }

    suspend fun result(sessionToken: SessionToken, payload: PragmaticBetPayload): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val totalAmount = payload.amount.toBigDecimal() + payload.promoWinAmount.toBigDecimal()

        val context = SettleSpinContext(
            session = session,
            extRoundId = payload.roundId,
            transactionId = payload.reference,
            freeSpinId = payload.bonusCode,
            winAmount = currencyAdapter.convertProviderToSystem(totalAmount, session.currency)
        )

        settleSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse()
        }

        val currentBalance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        return PragmaticResponse.Success(
            cash = currencyAdapter.convertSystemToProvider(currentBalance.real, currentBalance.currency).toString(),
            bonus = currencyAdapter.convertSystemToProvider(currentBalance.bonus, currentBalance.currency).toString(),
            currency = currentBalance.currency.value,
            transactionId = payload.reference
        )
    }

    suspend fun endRound(sessionToken: SessionToken, roundId: String): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val context = EndSpinContext(
            session = session,
            extRoundId = roundId,
            freeSpinId = null
        )

        endSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse()
        }

        return balance(sessionToken)
    }

    suspend fun refund(sessionToken: SessionToken, roundId: String, transactionId: String): PragmaticResponse {
        val session = sessionService.findByToken(sessionToken).getOrElse {
            return it.toErrorResponse()
        }

        val context = RollbackSpinContext(
            session = session,
            extRoundId = roundId,
            transactionId = transactionId
        )

        rollbackSpinSaga.execute(context).getOrElse {
            return it.toErrorResponse()
        }

        return balance(sessionToken)
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
            currencyAdapter.convertProviderToSystem(it, session.currency)
        }

        val game = gameService.findById(session.gameId).getOrElse {
            return it.toErrorResponse()
        }

        if (realAmount < java.math.BigInteger.ZERO) {
            val betAmount = realAmount.abs()

            val context = PlaceSpinContext(
                session = session,
                gameSymbol = game.symbol,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                amount = betAmount
            )

            placeSpinSaga.execute(context).getOrElse {
                return it.toErrorResponse()
            }
        } else {
            val context = SettleSpinContext(
                session = session,
                extRoundId = roundId,
                transactionId = reference,
                freeSpinId = null,
                winAmount = realAmount
            )

            settleSpinSaga.execute(context).getOrElse {
                return it.toErrorResponse()
            }
        }

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return it.toErrorResponse()
        }

        return PragmaticResponse.Success(
            cash = currencyAdapter.convertSystemToProvider(balance.real, balance.currency).toString(),
            bonus = currencyAdapter.convertSystemToProvider(balance.bonus, balance.currency).toString(),
            currency = balance.currency.value,
        )
    }

    private fun Throwable.toErrorResponse(): PragmaticResponse {
        TODO("Not yet implemented")
    }
}
