package ui

import Client
import Multiplayer
import Punkt
import Server
import Spiel
import Spieler
import SpielerTyp
import SpielerUpgrades
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import javafx.scene.paint.Color

object DesktopLauncher {

    class Game(val spiel: Spiel) : com.badlogic.gdx.Game() {
        override fun create() {
            setScreen(GameScreen(spiel))
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val multiplayer = leseMultiplayerModus(args)

        val gegnerTyp = multiplayer.gegnerTyp
        val gegner = Spieler(
            kristalle = 0.0,
            minen = 0,
            startpunkt = startPunkt(gegnerTyp),
            farbe = spielerFarbe(gegnerTyp),
            spielerTyp = gegnerTyp,
            upgrades = SpielerUpgrades(
                angriffspunkte = 20,
                verteidiegungspunkte = 10,
                schadensUpgrade = 0,
                panzerungsUprade = 0,
            )
        )

        val spielerTyp = multiplayer.spielerTyp
        val mensch = Spieler(
            kristalle = 0.0,
            minen = 0,
            startpunkt = startPunkt(spielerTyp),
            farbe = spielerFarbe(spielerTyp),
            spielerTyp = spielerTyp,
            upgrades = SpielerUpgrades(
                angriffspunkte = 20,
                verteidiegungspunkte = 10,
                schadensUpgrade = 0,
                panzerungsUprade = 0
            )
        )

        val spiel = Spiel(mensch, gegner, multiplayer = multiplayer)

        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 550
        config.title = "Starcraft III"
        LwjglApplication(Game(spiel), config)
    }

    private fun spielerFarbe(spielerTyp: SpielerTyp) =
        if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer) Color.RED else Color.BLUE

    private fun startPunkt(spielerTyp: SpielerTyp) =
        if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer)
            Punkt(x = 300.0, y = 115.0) else Punkt(x = 200.0, y = 105.0)


    private fun leseMultiplayerModus(args: Array<String>): Multiplayer {
        var server: Server? = null
        var client: Client? = null
        if (args.getOrNull(0) == "server") {
            server = Server()
        } else if (args.getOrNull(0) == "client") {
            client = Client(args[1])
        }
        return Multiplayer(client, server)
    }

}
