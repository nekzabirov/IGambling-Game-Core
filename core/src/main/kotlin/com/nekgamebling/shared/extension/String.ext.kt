package com.nekgamebling.shared.extension

fun String.toUrlSlug(): String {
    return this
        .lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9\\s-]"), "") // Remove special chars except spaces and hyphens
        .trim()
        .replace(Regex("\\s+"), "_") // Replace spaces with underscores
        .replace(Regex("_+"), "_") // Replace multiple underscores with single
}