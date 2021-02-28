@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection"
)

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.scene.text.Text
import javafx.stage.Stage
import sun.awt.util.IdentityArrayList
import java.awt.Point
import kotlin.math.*


lateinit var spiel: Spiel

val box: Pane = Pane().apply {
    prefWidth = 1850.0
    prefHeight = 950.0
}

var ausgewaehlt: MutableList<Einheit> = IdentityArrayList()

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

enum class KommandoWählen {
    Bewegen,
    Attackmove,
    Patrolieren,
    HoldPosition
}

@Suppress("SpellCheckingInspection")
class App(var kommandoWählen: KommandoWählen? = null) : Application() {
    val computer = spiel.computer
    val mensch = spiel.mensch

    override fun start(stage: Stage) {
        spiel.einheitProduziert = { einheitUiErstellen(it) }

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
                    kaufen(2000 + 400 * mensch.minen, mensch) {
                        mensch.minen += 1
                    }
                }
            }
        })
        hBox.children.add(Button("Arbeiter").apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    spiel.produzieren(mensch, mArbeiter)
                }
            }
        })
        prodoktionsgebäude.forEach { gebäude ->
            produktionsgebäude(hBox, gebäude)
        }

        hBox.onKeyPressed = EventHandler<KeyEvent> { event ->
            if (ausgewaehlt.size == 1 && ausgewaehlt[0].typ == mBasis) {
                kaufbareEinheiten.singleOrNull { event.text == it.hotkey }?.button?.fire()
            } else {
                if (ausgewaehlt.size > 0 && ausgewaehlt.none { it.typ == mBasis }) {
                    when (event.text) {
                        "a" -> {
                            kommandoWählen = KommandoWählen.Attackmove
                            scene.setCursor(Cursor.CROSSHAIR);
                        }
                        "s" -> {
                            if (!event.isShiftDown) {
                                ausgewaehlt.forEach {
                                    it.kommandoQueue.clear()
                                    it.holdPosition = false
                                }
                            }
                        }
                        "b" -> {
                            kommandoWählen = KommandoWählen.Bewegen
                            scene.setCursor(Cursor.CROSSHAIR);
                        }
                        "h" -> {
                            kommandoWählen = KommandoWählen.HoldPosition
                            val schiftcommand = event.isShiftDown
                            ausgewaehlt.forEach {
                                neuesKommando(
                                    einheit = it,
                                    kommando = Kommando.HoldPosition(),
                                    schiftcommand = schiftcommand
                                )
                            }
                        }
                    }
                }
            }
        }

        hBox.children.add(Button("Labor").apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    kaufen(2800, mensch) {
                        hBox.children.add(Button("LV " + (mensch.schadensUpgrade + 1) + " Schaden").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(2000 + 400 * mensch.schadensUpgrade, mensch) {
                                        mensch.schadensUpgrade += 1
                                        this.text = "LV " + (mensch.schadensUpgrade + 1) + " Schaden"

                                        if (mensch.schadensUpgrade == 5) {
                                            hBox.children.remove(this)
                                        }
                                    }
                                }
                            }
                        })
                        hBox.children.add(Button("LV " + (mensch.panzerungsUprade + 1) + " Panzerug").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(2000 + 400 * mensch.panzerungsUprade, mensch) {
                                        mensch.panzerungsUprade += 1
                                        this.text = "LV " + (mensch.panzerungsUprade + 1) + " Panzerug"

                                        if (mensch.panzerungsUprade == 5) {
                                            hBox.children.remove(this)
                                        }
                                    }
                                }
                            }
                        })
                        hBox.children.add(Button("Ansturm").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mBerserker.laufweite = 1.0
                                        mBerserker.springen = 150
                                        hBox.children.remove(this)
                                    }
                                }
                            }
                        })
                        hBox.children.remove(this)
                        hBox.children.add(Button("Verbesserte Zielsysteme").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mPanzer.reichweite = 500.0
                                        hBox.children.remove(this)
                                    }
                                }
                            }
                        })
                        hBox.children.remove(this)
                        hBox.children.add(Button("Fusionsantrieb").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mJäger.laufweite = 1.2
                                        mKampfschiff.laufweite = 0.3
                                        hBox.children.remove(this)
                                    }
                                }
                            }
                        })
                        hBox.children.remove(this)
                        hBox.children.add(Button("Verstärkte Heilmittel").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mSanitäter.schaden = 3.0
                                        mensch.vertärkteHeilmittel = true
                                        hBox.children.remove(this)
                                    }
                                }
                            }
                        })
                        hBox.children.remove(this)
                        hBox.children.add(Button("Strahlungsheilung").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mSanitäter.reichweite = 140.01
                                        mensch.strahlungsheilung = true
                                        hBox.children.remove(this)
                                    }
                                }
                            }
                        })
                        hBox.children.remove(this)
                        hBox.children.add(Button("Flammenwurf").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500, mensch) {
                                        mFlammenwerfer.flächenschaden = 40
                                        mFlammenwerfer.schaden = 2.5
                                        hBox.children.remove(this)
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
                if (kommandoWählen != null) {
                    scene.setCursor(Cursor.DEFAULT);
                    kommandoWählen = null
                } else {
                    ausgewaehlt.forEach { einheit ->
                        laufBefehl(einheit, event, angriffsZielpunkt = false, schiftcommand = event.isShiftDown)
                    }
                }
            }
            event.consume()
        }

        var auswahlStart: Punkt? = null
        var auswahlRechteck: Rectangle? = null

        box.onMousePressed = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                when (kommandoWählen) {
                    KommandoWählen.Attackmove -> {
                        ausgewaehlt.forEach { einheit ->
                            laufBefehl(
                                einheit = einheit,
                                event = it,
                                angriffsZielpunkt = true,
                                schiftcommand = it.isShiftDown
                            )
                        }
                    }
                    KommandoWählen.Bewegen -> ausgewaehlt.forEach { einheit ->
                        laufBefehl(
                            einheit = einheit,
                            event = it,
                            angriffsZielpunkt = false,
                            schiftcommand = it.isShiftDown
                        )
                    }
                    else -> {
                        auswahlStart = Punkt(it.x, it.y)
                    }
                }
                scene.setCursor(Cursor.DEFAULT);
                kommandoWählen = null
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

        Thread({
            while (true) {

                if (spiel.rundenLimit != null && spiel.runde == spiel.rundenLimit) {
                    Platform.exit()
                    break
                }

                spiel.runde = spiel.runde + 1

                Platform.runLater {
                    spiel.runde()
                    male()
                }
                Thread.sleep(spiel.warteZeit)
            }
        }).start()
    }

    private fun produktionsgebäude(hBox: HBox, gebäude: Gebäude) {
        hBox.children.add(Button(gebäude.name).apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    kaufen(gebäude.kristalle, mensch) {
                        hBox.children.remove(this)
                        techgebäude.filter { it.gebäude == gebäude }.forEach { gebäude ->
                            techgebäude(hBox, gebäude)
                        }
                        kaufbareEinheiten.filter { it.gebäude == gebäude }.forEach {
                            einheitKaufenButton(hBox, it.name, it)
                        }
                    }
                }

            }
        })
    }

    private fun techgebäude(hBox: HBox, gebäude: TechGebäude) {
        hBox.children.add(Button(gebäude.name).apply {
            onMouseClicked = EventHandler {
                if (it.button == MouseButton.PRIMARY) {
                    kaufen(gebäude.kristalle, mensch) {
                        kaufbareEinheiten.filter { it.techGebäude == gebäude }.forEach { it.button!!.isDisable = false }
                        hBox.children.remove(this)
                    }
                }
            }
        })
    }

    private fun `auswahl löschen`() {
        ausgewaehlt.forEach {
            box.children.remove(it.auswahlkreis)
            it.auswahlkreis = null
        }
        ausgewaehlt.clear()
    }

    private fun einheitKaufenButton(hBox: HBox, text: String, einheitenTyp: EinheitenTyp) {
        val button = Button(text).apply {
            if (einheitenTyp.techGebäude != null) {
                isDisable = true
            }
            onAction = EventHandler {
                spiel.produzieren(spieler = mensch, einheitenTyp = einheitenTyp)
            }
        }

        hBox.children.add(button)
        einheitenTyp.button = button
    }

    private fun laufBefehl(einheit: Einheit, event: MouseEvent, angriffsZielpunkt: Boolean, schiftcommand: Boolean) {
        val x = event.x
        val y = event.y

        val kommando =
            if (angriffsZielpunkt) Kommando.Attackmove(zielPunkt = Punkt(x, y)) else Kommando.Bewegen(Punkt(x, y))
        neuesKommando(einheit, kommando, schiftcommand)
        zielpunktKreisUndLinieHinzufügen(kommando, einheit)
    }

    private fun neuesKommando(einheit: Einheit, kommando: Kommando, schiftcommand: Boolean) {
        einheit.kommandoQueue.toList().forEach {
            if (!schiftcommand || it is Kommando.HoldPosition) {
                kommandoEntfernen(einheit, it)
            }
        }
        einheit.kommandoQueue.add(kommando)
    }

    private fun zielpunktKreisUndLinieHinzufügen(kommando: Kommando, einheit: Einheit) {
        val queue = einheit.kommandoQueue
        val letztesKommando = queue.getOrNull(queue.size - 2)
        zielpunktUndKreisHinzufügen(einheit, kommando, letztesKommando)
    }

    private fun zielpunktUndKreisHinzufügen(einheit: Einheit, kommando: Kommando, letztesKommando: Kommando?) {
        val zielPunkt = kommandoPosition(kommando, einheit)
        kommando.zielpunktkreis = kreis(x = zielPunkt.x, y = zielPunkt.y, radius = 5.0).apply {
            box.children.add(this)
        }

        if (einheit.kommandoQueue.size < 2) {
            return
        }

        val startPunkt = kommandoPosition(letztesKommando, einheit)

        if (einheit.kommandoQueue.size == 2) {
            linieHinzufügen(einheit.kommandoQueue.first(), einheit.punkt(), startPunkt)
        }

        linieHinzufügen(kommando, startPunkt, zielPunkt)
    }

    private fun linieHinzufügen(kommando: Kommando, startPunkt: Punkt, zielPunkt: Punkt) {
        kommando.zielpunktLinie = Line().apply {
            startX = startPunkt.x
            startY = startPunkt.y
            endX = zielPunkt.x
            endY = zielPunkt.y
            box.children.add(this)
        }
    }

    private fun kommandoPosition(letztesKommando: Kommando?, einheit: Einheit) = when (letztesKommando) {
        null -> Punkt(einheit.x, einheit.y)
        is Kommando.Angriff -> Punkt(letztesKommando.ziel.x, letztesKommando.ziel.y)
        is Kommando.Bewegen -> letztesKommando.zielPunkt
        is Kommando.Attackmove -> letztesKommando.zielPunkt
        is Kommando.Patrolieren -> letztesKommando.punkt2
        is Kommando.HoldPosition -> throw AssertionError("kann nicht passieren")
    }

    private fun initSpieler(spieler: Spieler) {
        spieler.einheiten.forEach {
            einheitUiErstellen(it)
        }
        box.children.add(spieler.kristalleText)
        box.children.add(spieler.minenText)
    }

    private fun einheitUiErstellen(einheit: Einheit) {
        val spieler = einheit.spieler

        einheit.kuerzel = Text(einheit.typ.kuerzel)
        einheit.lebenText = Text()
        einheit.bild.fill = spieler.farbe
        box.children.add(einheit.bild)
        box.children.add(einheit.lebenText)
        box.children.add(einheit.kuerzel)

        einheitMouseHandler(spieler, einheit.bild, einheit)
        einheitMouseHandler(spieler, einheit.kuerzel!!, einheit)
    }

    private fun einheitMouseHandler(spieler: Spieler, imageView: Node, einheit: Einheit) {
        if (spieler.mensch) {
            imageView.onMouseClicked = EventHandler { event ->
                if (kommandoWählen != null) {
                    if (kommandoWählen == KommandoWählen.Attackmove && event.button == MouseButton.PRIMARY) {
                        `ziel auswählen`(einheit, schiftcommand = event.isShiftDown)
                    }
                    return@EventHandler
                }
                if (event.button == MouseButton.PRIMARY) {
                    `auswahl löschen`()
                    auswählen(einheit)
                } else if (event.button == MouseButton.SECONDARY) {
                    `ziel auswählen`(einheit, schiftcommand = event.isShiftDown)
                }
                event.consume()
            }
        } else {
            imageView.onMouseClicked = EventHandler { event ->
                if (kommandoWählen != null) {
                    if (kommandoWählen == KommandoWählen.Attackmove && event.button == MouseButton.PRIMARY) {
                        `ziel auswählen`(einheit, schiftcommand = event.isShiftDown)
                    }
                    return@EventHandler
                }
                if (event.button == MouseButton.SECONDARY) {
                    `ziel auswählen`(einheit, schiftcommand = event.isShiftDown)
                }
                event.consume()
            }
        }
    }

    private fun `ziel auswählen`(einheit: Einheit, schiftcommand: Boolean) {
        ausgewaehlt.forEach {
            val kommando = Kommando.Angriff(ziel = einheit)
            neuesKommando(einheit = it, kommando = kommando, schiftcommand = schiftcommand)
            zielpunktKreisUndLinieHinzufügen(kommando, einheit)
        }
    }

    private fun auswählen(einheit: Einheit) {
        if (einheit.leben > 0 && einheit.auswahlkreis == null) {
            var auswahlKreis: Arc = kreis(x = -100.0, y = -100.0, radius = 25.0)
            box.children.add(auswahlKreis)
            einheit.auswahlkreis = auswahlKreis
            ausgewaehlt.add(einheit)
            //kommandos anzeigen
            einheit.kommandoQueue.forEach {

            }
        }
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

        val lebenText = einheit.lebenText!!
        lebenText.x = einheit.x - 10
        lebenText.y = einheit.y - 30
        lebenText.text = einheit.leben.toInt().toString()

        einheit.kuerzel!!.x = einheit.x - 12
        einheit.kuerzel!!.y = einheit.y


        if (einheit.kommandoQueue.size > 1) {
            einheit.kommandoQueue[0].zielpunktLinie?.apply {
                startX = einheit.x
                startY = einheit.y
            }
        }

        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando != null && kommando is Kommando.Angriff) {
            val ziel = kommando.ziel

            kommando.zielpunktLinie!!.apply {
                endX = ziel.x
                endY = ziel.y
            }
            kommando.zielpunktkreis!!.apply {
                centerX = ziel.x
                centerY = ziel.y
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val computer = Spieler(
                kristalle = 0.0,
                angriffspunkte = 20,
                verteidiegungspunkte = 10,
                minen = 0,
                startpunkt = Punkt(x = 900.0, y = 115.0),
                farbe = Color.RED,
                mensch = false,
                schadensUpgrade = 0,
                panzerungsUprade = 0
            ).apply {
                einheit(x = 1050.0, y = 110.0, einheitenTyp = cSpäher)
                einheit(x = 750.0, y = 110.0, einheitenTyp = cSonde)
                einheit(x = 850.0, y = 110.0, einheitenTyp = cInfantrie)
                einheit(x = 900.0, y = 50.0, einheitenTyp = cBasis)
                einheit(x = 950.0, y = 110.0, einheitenTyp = cInfantrie)
            }

            val mensch = Spieler(
                kristalle = 10000000000.0,
                angriffspunkte = 20,
                verteidiegungspunkte = 10,
                minen = 0,
                startpunkt = Punkt(x = 900.0, y = 905.0),
                farbe = Color.BLUE,
                mensch = true,
                schadensUpgrade = 0,
                panzerungsUprade = 0
            ).apply {
                einheit(x = 1050.0, y = 895.0, einheitenTyp = mSpäher)
                einheit(x = 750.0, y = 895.0, einheitenTyp = mArbeiter)
                einheit(x = 850.0, y = 895.0, einheitenTyp = mInfantrie)
                einheit(x = 900.0, y = 970.0, einheitenTyp = mBasis)
                einheit(x = 950.0, y = 895.0, einheitenTyp = mInfantrie)
            }

            spiel = Spiel(mensch, computer)

            launch(App::class.java)
        }
    }
}

fun einheitenBild(): Circle {
    return Circle(20.toDouble())
}