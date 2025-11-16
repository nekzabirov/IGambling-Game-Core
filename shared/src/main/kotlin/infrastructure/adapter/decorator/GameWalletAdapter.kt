package infrastructure.adapter.decorator

import app.adapter.WalletAdapter
import core.model.Balance
import domain.game.model.GameFull
import org.koin.core.component.KoinComponent

class GameWalletAdapter(private val game: GameFull) : WalletAdapter, KoinComponent {
    private val walletAdapter = getKoin().get<WalletAdapter>()

    override fun findBalance(playerId: String): Result<Balance> =
        walletAdapter.findBalance(playerId).map {
                if (!game.bonusBetEnable)
                    it.copy(bonusAmount = 0)
                else it
            }
}