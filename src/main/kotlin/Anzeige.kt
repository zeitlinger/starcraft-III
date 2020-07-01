@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis", "LiftReturnOrAssignment", "NonAsciiCharacters", "PropertyName", "EnumEntryName", "SpellCheckingInspection")

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
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

@Suppress("SpellCheckingInspection")
class App() : Application() {
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
                        spiel.produzieren(mensch, arbeiter)
                    }
            }
        })
        prodoktionsgebäude.forEach { gebäude ->
            produktionsgebäude(hBox, gebäude)
        }

        hBox.onKeyPressed = EventHandler<KeyEvent> { event ->
            kaufbareEinheiten.singleOrNull { event.text == it.hotkey }?.button?.fire()
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

                                        if (mensch.schadensUpgrade >= 5) {
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

                                        if (mensch.panzerungsUprade >= 5) {
                                            hBox.children.remove(this)
                                        }
                                    }
                                }
                            }
                        })
                        hBox.children.add(Button("Ansturm").apply {
                            onMouseClicked = EventHandler {
                                if (it.button == MouseButton.PRIMARY) {
                                    kaufen(1500,mensch) {
                                        berserker.laufweite = 1.0
                                        berserker.springen = 150
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
                ausgewaehlt.forEach { einheit ->
                    laufBefehl(einheit, event)
                }
            }
            event.consume()
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
                if (event.button == MouseButton.PRIMARY) {
                    `auswahl löschen`()
                    auswählen(einheit)

                } else if (event.button == MouseButton.SECONDARY) {
                    `ziel auswählen`(einheit)
                }
                event.consume()
            }
        } else {
            imageView.onMouseClicked = EventHandler { event ->
                if (event.button == MouseButton.SECONDARY) {
                    `ziel auswählen`(einheit)
                }
                event.consume()
            }
        }
    }

    private fun `ziel auswählen`(einheit: Einheit) {
        ausgewaehlt.forEach {
            it.ziel = einheit
            it.zielPunkt = null
            maleZiel(it, einheit.x, einheit.y)
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
                einheit(x = 1050.0, y = 110.0, einheitenTyp = späher)
                einheit(x = 750.0, y = 110.0, einheitenTyp = arbeiter)
                einheit(x = 850.0, y = 110.0, einheitenTyp = infantrie)
                einheit(x = 900.0, y = 50.0, einheitenTyp = basis)
                einheit(x = 950.0, y = 110.0, einheitenTyp = infantrie)
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
                einheit(x = 1050.0, y = 895.0, einheitenTyp = späher)
                einheit(x = 750.0, y = 895.0, einheitenTyp = arbeiter)
                einheit(x = 850.0, y = 895.0, einheitenTyp = infantrie)
                einheit(x = 900.0, y = 970.0, einheitenTyp = basis)
                einheit(x = 950.0, y = 895.0, einheitenTyp = infantrie)
            }

            spiel = Spiel(mensch, computer)

            launch(App::class.java)
        }
    }
}

fun einheitenBild(): Circle {
    return Circle(20.toDouble())
}


//Einheiten können nurvon einem Sanitäter gleichzeitig geheilt werden.
//K.I. :Sammelt erst die Truppen und greift dann an; baut verschiedene Einheiten, Minen, Upgrates und Gebäude
//Upgrades für eine bestimmte Einheiten:
// berserker: Marine,
// Elitemarine: stim,
// Sanitäter: alle Einheiten heilen, schneller heilen,
// Flammenwerfer: flächenschaden,
// panzer: reichweite,
// jäger: schneller,
// kampfschiff: yamatokanone
//einheiten nicht übereinander
//größere Karte
//Minnimap
//keine Sicht auf der karte
//Sichtweite für Einheiten
//Spetialressourcenquellen auf der Karte
//arbeiter und wissenschafter kann ressoursen einsammeln
//produktionszeit
//lebensanzeige(lebensbalken)
//rassen - silikoiden: neue rasse
//bessere Grafik
//Gebäude platzieren
//attackmove
//shiftbefehl
//gebäude auswählen
//kontrollgruppen
//kampagne
//nicht schiesssen beim laufen