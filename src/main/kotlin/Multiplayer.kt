@file:Suppress("SpellCheckingInspection")

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.put
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentLinkedDeque

@Serializable
data class MultiplayerKommandos(val data: List<MultiplayerKommando>)

@Serializable
sealed class MultiplayerKommando() {
}

@Serializable
class NeueEinheit(val x: Double, val y: Double, val einheitenTyp: Int) : MultiplayerKommando()

class Multiplayer(val client: Client?, val server: Server?) {

    val multiplayer: Boolean = client != null || server != null

    fun neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) {
        neuesKommando(NeueEinheit(x, y, 0))
    }

    private fun neuesKommando(kommando: MultiplayerKommando) {
        server?.senden?.add(kommando)
        client?.senden?.add(kommando)
    }
}

suspend fun sendenUndEmpfangen(
    sendeKommands: ConcurrentLinkedDeque<MultiplayerKommando>,
    empfangenKommands: ConcurrentLinkedDeque<MultiplayerKommando>,
    senden: suspend (MultiplayerKommandos) -> MultiplayerKommandos
) {
    val kopie = sendeKommands.toList()
    val empfangen = senden(MultiplayerKommandos(kopie))

    println("gesendet: $kopie empfangen: $empfangen")

    sendeKommands.removeAll(kopie)
    empfangenKommands.addAll(empfangen.data)
}

const val endpoint = "/kommandos"

class Server(
    var senden: ConcurrentLinkedDeque<MultiplayerKommando> = ConcurrentLinkedDeque(),
    var empfangen: ConcurrentLinkedDeque<MultiplayerKommando> = ConcurrentLinkedDeque()
) {

    init {
        Thread {
            server()
        }.start()
    }

    private fun server() {
        println("server gestartet")
        val server = embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                put(endpoint) {
                    sendenUndEmpfangen(senden, empfangen) {
                        val e = call.receive<MultiplayerKommandos>()
                        call.respond(it)
                        e
                    }
                }
            }
        }
        server.start(wait = true)
    }
}

class Client(
    val server: String,
    var senden: ConcurrentLinkedDeque<MultiplayerKommando> = ConcurrentLinkedDeque(),
    var empfangen: ConcurrentLinkedDeque<MultiplayerKommando> = ConcurrentLinkedDeque()
) {

    init {
        Thread {
            while (true) {
                Thread.sleep(1000)
                runBlocking {
                    sendenUndEmpfangen(senden, empfangen) {
                        sende(it)
                    }
                }
            }
        }.start()
    }

    suspend fun sende(kommandos: MultiplayerKommandos): MultiplayerKommandos {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        return client.use {
            it.put(this.server + endpoint) {
                contentType(ContentType.Application.Json)
                body = kommandos
            }
        }
    }
}

