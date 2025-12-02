package infrastructure.aggregator.pragmatic.model

internal class PragmaticConfig(private val config: Map<String, String>) {
    val secretKey = config["secretKey"] ?: ""

    val secureLogin = config["secureLogin"] ?: ""

    val gateWayUrl = config["gatewayUrl"] ?: ""
}
