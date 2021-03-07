import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.put
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


@Serializable
sealed class MultiplayerKommando() {
    class NeueEinheit(val x: Double, val y: Double, val einheitenTyp: EinheitenTyp) : MultiplayerKommando()
}

class Multiplayer(val client: Client?, val server: Server?) {

    val multiplayer: Boolean = client != null || server != null

    fun neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) {
        neuesKommando(MultiplayerKommando.NeueEinheit(x, y, einheitenTyp))
    }

    private fun neuesKommando(kommando: MultiplayerKommando) {
        if (server != null) {
            server.senden.add(kommando)
        }
    }
}

suspend fun sendenUndEmpfangen(
    sendeKommands: MutableList<MultiplayerKommando>,
    empfangenKommands: MutableList<MultiplayerKommando>,
    senden: suspend (List<MultiplayerKommando>) -> List<MultiplayerKommando>) {
    val kopie = sendeKommands.toList()
    val empfangen = senden(kopie)
    sendeKommands.removeAll(kopie)
    empfangenKommands.addAll(empfangen)
}

class Server(
    var senden: MutableList<MultiplayerKommando> = mutableListOf(),
    var empfangen: MutableList<MultiplayerKommando> = mutableListOf()) {

    init {
        Thread {
            server()
        }.start()
    }

    private fun server() {
        println("server gestartet")
        embeddedServer(Netty, port = 8000) {
            routing {
                put("/kommandos") {
                    sendenUndEmpfangen(senden, empfangen) {
                        val e = call.receive<List<MultiplayerKommando>>()
                        call.respond(it)
                        e
                    }
                }
            }
        }.start(wait = true)
    }
}

class Client(
    val server: String,
    var senden: MutableList<MultiplayerKommando> = mutableListOf(),
    var empfangen: MutableList<MultiplayerKommando> = mutableListOf()) {

    init {
        Thread {
            Thread.sleep(1000)
            runBlocking {
                sendenUndEmpfangen(senden, empfangen) {
                    sende(it)
                }
            }
        }.start()
    }

    suspend fun sende(kommands: List<MultiplayerKommando>): List<MultiplayerKommando> {
        val client = HttpClient(CIO) {

//            install(JsonFeature) {
//                            serializer = GsonSerializer {
//                                // .GsonBuilder
//                                serializeNulls()
//                                disableHtmlEscaping()
//                            }
//                        }
        }
        return client.use {
            it.put(this.server) {
                body = kommands
            }
        }
    }
}

