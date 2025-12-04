package com.nekgamebling.application.usecase.spin

import application.service.GameService
import application.service.SessionService
import application.service.SpinCommand
import application.service.SpinService
import domain.session.model.Session
import shared.value.SessionToken

class RollbackUsecase(
    private val sessionService: SessionService,
    private val gameService: GameService,
    private val spinService: SpinService
) {
    suspend operator fun invoke(
        session: Session,
        extRoundId: String,
        transactionId: String,
    ): Result<Unit> {
        // Create spin command
        val command = SpinCommand(
            extRoundId = extRoundId,
            transactionId = transactionId,
            amount = 0.toBigInteger()
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