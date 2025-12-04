package com.nekgamebling.application.usecase.spin

import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SessionService
import application.service.SpinCommand
import application.service.SpinService
import com.nekgamebling.application.service.AggregatorService
import shared.value.SessionToken

class RollbackUsecase(
    private val sessionService: SessionService,
    private val gameService: GameService,
    private val spinService: SpinService,
    private val aggregatorService: AggregatorService,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        token: SessionToken,
        extRoundId: String,
        transactionId: String,
        freeSpinId: String?
    ): Result<Unit> {
        // Find session
        val session = sessionService.findByToken(token).getOrElse {
            return Result.failure(it)
        }

        // Create spin command
        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = 0.toBigInteger(),
            freeSpinId = freeSpinId
        )

        // Place spin
        spinService.rollback(session, command).getOrElse {
            return Result.failure(it)
        }

        val game = gameService.findById(session.gameId).getOrElse {
            return Result.failure(it)
        }

        //TODO: Maybe do event

        return Result.success(Unit)
    }
}