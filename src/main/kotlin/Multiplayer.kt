import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.put
import io.ktor.response.respondText
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.Serializable

@Serializable
data class Command(val id: String)

class Server(var commands: List<Command> = emptyList()) {

    init {
        Thread {
            server()
        }.start()
    }

    private fun server() {
        embeddedServer(Netty, port = 8000) {
            routing {
                put("/commands") {
//                    val list = call.receive<List<Command>>()
                    call.respondText("angekommen")
                }
            }
        }.start(wait = true)
    }
}

class Client() {
    suspend fun sende(commands: List<Command>) {
        val client = HttpClient(CIO) {
//            install(JsonFeature) {
//                            serializer = GsonSerializer {
//                                // .GsonBuilder
//                                serializeNulls()
//                                disableHtmlEscaping()
//                            }
//                        }
        }
        client.use {
            it.put<List<Command>>("http://localhost:8080") {
                body = commands
            }
        }
    }
}

