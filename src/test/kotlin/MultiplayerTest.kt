import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class MultiplayerTest : FreeSpec({

    "encode MultiplayerKommandos"  {
        val s = MultiplayerKommandos.serializer()
        val message = Json.encodeToString(s, MultiplayerKommandos(listOf(NeueEinheit(Punkt(1.0, 2.0), "foo", 1))))
        message shouldBe """{"data":[{"type":"NeueEinheit","x":1.0,"y":2.0,"einheitenTyp":"foo","nummer":1}]}"""
    }


})
