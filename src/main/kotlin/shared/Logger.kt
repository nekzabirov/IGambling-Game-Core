package shared

import org.slf4j.LoggerFactory

internal object Logger {
    private val logger = LoggerFactory.getLogger("App")!!

    fun info(msg: String) = logger.info(msg)

    fun info(format: String, vararg arguments: Any) = logger.info(format, *arguments)
}