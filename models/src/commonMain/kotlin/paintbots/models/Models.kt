package paintbots.models

import kotlinx.serialization.Serializable

val colors = mapOf(
    '0' to "#000000",
    '1' to "#1D2B53",
    '2' to "#7E2553",
    '3' to "#008751",
    '4' to "#AB5236",
    '5' to "#5F574F",
    '6' to "#C2C3C7",
    '7' to "#FFF1E8",
    '8' to "#FF004D",
    '9' to "#FFA300",
    'a' to "#FFEC27",
    'b' to "#00E436",
    'c' to "#29ADFF",
    'd' to "#83769C",
    'e' to "#FF77A8",
    'f' to "#FFCCAA",
)

@Serializable
data class PublicBot(
    val name: String,
    val x: Int,
    val y: Int,
    val color: String,
)

/**
 * Kotlin ByteArray equals doesn't check contents. This one works with flows check whether to emit events based on equals check.
 */
@Serializable
class EqualByteArray(
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EqualByteArray

        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        return content.contentHashCode()
    }

}

@Serializable
data class CanvasSettings(
    val width: Int,
    val height: Int,
)

@Serializable
data class GameState(
    val bots: List<PublicBot>,
    val canvasPng: EqualByteArray,
    val settings: CanvasSettings,
)

@Serializable
enum class Command {
    KICK_ALL,
    RESET_CANVAS,
}
