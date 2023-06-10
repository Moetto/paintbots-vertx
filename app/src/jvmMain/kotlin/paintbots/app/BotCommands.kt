package paintbots.app

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.vertx.core.shareddata.Lock
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.multik.ndarray.data.MutableMultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.set
import paintbots.models.colors
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min

typealias BotCommandReturn = Either<PaintBotsException, Pair<State, Bot>>

class NotRegisteredException : PaintBotsException()

class AlreadyRegisteredException : PaintBotsException()

val botColors = colors.map { it.key to Color.decode(it.value) }.toMap()

fun deregisterBot(bot: Bot, paramValue: String, state: State): Either<PaintBotsException, Pair<State, Bot>> {
    return Pair(state.copy(bots = state.bots - bot.id), bot).right()
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

fun moveBot(bot: Bot, paramValue: String, state: State): Either<PaintBotsException, Pair<State, Bot>> {

    val direction =
        Direction.values().firstOrNull { it.name == paramValue } ?: return InvalidParameterException().left()
    val newBot = when (direction) {
        Direction.LEFT -> bot.copy(x = max(bot.x - 1, 0))
        Direction.RIGHT -> bot.copy(x = min(bot.x + 1, settings.width - 1))
        Direction.UP -> bot.copy(y = max(bot.y - 1, 0))
        Direction.DOWN -> bot.copy(y = min(bot.y + 1, settings.height - 1))
    }
    return Pair(state.copy(bots = state.bots - bot.id + (newBot.id to newBot)), newBot).right()
}

fun noop(bot: Bot, paramValue: String, state: State): Either<PaintBotsException, Pair<State, Bot>> {
    return Pair(state, bot).right()
}

fun setBotColor(bot: Bot, paramValue: String, state: State): BotCommandReturn {
    val color = paramValue[0]
    return if (botColors.keys.contains(color)) {
        val newBot = bot.copy(color = paramValue[0])
        Pair(state.copy(bots = state.bots - bot.id + (bot.id to newBot)), bot).right()
    } else {
        InvalidParameterException().left()
    }
}

fun clear(bot: Bot, paramValue: String, state: State): BotCommandReturn {
    val newCanvas = state.canvas.copy() as MutableMultiArray
    newCanvas[bot.x, bot.y] = Color.pink.rgb
    val newState = state.copy(canvas = newCanvas)
    return Pair(newState, bot).right()
}

fun register(ctx: RoutingContext): Either<PaintBotsException, Pair<State, Bot>> {
    return getFormAttribute(ctx, "register").map { name ->
        return swapState { state ->
            if (state.bots.values.any { name == it.name }) {
                println("Already registered $name")
                Pair(state, AlreadyRegisteredException().left())
            } else {
                val id = UUID.randomUUID().toString()
                val bot = Bot(
                    name,
                    id,
                    (0 until settings.width).random(),
                    (0 until settings.height).random(),
                )
                println("New bot $bot")
                val newState = state.copy(bots = state.bots + (id to bot))
                Pair(newState, Pair(newState, bot).right())
            }
        }
    }
}

fun setPixel(bot: Bot, paramValue: String, state: State): Either<PaintBotsException, Pair<State, Bot>> {
    val newState = swapPixel(state, bot.x, bot.y, botColors[bot.color]!!.rgb)
    return Pair(newState, bot).right()
}

fun botCommand(
    scope: CoroutineScope,
    ctx: RoutingContext,
    paramValue: String,
    func: ((Bot, String, State) -> Either<PaintBotsException, Pair<State, Bot>>),
) {
    scope.launch {
        val (id, lock) = getId(ctx).fold({
            throw MissingParameterException()
        }, {
            Pair(it, ctx.vertx().sharedData().getLock(it).await())
        })
        lock.use {
            val bot: Bot = currentState.value.bots[id] ?: throw NotRegisteredException()
            swapState { currentState ->
                func.invoke(bot, paramValue, currentState).fold({ e ->
                    println("Error in command $func for $bot")
                    Pair(currentState, e.left())
                }, { (state, bot) ->
                    Pair(state, bot.right())
                })
            }.map { newBot ->
                delay(settings.botDelayMs)
                ctx.response().setStatusCode(200).end("x=${newBot.x}&y=${newBot.y}&color=${newBot.color}")
            }.onLeft {
                ctx.response().setStatusCode(400).end()
            }
        }
    }
}

fun removeBot(bot: Bot, paramValue: String, state: State): Either<PaintBotsException, Pair<State, Bot>> =
    Pair(state.copy(bots = state.bots - bot.id), bot).right()

val commands = mapOf<String, ((Bot, String, State) -> BotCommandReturn)>(
    "paint" to ::setPixel,
    "bye" to ::removeBot,
    "move" to ::moveBot,
    "info" to ::noop,
    "color" to ::setBotColor,
    "clear" to ::clear,
    "bye" to ::deregisterBot,
)

private inline fun <T> Lock.use(block: () -> T): T {
    try {
        return block()
    } catch (e: Exception) {
        throw e
    } finally {
        this.release()
    }
}
