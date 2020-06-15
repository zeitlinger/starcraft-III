@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis", "LiftReturnOrAssignment", "NonAsciiCharacters", "PropertyName", "EnumEntryName", "SpellCheckingInspection")

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlin.math.*


data class Gebäude(
        val name: String,
        val kristalle: Int
)

val kaserne = Gebäude(name = "Kaserne", kristalle = 1500)
val fabrik = Gebäude(name = "Fabrik", kristalle = 2000)
val raumhafen = Gebäude(name = "Raumhafen", kristalle = 2500)
val prodoktionsgebäude = listOf(
        kaserne,
        fabrik,
        raumhafen
)


data class TechGebäude(
        val name: String,
        val kristalle: Int
)

val schmiede = TechGebäude(name = "Schmiede", kristalle = 2000)
val fusionskern = TechGebäude(name = "Fusionskern", kristalle = 3000)
val akademie = TechGebäude(name = "Akademie", kristalle = 2500)
val reaktor = TechGebäude(name = "Reaktor", kristalle = 2000)
val techgebäude = listOf(
        schmiede,
        fusionskern,
        akademie,
        reaktor
)

enum class KannAngreifen {
    alles,
    boden,
    heilen
}

enum class LuftBoden {
    luft, boden
}

data class EinheitenTyp(
        val schaden: Double,
        val reichweite: Int,
        val leben: Double,
        val laufweite: Double,
        val kristalle: Int,
        val kuerzel: String,
        val panzerung: Double,
        val kannAngreifen: KannAngreifen,
        val luftBoden: LuftBoden = LuftBoden.boden,
        val gebäude: Gebäude?,
        val techGebäude: TechGebäude? = null,
        val name: String,
        val hotkey: String?
)

data class Einheit(
        val typ: EinheitenTyp,
        val reichweite: Int,
        var leben: Double,
        val schaden: Double,
        var x: Double,
        var y: Double,
        val laufweite: Double,
        val bild: Circle = Main.einheitenBild(),
        val kuerzel: Text,
        val lebenText: Text = Text(),
        var ziel: Einheit? = null,
        var zielPunkt: Punkt? = null,
        var zielpunktkreis: Arc? = null,
        var zielpunktLinie: Line? = null,
        var panzerung: Double,
        var auswahlkreis: Arc? = null
) {
    fun punkt() = Punkt(x = x, y = y)
}

data class Punkt(
        var x: Double,

        var y: Double
)


data class Spieler(
        val einheiten: MutableList<Einheit>,
        var kristalle: Double,
        var angriffspunkte: Int,
        var verteidiegungspunkte: Int,
        var minen: Int,
        var startpunkt: Punkt,
        val farbe: Color,
        val mensch: Boolean,
        val kristalleText: Text = Text(),
        val minenText: Text = Text(),
        var schadenUpgrade: Int,
        var panzerungUprade: Int
)


@Suppress("SpellCheckingInspection")
class Main : Application() {

    val infantrie = EinheitenTyp(name = "Infantrie", reichweite = 150, leben = 1000.0, schaden = 1.0, laufweite = 0.5, kristalle = 500, kuerzel = "INF", panzerung = 0.125,
            kannAngreifen = KannAngreifen.alles, gebäude = kaserne, hotkey = "q")
    val eliteinfantrie = EinheitenTyp(name = "Elite-Infantrie", reichweite = 250, leben = 1200.0, schaden = 1.8, laufweite = 0.8, kristalle = 1400, kuerzel = "ELI", panzerung = 0.1,
            kannAngreifen = KannAngreifen.alles, gebäude = kaserne, techGebäude = akademie, hotkey = "w")
    val berserker = EinheitenTyp(name = "Berserker", reichweite = 40, leben = 2000.0, schaden = 4.0, laufweite = 1.0, kristalle = 1000, kuerzel = "BER", panzerung = 0.25,
            kannAngreifen = KannAngreifen.boden, gebäude = kaserne, techGebäude = schmiede, hotkey = "e")
    val flammenwerfer = EinheitenTyp(name = "Flammenwerfer", reichweite = 100, leben = 2600.0, schaden = 2.0, laufweite = 1.2, kristalle = 2200, kuerzel = "FLA", panzerung = 0.3,
            kannAngreifen = KannAngreifen.boden, gebäude = fabrik, hotkey = "a")
    val panzer = EinheitenTyp(name = "Panzer", reichweite = 500, leben = 10000.0, schaden = 5.0, laufweite = 0.25, kristalle = 2500, kuerzel = "PAN", panzerung = 0.4,
            kannAngreifen = KannAngreifen.boden, gebäude = fabrik, techGebäude = reaktor, hotkey = "s")
    val basis = EinheitenTyp(name = "Basis", reichweite = 500, leben = 30000.0, schaden = 12.0, laufweite = 0.0, kristalle = 0, kuerzel = "BAS", panzerung = 0.45,
            kannAngreifen = KannAngreifen.alles, gebäude = null, hotkey = null)
    val jäger = EinheitenTyp(name = "Jäger", reichweite = 120, leben = 800.0, schaden = 3.0, laufweite = 0.8, kristalle = 1800, kuerzel = "JÄG", panzerung = 0.14,
            kannAngreifen = KannAngreifen.alles, luftBoden = LuftBoden.luft, gebäude = raumhafen, hotkey = "d")
    val sanitäter = EinheitenTyp(name = "Sanitäter", reichweite = 40, leben = 800.0, schaden = 2.0, laufweite = 0.7, kristalle = 1100, kuerzel = "SAN", panzerung = 0.0,
            kannAngreifen = KannAngreifen.heilen, gebäude = kaserne, techGebäude = akademie, hotkey = "r")
    val kampfschiff = EinheitenTyp(name = "Kampfschiff", reichweite = 250, leben = 20000.0, schaden = 9.0, laufweite = 0.2, kristalle = 3500, kuerzel = "KSF", panzerung = 0.5,
            kannAngreifen = KannAngreifen.alles, luftBoden = LuftBoden.luft, gebäude = raumhafen, techGebäude = fusionskern, hotkey = "f")
    val kaufbareEinheiten = listOf(infantrie, eliteinfantrie, berserker, panzer, jäger, sanitäter, kampfschiff, flammenwerfer)

    val computer = Spieler(einheiten = mutableListOf(
            einheit(x = 1050.0, y = 110.0, einheitenTyp = infantrie),
            einheit(x = 750.0, y = 110.0, einheitenTyp = berserker),
            einheit(x = 850.0, y = 110.0, einheitenTyp = eliteinfantrie),
            einheit(x = 900.0, y = 50.0, einheitenTyp = basis),
            einheit(x = 950.0, y = 110.0, einheitenTyp = sanitäter)
    ),
            kristalle = 0.0,
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            minen = 0,
            startpunkt = Punkt(x = 900.0, y = 115.0),
            farbe = Color.RED,
            mensch = false,
            schadenUpgrade = 0,
            panzerungUprade = 0
    )

    private fun einheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) =
            Einheit(reichweite = einheitenTyp.reichweite,
                    leben = einheitenTyp.leben,
                    schaden = einheitenTyp.schaden,
                    x = x,
                    y = y,
                    laufweite = einheitenTyp.laufweite,
                    kuerzel = Text(einheitenTyp.kuerzel),
                    panzerung = einheitenTyp.panzerung,
                    typ = einheitenTyp
            )

    val mensch = Spieler(einheiten = mutableListOf(
            einheit(x = 1050.0, y = 895.0, einheitenTyp = infantrie),
            einheit(x = 750.0, y = 895.0, einheitenTyp = berserker),
            einheit(x = 850.0, y = 895.0, einheitenTyp = eliteinfantrie),
            einheit(x = 900.0, y = 970.0, einheitenTyp = basis),
            einheit(x = 950.0, y = 895.0, einheitenTyp = sanitäter)
    ),
            kristalle = 10000000000000000000.0,
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            minen = 0,
            startpunkt = Punkt(x = 900.0, y = 905.0),
            farbe = Color.BLUE,
            mensch = true,
            schadenUpgrade = 0,
            panzerungUprade = 0
    )


    fun kaufen(kristalle: Int, spieler: Spieler = mensch, aktion: () -> Unit) {
        if (spieler.kristalle >= kristalle) {
            aktion()
            spieler.kristalle -= kristalle
        }
    }

    val box: Pane = Pane().apply {
        prefWidth = 1850.0
        prefHeight = 950.0
    }

    var ausgewaehlt: MutableList<Einheit> = mutableListOf()

    private fun kreis(x: Double, y: Double, radius: Double): Arc {
        return Arc().apply {
            centerX = x
            centerY = y
            radiusX = radius
            radiusY = radius
            fill = Color.TRANSPARENT
            stroke = Color.BLACK
            type = ArcType.OPEN
            length = 360.0
        }
    }

    override fun start(stage: Stage) {


        initSpieler(computer)
        initSpieler(mensch)

        mensch.kristalleText.x = 10.0
        mensch.kristalleText.y = 950.0

        mensch.minenText.x = 160.0
        mensch.minenText.y = 950.0

        val vBox = VBox(10.0)

        val scene = Scene(vBox, 1850.0, 1000.0)
        scene.fill = null

        stage.scene = scene
        stage.show()

        val hBox = HBox()



        hBox.children.add(Button("Mine").apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    kaufen(2000 + 400 * mensch.minen) {
                        mensch.minen += 1
                    }
                }


            }
        })
        prodoktionsgebäude.forEach { gebäude ->
            hBox.children.add(Button(gebäude.name).apply {
                onMouseClicked = EventHandler {
                    if (it.button == MouseButton.PRIMARY) {
                        kaufen(gebäude.kristalle) {
                            hBox.children.remove(this)
                            kaufbareEinheiten.filter { it.gebäude == gebäude }.forEach {
                                kaufButton(hBox, it.name, it)
                            }
                        }
                    }

                }
            })
        }
        techgebäude.forEach { gebäude ->
            hBox.children.add(Button(gebäude.name).apply {
                onMouseClicked = EventHandler {
                    if (it.button == MouseButton.PRIMARY) {
                        kaufen(gebäude.kristalle) {
                            hBox.children.remove(this)
                        }
                    }

                }
            })
        }



        hBox.children.add(Button("Labor").apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    kaufen(2800) {
                        hBox.children.add(Button("LV " + (mensch.schadenUpgrade + 1) + " Schaden").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(2000 + 400 * mensch.schadenUpgrade) {
                                        mensch.schadenUpgrade += 1
                                        this.text = "LV " + (mensch.schadenUpgrade + 1) + " Schaden"

                                        if (mensch.schadenUpgrade >= 5) {
                                            hBox.children.remove(this)
                                        }
                                    }
                                }

                            }
                        })
                        hBox.children.add(Button("LV " + (mensch.panzerungUprade + 1) + " Panzerug").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(2000 + 400 * mensch.panzerungUprade) {
                                        mensch.panzerungUprade += 1
                                        this.text = "LV " + (mensch.panzerungUprade + 1) + " Panzerug"

                                        if (mensch.panzerungUprade >= 5) {
                                            hBox.children.remove(this)
                                        }
                                    }
                                }


                            }
                        })
                        hBox.children.remove(this)
                    }
                }
            }
        })



        vBox.children.add(box)
        vBox.children.add(hBox)

        box.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.SECONDARY) {
                ausgewaehlt.forEach { einheit ->
                    laufBefehl(einheit, event)
                }
            }
        }

        var auswahlStart: Punkt? = null
        var auswahlRechteck: Rectangle? = null

        box.onMousePressed = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                auswahlStart = Punkt(it.x, it.y)
            }

        }
        box.onMouseDragged = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                auswahlStart?.let { s ->
                    val x = min(s.x, it.x)
                    val y = min(s.y, it.y)
                    val mx = max(s.x, it.x)
                    val my = max(s.y, it.y)

                    val r = auswahlRechteck ?: Rectangle().apply {
                        fill = Color.TRANSPARENT
                        stroke = Color.BLACK
                        strokeWidth = 2.0
                    }
                    r.x = x
                    r.y = y
                    r.width = mx - x
                    r.height = my - y

                    if (auswahlRechteck == null) {
                        auswahlRechteck = r
                        box.children.add(auswahlRechteck)
                    }
                }
            }

        }
        box.onMouseReleased = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY) {

                auswahlRechteck?.let { r ->
                    mensch.einheiten.forEach {
                        if (it.bild.boundsInParent.intersects(r.boundsInParent)) {
                            `auswahl löschen`()
                        }
                    }
                    mensch.einheiten.forEach {
                        if (it.bild.boundsInParent.intersects(r.boundsInParent)) {
                            auswählen(it)
                        }
                    }


                    box.children.remove(r)
                }
                auswahlStart = null
                auswahlRechteck = null
                event.consume()
            }

        }

        Thread(Runnable {
            while (true) {
                Platform.runLater {
                    runde()
                    male()
                }
                Thread.sleep(15)
            }
        }).start()
    }

    private fun `auswahl löschen`() {
        ausgewaehlt.forEach {
            box.children.remove(it.auswahlkreis)
            it.auswahlkreis = null
        }
        ausgewaehlt.clear()
    }

    private fun kaufButton(hBox: HBox, text: String, einheitenTyp: EinheitenTyp) {
        hBox.children.add(Button(text).apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    produzieren(spieler = mensch, einheitenTyp = einheitenTyp)
                }

            }
        })
    }

    private fun laufBefehl(einheit: Einheit, event: MouseEvent) {
        val x = event.x
        val y = event.y
        einheit.zielPunkt = Punkt(x = x, y = y)
        einheit.ziel = null

        maleZiel(einheit, x, y)
    }

    private fun maleZiel(einheit: Einheit, x: Double, y: Double) {
        val z = einheit.zielpunktkreis
        if (z != null) {
            box.children.remove(z)
        }
        einheit.zielpunktLinie?.let { box.children.remove(it) }

        einheit.zielpunktkreis = kreis(x = x, y = y, radius = 5.0).apply {
            box.children.add(this)
        }
        einheit.zielpunktLinie = Line().apply {
            startX = einheit.x
            startY = einheit.y
            endX = x
            endY = y
            box.children.add(this)
        }
    }

    private fun initSpieler(spieler: Spieler) {
        spieler.einheiten.forEach {
            bildHinzufuegen(spieler, it)
        }
        box.children.add(spieler.kristalleText)
        box.children.add(spieler.minenText)
    }

    private fun bildHinzufuegen(spieler: Spieler, einheit: Einheit) {
        einheit.bild.fill = spieler.farbe
        box.children.add(einheit.bild)
        box.children.add(einheit.lebenText)
        box.children.add(einheit.kuerzel)

        einheitMouseHandler(spieler, einheit.bild, einheit)
        einheitMouseHandler(spieler, einheit.kuerzel, einheit)
    }

    private fun einheitMouseHandler(spieler: Spieler, imageView: Node, einheit: Einheit) {
        if (spieler.mensch) {
            imageView.onMouseClicked = EventHandler { event ->
                if (event.button == MouseButton.PRIMARY) {
                    `auswahl löschen`()
                    auswählen(einheit)
                    event.consume()
                }

            }
        } else {
            imageView.onMouseClicked = EventHandler { event ->
                if (event.button == MouseButton.SECONDARY)
                    ausgewaehlt.forEach {
                        it.ziel = einheit
                        it.zielPunkt = null
                        maleZiel(it, einheit.x, einheit.y)
                        event.consume()
                    }
            }
        }
    }

    private fun auswählen(einheit: Einheit) {
        if (einheit.leben > 0 && einheit.auswahlkreis == null) {
            var auswahlKreis: Arc = kreis(x = -100.0, y = -100.0, radius = 25.0)
            box.children.add(auswahlKreis)
            einheit.auswahlkreis = auswahlKreis
            ausgewaehlt.add(einheit)
        }
    }

    private fun runde() {
        computer.kristalle += 1.0 + 0.2 * computer.minen
        mensch.kristalle += 1.0 + 0.2 * mensch.minen
        produzieren(spieler = computer, einheitenTyp = panzer)
        bewegeSpieler(computer, mensch)
        bewegeSpieler(mensch, computer)

        schiessen(computer, mensch)
        schiessen(mensch, computer)

        //man gewinnt, wenn man die Basis des Gegners zerstoert
        computer.einheiten.toList().forEach { entfernen(it, computer, mensch) }
        mensch.einheiten.toList().forEach { entfernen(it, mensch, computer) }

        if (computer.einheiten.none { it.typ == basis }) {
            box.children.add(Text("Sieg").apply {
                x = 700.0
                y = 500.0
                font = Font(200.0)
            })
        }
        if (mensch.einheiten.none { it.typ == basis }) {
            box.children.add(Text("Niderlage").apply {
                x = 300.0
                y = 500.0
                font = Font(200.0)
            })
        }
    }

    private fun schiessen(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach {
            val ziel = zielauswaehlenSchießen(gegner, it)
            if (ziel != null) {
                schiessen(it, ziel, spieler)
            }
        }
    }

    fun produzieren(spieler: Spieler, einheitenTyp: EinheitenTyp) {
        kaufen(einheitenTyp.kristalle, spieler) {
            val einheit = einheit(
                    x = spieler.startpunkt.x,
                    y = spieler.startpunkt.y,
                    einheitenTyp = einheitenTyp)
            spieler.einheiten.add(einheit)
            bildHinzufuegen(spieler, einheit)
        }
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { einheit -> bewege(einheit, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        val ziel = zielpunktAuswaehlen(gegner, einheit)
        if (ziel == null) {
            return
        }

        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x
        val e = entfernung(einheit, ziel)

        val reichweite = if (zielauswaehlenBewegen(gegner, einheit) != null) einheit.reichweite else 0
        if (e - reichweite >= 0) {
            einheit.x += smaller(b, b * einheit.laufweite / e)
            einheit.y += smaller(a, a * einheit.laufweite / e)
        }

        if (einheit.zielPunkt != null && einheit.zielPunkt == einheit.punkt()) {
            zielEntfernen(einheit)
        }
    }

    private fun zielEntfernen(einheit: Einheit) {
        einheit.zielpunktLinie?.let { box.children.remove(it) }
        einheit.zielpunktkreis?.let { box.children.remove(it) }
        einheit.zielpunktLinie = null
        einheit.zielpunktkreis = null
        einheit.zielPunkt = null
        einheit.ziel = null
    }

    fun smaller(a: Double, b: Double): Double {
        if (a.absoluteValue <= b.absoluteValue) {
            return a
        }
        return b
    }

    private fun zielpunktAuswaehlen(gegner: Spieler, einheit: Einheit): Punkt? {
        val ziel = zielauswaehlenBewegen(gegner, einheit)
        if (ziel != null) {
            return ziel.punkt()
        } else {
            return einheit.zielPunkt
        }
    }

    private fun zielauswaehlenSchießen(gegner: Spieler, einheit: Einheit): Einheit? {
        if (einheit.ziel != null) {
            return einheit.ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return heilen(gegner, einheit)
        }

        val naechsteEinheit =
                gegner.einheiten.minBy { entfernung(einheit, it) }
        val naechsteBedrohlichEinheit =
                gegner.einheiten
                        .filter { entfernung(einheit, it) <= 300 && kannAngreifen(it, einheit) }
                        .minBy { entfernung(einheit, it) }
        if (naechsteBedrohlichEinheit != null) {
            return naechsteBedrohlichEinheit
        }

        //automatisch auf Einheiten in Reichweite schiessen
        if (naechsteEinheit != null) {
            if (`ist in Reichweite`(einheit, naechsteEinheit)) {
                return naechsteEinheit
            }
        }
        return einheit.ziel
    }

    private fun heilen(gegner: Spieler, einheit: Einheit): Einheit? {
        val naechsteEinheit = `nächste Einheit zum Heilen`(gegner, einheit)

        if (naechsteEinheit != null) {
            if (`ist in Reichweite`(einheit, naechsteEinheit)) {
                return naechsteEinheit
            }
        }

        return null
    }

    private fun zielauswaehlenBewegen(gegner: Spieler, einheit: Einheit): Einheit? {
        if (einheit.ziel != null) {
            return einheit.ziel
        }
        if (einheit.zielPunkt != null) {
            return null
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return `nächste Einheit zum Heilen`(gegner, einheit)
        }

        val verbündeter = `verbündetem helfen`(gegner, einheit)
        if (verbündeter != null) {
            return verbündeter
        }

        if (gegner.mensch) {
            val naechsteEinheit = gegner.einheiten
                    .filter { kannAngreifen(einheit, it) }
                    .minBy { entfernung(einheit, it) }

            return naechsteEinheit
        }

        return null
    }

    private fun `verbündetem helfen`(gegner: Spieler, einheit: Einheit): Einheit? {
        gegner.einheiten.forEach { gEinheit ->
            val spieler = gegner(gegner)
            spieler.einheiten.forEach { verbündeter ->

                if (`ist Verbündeter bedroht`(gEinheit, verbündeter, einheit)) {
                    return gEinheit
                }
            }
        }
        return null
    }

    private fun `ist Verbündeter bedroht`(gegner: Einheit, verbündeter: Einheit, einheit: Einheit): Boolean {
        return `ist in Reichweite`(gegner, verbündeter) &&
                entfernung(einheit, verbündeter.punkt()) < 300 &&
                kannAngreifen(einheit, gegner)
    }

    private fun `nächste Einheit zum Heilen`(gegner: Spieler, einheit: Einheit) =
            gegner(gegner).einheiten
                    .filter { it.leben < it.typ.leben &&
                            entfernung(einheit, it) < 300 &&
                            einheit != it}
                    .minBy { entfernung(einheit, it) }

    fun gegner(spieler: Spieler): Spieler {
        if (spieler == mensch) {
            return computer
        }
        return mensch
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        if (entfernung(einheit, ziel) - einheit.reichweite < 0.0) {
            if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
                ziel.leben = min(ziel.leben + einheit.schaden, ziel.typ.leben)
            } else {
                ziel.leben -= einheit.schaden + spieler.schadenUpgrade / 10 - ziel.panzerung - gegner(spieler).panzerungUprade / 10
            }
        }
    }

    private fun entfernen(einheit: Einheit, spieler: Spieler, gegner: Spieler) {
        if (einheit.leben < 1) {
            spieler.einheiten.remove(einheit)

            if (ausgewaehlt.contains(einheit)) {
                val kreis = einheit.auswahlkreis
                ausgewaehlt.remove(einheit)
                box.children.remove(kreis)
            }

            gegner.einheiten.forEach { g ->
                if (g.ziel == einheit) {
                    zielEntfernen(g)
                }
            }
            box.children.remove(einheit.bild)
            box.children.remove(einheit.lebenText)
            box.children.remove(einheit.kuerzel)
            einheit.zielpunktLinie?.let { box.children.remove(it) }
            einheit.zielpunktkreis?.let { box.children.remove(it) }
        }
    }

    fun `ist in Reichweite`(einheit: Einheit, ziel: Einheit): Boolean {
        return entfernung(einheit, ziel) < einheit.reichweite
    }

    fun entfernung(einheit: Einheit, ziel: Einheit): Double {
        if (!kannAngreifen(einheit, ziel)) {
            return 7000000000000000000.0
        }

        return entfernung(einheit, ziel.punkt())
    }

    private fun kannAngreifen(einheit: Einheit, ziel: Einheit) =
            !(ziel.typ.luftBoden == LuftBoden.luft && einheit.typ.kannAngreifen == KannAngreifen.boden)

    fun entfernung(einheit: Einheit, ziel: Punkt): Double {
        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x

        return sqrt(a.pow(2) + b.pow(2))
    }

    fun male() {
        computer.einheiten.toList().forEach { maleEinheit(it) }
        mensch.einheiten.toList().forEach { maleEinheit(it) }
        mensch.kristalleText.text = "Kristalle: " + mensch.kristalle.toInt().toString()
        mensch.minenText.text = "Minen: " + mensch.minen.toString()

        ausgewaehlt.forEach { einheit ->
            einheit.auswahlkreis!!.centerX = einheit.bild.layoutX
            einheit.auswahlkreis!!.centerY = einheit.bild.layoutY
        }
    }

    fun maleEinheit(einheit: Einheit) {
        val kreis = einheit.bild

        kreis.layoutX = einheit.x
        kreis.layoutY = einheit.y

        val lebenText = einheit.lebenText
        lebenText.x = einheit.x - 10
        lebenText.y = einheit.y - 30
        lebenText.text = einheit.leben.toInt().toString()

        einheit.kuerzel.x = einheit.x - 12
        einheit.kuerzel.y = einheit.y

        einheit.zielpunktLinie?.apply {
            startX = einheit.x
            startY = einheit.y
            val ziel = einheit.ziel
            if (ziel != null) {
                endX = ziel.x
                endY = ziel.y
                einheit.zielpunktkreis!!.centerX = ziel.x
                einheit.zielpunktkreis!!.centerY = ziel.y
            }
        }
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }

        fun einheitenBild(): Circle {
            return Circle(20.toDouble())
        }
    }
}
//Einheiten können nurvon einem Sanitäter gleichzeitig geheilt werden.
//Tech gebeude werden benötigt um bestimmte einheiten herstellen zu können.
//K.I. :Sammelt erst die Truppen und greift dann an; baut verschiedene Einheiten, Minen, Upgrates und Gebäude
//Upgrades nur für eine bestimmte Einheit
//einheiten nicht übereinander
//größere Karte
//Minnimap
//keine Sicht auf der karte
//Sichtweite für Einheiten
//Spetialressourcenquellen auf der Karte
//einheitenfaehikkeiten
//produktionszeit
//mit Tastatur prodozieren
//lebensanzeige(lebensbalken)
//rassen
//bessere Grafik
//Gebäude platzieren
//attackmove
//shiftbefehl