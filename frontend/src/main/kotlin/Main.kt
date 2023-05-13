import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.w3c.dom.HTMLImageElement
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Websocket : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job

    private val client = HttpClient {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun start() = launch {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 31173, path = "/") {
            try {
                while (isActive) {
                    val img = incoming.receive()
                    console.log("Img received")
                    val canvas = document.getElementById("canvas") as HTMLImageElement
                    console.log("Canvas found")
                    canvas.src = "data:image/png;base64,${Base64.encode(img.readBytes())}"
                    console.log("Canvas source set")
                }
            } catch (e: ClosedReceiveChannelException) {
                console.log("Channel closed")
            }
        }
    }
}

fun main() {
    Websocket().start()
}
