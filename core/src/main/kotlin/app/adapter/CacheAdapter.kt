package app.adapter

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

interface CacheAdapter {

    fun <T : Any> save(key: String, value: T, duration: Duration = 1.hours)

    fun <T : Any> get(key: String): T?

}