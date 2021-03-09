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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import java.util.concurrent.ConcurrentLinkedDeque


@Serializable
@SerialName("Einheit")
private class EinheitSurrogate(val spieler: SpielerTyp, val nummer: Int) {}

object EinheitSerializer : KSerializer<Einheit> {
    private val serializer = EinheitSurrogate.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Einheit) {
        encoder.encodeSerializableValue(serializer, EinheitSurrogate(value.spieler.spielerTyp, value.nummer))
    }

    override fun deserialize(decoder: Decoder): Einheit =
        decoder.decodeSerializableValue(serializer).let { spiel.spieler(it.spieler).einheit(it.nummer) }
}

@Serializable
data class MultiplayerKommandos(val data: List<MultiplayerKommando>)

@Serializable
sealed class MultiplayerKommando

@Serializable
class NeueEinheit(val x: Double, val y: Double, val einheitenTyp: String, val nummer: Int) : MultiplayerKommando()

@Serializable
class NeueKommandos(val einheit: Einheit, val kommandos: List<EinheitenKommando>) : MultiplayerKommando()

@Serializable
class NeueUpgrades(val einheitenTyp: EinheitenTyp) : MultiplayerKommando()

@Serializable
class NeueSpielerUpgrades(val spielerUpgrades: SpielerUpgrades) : MultiplayerKommando()

@Serializable
object ClientJoined : MultiplayerKommando()

class Multiplayer(private val client: Client?, val server: Server?) {

    val spielerTyp: SpielerTyp = when {
        client == null && server == null -> SpielerTyp.mensch
        server != null -> SpielerTyp.server
        else -> SpielerTyp.client
    }

    val gegnerTyp: SpielerTyp = when (spielerTyp) {
        SpielerTyp.mensch -> SpielerTyp.computer
        SpielerTyp.server -> SpielerTyp.client
        else -> SpielerTyp.server
    }

    fun empfangeneKommandosVerarbeiten(handler: (MultiplayerKommando) -> Unit) {
        fun empfange(l: ConcurrentLinkedDeque<MultiplayerKommando>) {
            val kommando = l.pollFirst() ?: return
            handler(kommando)
            empfange(l)
        }
        server?.empfangen?.let { empfange(it) }
        client?.empfangen?.let { empfange(it) }
    }

    fun sendeStartAnServer() {
        neuesKommando(ClientJoined)
    }

    fun neueEinheit(x: Double, y: Double, einheit: Einheit) {
        neuesKommando(NeueEinheit(x, y, einheit.typ.name, einheit.nummer))
    }

    fun neueKommandos(einheit: Einheit) {
        neuesKommando(NeueKommandos(einheit, einheit.kommandoQueue))
    }

    fun upgrade(einheitenTyp: EinheitenTyp) {
        neuesKommando(NeueUpgrades(einheitenTyp))
    }

    fun upgrade(spieler: Spieler) {
        neuesKommando(NeueSpielerUpgrades(spieler.upgrades))
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

