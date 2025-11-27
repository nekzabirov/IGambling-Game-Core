package app.service.spin

sealed class ISpinCommand(
    open val extRoundId: String,
    open val transactionId: String,
    open val amount: Int
) {
    class Builder {
        private var extRoundId: String = ""

        private var transactionId: String = ""

        private var amount: Int = 0

        private var freeSpinId: String? = null

        fun withExtRoundId(extRoundId: String) = apply { this.extRoundId = extRoundId }

        fun withTransactionId(transactionId: String) = apply { this.transactionId = transactionId }

        fun withAmount(amount: Int) = apply { this.amount = amount }

        fun withFreeSpinId(freeSpinId: String) = apply { this.freeSpinId = freeSpinId }

        fun build() = if (freeSpinId != null) {
            FreespinSpinCommand(
                referenceId = freeSpinId!!,
                extRoundId = extRoundId,
                transactionId = transactionId,
                amount = amount
            )
        } else {
            SpinCommand(
                extRoundId = extRoundId,
                transactionId = transactionId,
                amount = amount
            )
        }
    }
}

data class SpinCommand(
    override val extRoundId: String,
    override val transactionId: String,
    override val amount: Int
) : ISpinCommand(extRoundId, transactionId, amount)

data class FreespinSpinCommand(
    val referenceId: String,
    override val extRoundId: String,
    override val transactionId: String,
    override val amount: Int
) : ISpinCommand(extRoundId, transactionId, amount)
