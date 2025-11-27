package core.model

data class Page<T>(
    val items: List<T>,

    val totalPages: Long,
) {
    inline fun <R> map(transform: (T) -> R) = Page(items.map(transform), totalPages)
}

data class Pageable(val page: Int, val size: Int) {
    internal val pageReal = (page).coerceAtLeast(1)
    internal val sizeReal = (size).coerceAtLeast(1)
    internal val offset = (pageReal - 1L) * size

    fun getTotalPage(totalItems: Long) = (totalItems / sizeReal).coerceAtLeast(1L)
}