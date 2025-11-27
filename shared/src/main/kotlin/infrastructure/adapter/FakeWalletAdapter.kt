package infrastructure.adapter

import app.adapter.WalletAdapter
import core.model.Balance
import core.value.Currency

class FakeWalletAdapter : WalletAdapter {
    companion object {
        var fakeBalance: Balance = Balance(
            real = 1000,
            bonus = 100,
            currency = Currency("EUR")
        )
    }

    override suspend fun findBalance(playerId: String): Result<Balance> {
        return Result.success(fakeBalance)
    }

    override suspend fun withdraw(
        playerId: String,
        referenceId: String,
        currency: Currency,
        real: Int,
        bonus: Int
    ): Result<Unit> {
        fakeBalance = fakeBalance.copy(real = fakeBalance.real - real, bonus = fakeBalance.bonus - bonus)
        return Result.success(Unit)
    }

    override suspend fun deposit(
        playerId: String,
        referenceId: String,
        currency: Currency,
        real: Int,
        bonus: Int
    ): Result<Unit> {
        fakeBalance = fakeBalance.copy(real = fakeBalance.real + real, bonus = fakeBalance.bonus + bonus)
        return Result.success(Unit)
    }

    override suspend fun rollback(playerId: String, referenceId: String): Result<Unit> {
        return Result.success(Unit)
    }
}