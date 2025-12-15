package infrastructure.aggregator.shared

import domain.common.error.AggregatorError

/**
 * Shared utility for validating freespin preset values against a preset schema.
 * Used by all aggregator freespin adapters to reduce code duplication.
 */
object FreespinPresetValidator {

    /**
     * Validates preset values against a preset schema and extracts validated values.
     *
     * @param presetValue User-provided preset values
     * @param presetSchema Schema defining the constraints (minimal, maximum, default)
     * @return Result containing a map of validated key-value pairs or an error
     */
    fun validate(
        presetValue: Map<String, Int>,
        presetSchema: Map<String, Any>
    ): Result<Map<String, Int>> {
        val validatedValues = mutableMapOf<String, Int>()

        for (entry in presetSchema) {
            val key = entry.key
            val constraints = entry.value as Map<*, *>

            val value = when {
                presetValue.containsKey(key) -> presetValue[key]!!
                constraints.containsKey("default") -> constraints["default"]!! as Int
                else -> return Result.failure(
                    AggregatorError("Missing required preset value: $key")
                )
            }

            val minimal = constraints["minimal"] as? Int
            val maximum = constraints["maximum"] as? Int

            if (minimal != null && value < minimal) {
                return Result.failure(
                    AggregatorError("$key value too small: $value < $minimal")
                )
            }

            if (maximum != null && value > maximum) {
                return Result.failure(
                    AggregatorError("$key value too large: $value > $maximum")
                )
            }

            validatedValues[key] = value
        }

        return Result.success(validatedValues)
    }
}
