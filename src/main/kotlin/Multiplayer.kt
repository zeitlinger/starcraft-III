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
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import java.util.concurrent.ConcurrentLinkedDeque

@Serializable
data class MultiplayerKommandos(val data: List<MultiplayerKommando>)

@Serializable
sealed class MultiplayerKommando()

@Serializable
class NeueEinheit(val x: Double, val y: Double, val einheitenTyp: String, val nummer: Int) : MultiplayerKommando()

@Serializable
class NeueKommandos(val nummer: Int, val kommandos: List<EinheitenKommando>) : MultiplayerKommando()

class Multiplayer(private val client: Client?, val server: Server?) {

    val multiplayer: Boolean = client != null || server != null

    fun empfangeneKommandosVerarbeiten(handler: (MultiplayerKommando) -> Unit) {
        fun empfange(l: ConcurrentLinkedDeque<MultiplayerKommando>) {
            val kommando = l.pollFirst() ?: return
            handler(kommando)
            empfange(l)
        }
        server?.empfangen?.let { empfange(it) }
        client?.empfangen?.let { empfange(it) }
    }

    fun neueEinheit(x: Double, y: Double, einheit: Einheit) {
        neuesKommando(NeueEinheit(x, y, einheit.typ.name, einheit.nummer))
    }

    fun neueKommandos(einheit: Einheit) {
        neuesKommando(NeueKommandos(einheit.nummer, einheit.kommandoQueue))
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
        val server = embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json(Json {
                    serializersModule =
                        serializersModuleOf(MultiplayerKommandos::class, MultiplayerKommandos.serializer())
                })
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
                Thread.sleep(100)
                runBlocking {
                    sendenUndEmpfangen(senden, empfangen) {
                        sende(it)
                    }
                }
            }
        }.start()
    }

    private suspend fun sende(kommandos: MultiplayerKommandos): MultiplayerKommandos {
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

