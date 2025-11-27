package infrastructure.aggregator.onegamehub.hook.error

class OneGameHubInvalidateRequest : OneGameHubError() {
    override val code: String = "ERR001"

    override val display: Boolean = false

    override val action: String = "restart"

    override val message: String = "Something wrong with request."

    override val description: String = "Invalid request. Please try again."
}