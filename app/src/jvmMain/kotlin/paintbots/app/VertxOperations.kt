package paintbots.app

import arrow.core.Either
import arrow.core.left
import arrow.core.partially1
import arrow.core.right
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import paintbots.models.Command
import paintbots.models.GameState


fun wsTextMessageHandler(msg: String) {
    val command = try {
        Command.valueOf(msg)
    } catch (e: IllegalArgumentException) {
        println("Unknown command $msg")
    }
    when (command) {
        Command.RESET_CANVAS -> {
            println("Resetting canvas")
            swapState(noResSwap(::resetCanvas))
        }

        Command.KICK_ALL -> {
            println("Kicking all bots")
            swapState(noResSwap(::kickAll))
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun sendState(state: GameState, ws: ServerWebSocket) {
    ws.writeBinaryMessage(Buffer.buffer(Cbor.Default.encodeToByteArray(state)))
}

fun router(coroutineScope: CoroutineScope, vertx: Vertx): Router {
    val router = Router.router(vertx)
    router.post("/").handler(BodyHandler.create().setMergeFormAttributes(true))
        .handler(::routeByParam.partially1(coroutineScope))
    router.route("/*").handler(StaticHandler.create())
    return router
}

fun routeByParam(scope: CoroutineScope, ctx: RoutingContext) {
    ctx.request().formAttributes()["register"]?.let {
        register(ctx).fold({
            ctx.response().setStatusCode(400).end(it.toString())
        }, { (_, bot) ->
            ctx.response().setStatusCode(200).end(bot.id)
        })
    } ?: ctx.request().formAttributes().firstNotNullOfOrNull { formKv ->
        commands[formKv.key]?.let {
            return@let botCommand(scope, ctx, formKv.value, it)
        }
    } ?: ctx.response().setStatusCode(400).end("Unknown command ${ctx.request().formAttributes()}")
}

fun getFormAttribute(ctx: RoutingContext, attribute: String): Either<MissingParameterException, String> {
    return ctx.request().getFormAttribute(attribute)?.right() ?: MissingParameterException().left()
}

fun getId(ctx: RoutingContext): Either<MissingParameterException, String> {
    return getFormAttribute(ctx, "id")
}
