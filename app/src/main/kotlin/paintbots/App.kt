/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package paintbots

import arrow.core.Either
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.util.UUID

class MissingParameterException : Exception()

data class Bot(
    val name: String,
    val id: UUID,
)

data class State(
    val bots: List<Bot>,
)

var state = State(listOf())

fun getFormAttribute(ctx: RoutingContext, attribute: String): Either<MissingParameterException, String> {
    return Either.Right(ctx.request().getFormAttribute(attribute)) ?: Either.Left(MissingParameterException())
}

fun getId(ctx: RoutingContext): Either<MissingParameterException, String> {
    return getFormAttribute(ctx, "id")
}

fun register(ctx: RoutingContext) {
    val wantedName = getFormAttribute(ctx, "register")
    val (responseCode, responseBody) = wantedName.fold(
        {
            Pair(400, "Parameter id required")
        },
        { name ->
            if (state.bots.any { name == it.name }) {
                Pair(409, "Bot with that name already registered")
            } else {
                val id = UUID.randomUUID()
                state = state.copy(bots = state.bots + Bot(name, id))
                Pair(200, id.toString())
            }
        },
    )
    ctx.response().setStatusCode(responseCode).end(responseBody)
}

fun paint(ctx: RoutingContext) {
    ctx.response().setStatusCode(200).end()
}

fun bye(ctx: RoutingContext) {
    ctx.response().setStatusCode(200).end()
}

val commands = mapOf(
    "register" to ::register,
    "paint" to ::paint,
    "bye" to ::bye,
)

fun routeByParam(ctx: RoutingContext) {
    ctx.request().formAttributes().firstNotNullOfOrNull { formKv ->
        commands[formKv.key]
    }?.invoke(ctx) ?: ctx.response().setStatusCode(400).end("Unknown command ${ctx.request().formAttributes()}")
}

fun router(vertx: Vertx): Router {
    val router = Router.router(vertx)
    router.post("/")
        .handler(BodyHandler.create().setMergeFormAttributes(true))
        .handler(::routeByParam)
    return router
}

fun main() {
    val vertx = Vertx.vertx()
    val options = HttpServerOptions().setPort(31173)
    vertx.createHttpServer(options)
        .requestHandler(router(vertx))
        .listen()
}
