package com.nekzabirov.aggregators.adapter.onegamehub.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto<T>(
    val status: Int,

    val response: T? = null
) {
    val success: Boolean = status == 200
}
