package infrastructure.aggregator.onegamehub.hook.error

class OneGameHubInsufficientBalance : OneGameHubError() {
    override val code: String = "ERR003"

    override val display: Boolean = true

    override val action: String = "continue"

    override val message: String = "Insufficient funds to place current wager. Please reduce the stake or add more funds to your balance."

    override val description: String = "Should be used then bet cant be accepted because of insufficient funds in player balance"
}