package infrastructure.aggregator.onegamehub.model

import domain.aggregator.adapter.IAggregatorConfig

class OneGameHubConfig : IAggregatorConfig {
    var salt: String = ""
        private set

    var secret: String = ""
        private set

    var partner: String = ""
        private set

    var gateway: String = ""
        private set

    override fun parse(data: Map<String, String>) {
        salt = data["salt"] ?: ""
        secret = data["secret"] ?: ""
        partner = data["partner"] ?: ""
        gateway = data["gateway"] ?: ""
    }
}