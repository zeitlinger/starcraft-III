@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis", "LiftReturnOrAssignment")

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt


data class EinheitenTyp(
        val schaden: Int,
        val reichweite: Int,
        val leben: Int,
        val laufweite: Double,
        val kristalle: Int
)

data class Einheit(
        val reichweite: Int,
        var leben: Int,
        val schaden: Int,
        var x: Double,
        var y: Double,
        val laufweite: Double,
        val bild: Circle = Main.einheitenBild(),
        val lebenText: Text = Text(),
        var ziel: Einheit? = null,
        var zielPunkt: Punkt? = null,
        var zielpunktkreis: Arc? = null,
        var zielpunktLinie: Line? = null
) {
    fun punkt() = Punkt(x = x, y = y)
}

data class Punkt(
        var x: Double,
        var y: Double
)

data class Spieler(
        val einheiten: MutableList<Einheit>,
        var kristalle: Int,
        var angriffspunkte: Int,
        var verteidiegungspunkte: Int,
        var minen: Int,
        var startpunkt: Punkt,
        val farbe: Color,
        val mensch: Boolean,
        val kristalleText: Text = Text()
)

@Suppress("SpellCheckingInspection")
class Main : Application() {

    val infantrie = EinheitenTyp(reichweite = 150, leben = 1000, schaden = 1, laufweite = 0.5, kristalle = 500)
    val berserker = EinheitenTyp(reichweite = 40, leben = 2000, schaden = 4, laufweite = 1.0, kristalle = 1000)
    val panzer = EinheitenTyp(reichweite = 500, leben = 10000, schaden = 5, laufweite = 0.25, kristalle = 2500)

    val computer = Spieler(einheiten = mutableListOf(
            einheit(x = 1000.0, y = 10.0, einheitenTyp = infantrie),
            einheit(x = 800.0, y = 10.0, einheitenTyp = berserker),
            einheit(x = 900.0, y = 10.0, einheitenTyp = panzer)
    ),
            kristalle = 0,
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            minen = 3,
            startpunkt = Punkt(x = 925.0, y = 10.0),
            farbe = Color.RED,
            mensch = false
    )

    private fun einheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) =
            Einheit(reichweite = einheitenTyp.reichweite,
                    leben = einheitenTyp.leben,
                    schaden = einheitenTyp.schaden,
                    x = x,
                    y = y, laufweite = einheitenTyp.laufweite)

    val mensch = Spieler(einheiten = mutableListOf(
            einheit(x = 1000.0, y = 950.0, einheitenTyp = infantrie),
            einheit(x = 800.0, y = 950.0, einheitenTyp = berserker),
            einheit(x = 900.0, y = 950.0, einheitenTyp = panzer)
    ),
            kristalle = 0,
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            minen = 3, startpunkt = Punkt(x = 925.0, y = 950.0),
            farbe = Color.BLUE,
            mensch = true)

    val box: Pane = Pane().apply {
        prefWidth = 1850.0
        prefHeight = 950.0
    }

    var ausgewaehlt: Einheit? = null

    var auswahlKreis: Arc = kreis(x = -100.0, y = -100.0, radius = 25.0)

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
        box.children.add(auswahlKreis)

        initSpieler(computer)
        initSpieler(mensch)

        mensch.kristalleText.x = 10.0
        mensch.kristalleText.y = 950.0

        val vBox = VBox(10.0)

        val scene = Scene(vBox, 1850.0, 1000.0)
        scene.fill = null

        stage.scene = scene
        stage.show()

        val hBox = HBox()
        hBox.children.add(Button("Panzer").apply {
            onMouseClicked = EventHandler {
                produzieren(spieler = mensch, einheitenTyp = panzer)
            }
        })
        hBox.children.add(Button("berserker").apply {
            onMouseClicked = EventHandler {
                produzieren(spieler = mensch, einheitenTyp = berserker)
            }
        })
        hBox.children.add(Button("infantrie").apply {
            onMouseClicked = EventHandler {
                produzieren(spieler = mensch, einheitenTyp = infantrie)
            }
        })

        vBox.children.add(box)
        vBox.children.add(hBox)

        box.onMouseClicked = EventHandler { event ->
            ausgewaehlt?.let { einheit ->
                if (einheit.leben >= 1) {
                    laufBefehl(einheit, event)
                }
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
    }

    private fun bildHinzufuegen(spieler: Spieler, einheit: Einheit) {
        einheit.bild.fill = spieler.farbe
        box.children.add(einheit.bild)
        box.children.add(einheit.lebenText)

        val imageView = einheit.bild

        if (spieler.mensch) {
            imageView.onMouseClicked = EventHandler { event ->
                if (einheit.leben > 0) {
                    ausgewaehlt = einheit
                    event.consume()
                }
            }
        } else {
            imageView.onMouseClicked = EventHandler { event ->
                ausgewaehlt?.let {
                    if (einheit.leben > 0) {
                        it.ziel = einheit
                        it.zielPunkt = null
                        maleZiel(it, einheit.x, einheit.y)
                        event.consume()
                    }
                }
            }
        }
    }

    private fun runde() {
        computer.kristalle += 1
        mensch.kristalle += 1
        produzieren(spieler = computer, einheitenTyp = panzer)
        bewegeSpieler(computer, mensch)
        bewegeSpieler(mensch, computer)

        schiessen(computer, mensch)
        schiessen(mensch, computer)

        computer.einheiten.toList().forEach { entfernen(it, computer, mensch) }
        mensch.einheiten.toList().forEach { entfernen(it, mensch, computer) }

        if (computer.einheiten.isEmpty()) {
            box.children.add(Text("Sieg").apply {
                x = 700.0
                y = 500.0
                font = Font(200.0)
            })
        }
        if (mensch.einheiten.isEmpty()) {
            box.children.add(Text("Niderlage").apply {
                x = 300.0
                y = 500.0
                font = Font(200.0)
            })
        }
    }

    private fun schiessen(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach {
            val ziel = zielauswaehlen(gegner, it, false)
            if (ziel != null) {
                schiessen(it, ziel)
            }
        }
    }

    fun produzieren(spieler: Spieler, einheitenTyp: EinheitenTyp) {
        if (spieler.kristalle >= einheitenTyp.kristalle) {
            val einheit = einheit(
                    x = spieler.startpunkt.x,
                    y = spieler.startpunkt.y,
                    einheitenTyp = einheitenTyp)
            spieler.einheiten.add(einheit)
            spieler.kristalle -= einheitenTyp.kristalle
            bildHinzufuegen(spieler, einheit)
        }
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { bewege(it, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        val ziel = zielpunktAuswaehlen(gegner, einheit)
        if (ziel == null) {
            return
        }

        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x
        val A = entfernung(einheit, ziel)

        val reichweite = if (zielauswaehlen(gegner, einheit) != null) einheit.reichweite else 0
        if (A - reichweite >= 0) {
            einheit.x += smaller(b, b * einheit.laufweite / A)
            einheit.y += smaller(a, a * einheit.laufweite / A)
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
        val ziel = zielauswaehlen(gegner, einheit)
        if (ziel != null) {
            return ziel.punkt()
        } else {
            return einheit.zielPunkt
        }
    }

    private fun zielauswaehlen(gegner: Spieler, einheit: Einheit, bewegen: Boolean = true): Einheit? {
        val NaechsteEinheit = gegner.einheiten.minBy { entfernung(einheit, it.punkt()) }
        if (gegner.mensch) {
            return NaechsteEinheit
        } else {
            if (einheit.ziel == null && NaechsteEinheit != null) {
                if (entfernung(NaechsteEinheit, einheit.punkt()) < einheit.reichweite) {
                    if (!bewegen) {
                        return NaechsteEinheit
                    }
                }
                //beschuetzerradius 150
                if (bewegen) {
                    gegner.einheiten.forEach { gEinheit ->
                        mensch.einheiten.forEach { mEinheit ->

                            if (entfernung(mEinheit, gEinheit.punkt()) < gEinheit.reichweite &&
                                    entfernung(einheit, mEinheit.punkt()) < 150) {
                                return gEinheit
                            }
                        }
                    }
                }
            }
            return einheit.ziel
        }
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit) {
        if (entfernung(einheit, ziel.punkt()) - einheit.reichweite < 0.0) {
            ziel.leben -= einheit.schaden
        }
    }

    private fun entfernen(einheit: Einheit, spieler: Spieler, gegner: Spieler) {
        if (einheit.leben < 1) {
            spieler.einheiten.remove(einheit)
            if (ausgewaehlt == einheit) {
                ausgewaehlt = null
                auswahlKreis.centerX = -100.0
                auswahlKreis.centerY = -100.0
            }
            gegner.einheiten.forEach {
                if (it.ziel == einheit) {
                    it.ziel = null
                }
            }
            box.children.remove(einheit.bild)
            box.children.remove(einheit.lebenText)
            einheit.zielpunktLinie?.let { box.children.remove(it) }
            einheit.zielpunktkreis?.let { box.children.remove(it) }
        }
    }

    fun entfernung(einheit: Einheit, ziel: Punkt): Double {
        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x
        val A = sqrt(a.pow(2) + b.pow(2))

        return A
    }

    fun male() {
        computer.einheiten.toList().forEach { maleEinheit(it) }
        mensch.einheiten.toList().forEach { maleEinheit(it) }
        mensch.kristalleText.text = "Kristalle: " + mensch.kristalle.toString()

        ausgewaehlt?.let {
            auswahlKreis.centerX = it.bild.layoutX
            auswahlKreis.centerY = it.bild.layoutY
        }
    }

    fun maleEinheit(einheit: Einheit) {
        val kreis = einheit.bild

        kreis.layoutX = einheit.x
        kreis.layoutY = einheit.y

        val lebenText = einheit.lebenText
        lebenText.x = einheit.x - 10
        lebenText.y = einheit.y - 30
        lebenText.text = einheit.leben.toString()

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
//einheitenkuerzel sehen
//Basis -> wegpunkt fuer Einheiten
//fliegende Einheiten
//Sanitaeter
//Minen
//Tech gebeude
//K.I.
//Upgrates
