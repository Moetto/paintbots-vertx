package paintbots.app

import com.sksamuel.hoplite.ConfigLoader
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.multik.ndarray.data.get
import paintbots.models.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.random.Random

sealed class PaintBotsException : Exception()

class InvalidParameterException : PaintBotsException()
class MissingParameterException : PaintBotsException()

data class Settings(
    val width: Int = 100,
    val height: Int = 100,
    val port: Int = 31173,
    val imagesFolder: String = "/tmp/paintbots/${UUID.randomUUID()}/",
    val botDelayMs: Long = 100,
    val updateTickMs: Long = 100,
)

data class Bot(
    val name: String,
    val id: String,
    val x: Int,
    val y: Int,
    val color: Char = Random.nextInt(0, 16).toString(16).first(),
)

val settings = ConfigLoader().loadConfigOrThrow<Settings>("config.properties")
private val vertx: Vertx = Vertx.vertx()

fun main() {
    swapState(noResSwap(::resetCanvas))
    vertx.deployVerticle(PaintBots())

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Making video before shutting down")
            makeVideo(File(settings.imagesFolder), "video.mp4")
            println("Video done. It's at ${settings.imagesFolder}video.mp4")
            println("Bye")
        }
    })
}

class PaintBots : CoroutineVerticle() {
    override suspend fun start() {
        val options = HttpServerOptions().setPort(settings.port)
        val publicState = MutableStateFlow(toPublicState(currentState.value))
        val canvasOnlyFlow = MutableStateFlow(publicState.value.canvasPng)

        launch {
            publicState.collect {
                canvasOnlyFlow.value = it.canvasPng
            }
        }

        launch {
            updatePublicState(currentState, publicState, settings.updateTickMs)
        }

        launch {
            saveStateToFile(settings.imagesFolder, canvasOnlyFlow)
        }

        vertx.createHttpServer(options)
            .requestHandler(router(this, vertx))
            .webSocketHandler { ws ->
                ws.textMessageHandler(::wsTextMessageHandler)

                sendState(publicState.value, ws)

                val sendJob = launch {
                    publicState.collect {
                        sendState(it, ws)
                    }
                }

                ws.closeHandler {
                    sendJob.cancel()
                }
            }
            .listen().onSuccess {
                println("Server ready. When I'm shut down, a video can be found at ${settings.imagesFolder}video.mp4")
                println(settings)
            }
    }
}

fun toPublicState(state: State): GameState {
    val os = ByteArrayOutputStream()
    val (w, h) = state.canvas.shape

    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).apply {
        for (i in 0 until w) {
            for (j in 0 until h) {
                val color = state.canvas[i, j]
                setRGB(i, j, color)
            }
        }
    }
    ImageIO.write(image, "png", os)
    val ps = GameState(state.bots.map { it.value }.map { PublicBot(it.name, it.x, it.y, colors[it.color]!!) },
        EqualByteArray(os.toByteArray()),
        CanvasSettings(settings.width, settings.height)
    )
    os.close()
    return ps
}

suspend fun updatePublicState(
    currentState: StateFlow<State>,
    gameState: MutableStateFlow<GameState>,
    delay: Long,
) {
    currentState.collect {
        gameState.emit(toPublicState(it))
        delay(delay)
    }
}

suspend fun saveStateToFile(imagesFolder: String, canvasStateFlow: StateFlow<EqualByteArray>) {
    File(imagesFolder).mkdirs()

    var counter = 0
    canvasStateFlow.collect { canvas ->
        File("${settings.imagesFolder}/${counter.toString().padStart(10, '0')}.png").outputStream().use {
            it.write(canvas.content)
        }
        counter += 1
    }
}
