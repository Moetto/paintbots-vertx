import dev.fritz2.core.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import paintbots.models.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalEncodingApi
suspend fun websocket(
    stateStore: Store<GameState>,
    commands: Flow<Command>,
) {
    val stateFlow = MutableSharedFlow<GameState>()
    stateFlow handledBy stateStore.update

    HttpClient {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 31173, path = "/") {
        commands.map { Frame.Text(it.name) }.handledBy(outgoing::send)
        try {
            while (this@webSocket.isActive) {
                val state = Cbor.Default.decodeFromByteArray<GameState>(incoming.receive().data)
                stateFlow.emit(state)
            }
        } catch (e: ClosedReceiveChannelException) {
            console.log("Channel closed")
        }
    }
}

data class Settings(
    val showBots: Boolean = true,
    val showNames: Boolean = false,
)

data class State(
    val gameState: GameState,
    val settings: Settings,
)

@OptIn(ExperimentalEncodingApi::class)
fun main() {
    val commands = MutableSharedFlow<Command>()
    val stateStore =
        storeOf(State(GameState(listOf(), EqualByteArray(ByteArray(0)), CanvasSettings(0, 0)), Settings()))

    val canvasLens = lensOf<State, String>(
        "b64canvas",
        { Base64.encode(it.gameState.canvasPng.content) },
        { _, _ -> throw NotImplementedError() })

    val settingsLens = lensOf<State, Settings>(
        "settings",
        { it.settings },
        { state, settings -> state.copy(settings = settings) })

    val stateLens = lensOf<State, GameState>(
        "state",
        { it.gameState },
        { state, publicState -> state.copy(gameState = publicState) })

    val settingsStore = stateStore.map(settingsLens)

    MainScope().launch {
        websocket(stateStore.map(stateLens), commands)
    }

    render {
        div("") {
            div("p-2 float-left square flex flex-col") {
                div("m-1 relative flex flex-1") {
                    img("z-0 container mx-auto absolute top-0 left-0") {
                        stateStore.map(canvasLens).data handledBy {
                            console.log("Setting image")
                            src("data:image/png;base64,${it}")
                        }
                        inlineStyle("image-rendering: pixelated;")
                    }
                    div("z-10 container flex flex-1") {
                        stateStore.data.render(this) { state ->
                            svg("flex flex-1") {
                                if (!state.settings.showBots) {
                                    return@svg
                                }
                                state.gameState.bots.forEach { bot ->
                                    val x = bot.x.toDouble() / state.gameState.settings.width * 100
                                    val y = bot.y.toDouble() / state.gameState.settings.width * 100

                                    if (state.settings.showNames) {
                                        text {
                                            attr("x", "${x + 1}%")
                                            attr("y", "${y + 1}%")
                                            attr("fill", "black")
                                            attr("font-family", "monospace")
                                            +bot.name
                                        }
                                    }

                                    circle {
                                        attr("cx", "${x + 0.5}%")
                                        attr("cy", "${y + 0.5}%")
                                        attr("r", "0.8%")
                                        attr("fill", bot.color)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        div("m-2 float-left") {
            button("float-left bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded") {
                +"Reset"
                clicks.map { Command.RESET_CANVAS }.handledBy(commands::emit)
            }
            button("float-left bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded") {
                +"Kick all"
                clicks.map { Command.KICK_ALL }.handledBy(commands::emit)
            }
            button("float-left bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded") {
                settingsStore.data.render {
                    if (it.showBots) {
                        +"Hide bots"
                    } else {
                        +"Show bots"
                    }
                }
                clicks handledBy settingsStore.handle {
                    it.copy(showBots = !it.showBots)
                }
            }
            button("float-left bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded") {
                settingsStore.data.render {
                    if (it.showNames) {
                        +"Hide names"
                    } else {
                        +"Show names"
                    }
                }
                clicks handledBy settingsStore.handle {
                    it.copy(showNames = !it.showNames)
                }
            }
        }
    }
}

fun RenderContext.circle(
    baseClass: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: SvgTag.() -> Unit,
): SvgTag = register(SvgTag("circle", id, baseClass, job = job, ScopeContext(this.scope).apply(scope).scope), content)

fun RenderContext.text(
    baseClass: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: SvgTag.() -> Unit,
): SvgTag = register(SvgTag("text", id, baseClass, job = job, ScopeContext(this.scope).apply(scope).scope), content)
