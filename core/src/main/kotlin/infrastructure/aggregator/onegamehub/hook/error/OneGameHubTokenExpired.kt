package infrastructure.aggregator.onegamehub.hook.error

class OneGameHubTokenExpired : OneGameHubError() {
    override val code: String = "ERR005"

    override val display: Boolean = true

    override val action: String = "restart"

    override val message: String = "Session expired, please login again."

    override val description: String = "Session expired or not exits"
}