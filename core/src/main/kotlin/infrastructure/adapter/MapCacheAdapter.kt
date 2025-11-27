package infrastructure.adapter

import app.adapter.CacheAdapter
import kotlin.time.Duration

class MapCacheAdapter : CacheAdapter {
    private val map = LinkedHashMap<String, Any>()

    override fun <T : Any> save(key: String, value: T, duration: Duration) {
        map[key] = value
    }

    override fun <T : Any> get(key: String): T? {
        if (!map.containsKey(key)) {
            return null
        }

        return map[key] as T?
    }
}