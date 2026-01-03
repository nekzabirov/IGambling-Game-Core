package application.port.inbound.command

import application.port.inbound.Command

data class UpdateGameImageCommand(
    val identity: String,
    val key: String,
    val file: ByteArray,
    val extension: String
) : Command<Unit> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UpdateGameImageCommand
        return identity == other.identity &&
                key == other.key &&
                file.contentEquals(other.file) &&
                extension == other.extension
    }

    override fun hashCode(): Int {
        var result = identity.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + file.contentHashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}
