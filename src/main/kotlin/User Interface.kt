@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection", "FunctionName", "ClassName", "LocalVariableName"
)

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventType
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.stage.Stage
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min


lateinit var spiel: Spiel

val karte: Pane = Pane().apply {
    prefWidth = 3850.0
    prefHeight = 2950.0
}

fun Node.mausTaste(
    button: MouseButton,
    type: EventType<MouseEvent> = MouseEvent.MOUSE_PRESSED,
    filter: () -> Boolean = { true },
    aktion: (MouseEvent) -> Unit
) {
    this.addEventFilter(type) { event ->
        if (filter()) {
            if (event.button == button) {
                aktion(event)
            }
            event.consume()
        }
    }
}

var ausgewaehlt: MutableSet<Einheit> = Collections.newSetFromMap(IdentityHashMap<Einheit, Boolean>())

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

enum class KommandoWählen(val hotkey: String) {
    Bewegen("b"),
    Attackmove("a"),
    Patrolieren("p"),
    Yamatokanone("y")
}

enum class Laufbefehl(val wählen: KommandoWählen) {
    Bewegen(KommandoWählen.Bewegen),
    Attackmove(KommandoWählen.Attackmove),
    Patrolieren(KommandoWählen.Patrolieren)
}

@Suppress("SpellCheckingInspection")
class App(var kommandoWählen: KommandoWählen? = null) : Application() {
    val computer = spiel.computer
    val mensch = spiel.mensch
    lateinit var buttonLeiste: HBox

    fun button(name: String, aktion: (Button) -> Unit): Button = Button(name).apply {
        this.mausTaste(MouseButton.PRIMARY) {
            aktion(this)
        }
        buttonLeiste.children.add(this)
    }

    fun kaufButton(name: String, kritalle: Int, aktion: (Button) -> Unit): Button =
        button(name) {
            kaufen(kritalle, mensch) {
                aktion(it)
            }
        }.apply {
            mensch.addKristallObserver { this.isDisable = it < kritalle }
        }

    fun einmalKaufen(name: String, kritalle: Int, aktion: () -> Unit): Button =
        kaufButton(name, kritalle) {
            aktion()
            buttonLeiste.children.remove(it)
        }

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

        buttonLeiste = HBox()
        kaufButton("Mine", 2000 + 400 * mensch.minen) {
            mensch.minen += 1
        }
        kaufButton("Arbeiter", mArbeiter.kristalle) {
            spiel.neueEinheit(mensch, mArbeiter)
        }
        prodoktionsgebäude.forEach { gebäude ->
            produktionsgebäude(gebäude)
        }

        stage.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (ausgewaehlt.size == 1 && ausgewaehlt.iterator().next().typ == mBasis) {
                kaufbareEinheiten.singleOrNull { event.text == it.hotkey }?.button?.fire()
            } else {
                if (ausgewaehlt.size > 0 && ausgewaehlt.none { it.typ == mBasis }) {
                    auswahlHotkeys(scene, event.text, event.isShiftDown)
                }
            }
        }

        einmalKaufen("Labor", 2800) {
            laborGekauft()
        }

        vBox.children.add(scrollPane(vBox))
        vBox.children.add(buttonLeiste)

        karte.mausTaste(MouseButton.SECONDARY) {
            if (kommandoWählen != null) {
                scene.cursor = Cursor.DEFAULT
                kommandoWählen = null
            } else {
                ausgewaehlt.forEach { einheit ->
                    laufBefehl(einheit, it, laufbefehl = Laufbefehl.Bewegen, schiftcommand = it.isShiftDown)
                }
            }
        }

        var auswahlStart: Punkt? = null
        var auswahlRechteck: Rectangle? = null

        karte.mausTaste(MouseButton.PRIMARY) {
            val laufbefehl = Laufbefehl.values().singleOrNull { it.wählen == kommandoWählen }
            if (laufbefehl != null) {
                ausgewaehlt.forEach { einheit ->
                    laufBefehl(
                        einheit = einheit,
                        event = it,
                        laufbefehl = laufbefehl,
                        schiftcommand = it.isShiftDown
                    )
                }
            } else {
                auswahlStart = Punkt(it.x, it.y)
            }
            scene.cursor = Cursor.DEFAULT
            kommandoWählen = null
        }
        karte.mausTaste(MouseButton.PRIMARY, MouseEvent.MOUSE_DRAGGED) {
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
                    karte.children.add(auswahlRechteck)
                }
            }
        }
        karte.mausTaste(MouseButton.PRIMARY, MouseEvent.MOUSE_RELEASED) {
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

                karte.children.remove(r)
            }
            auswahlStart = null
            auswahlRechteck = null
        }

        Thread {
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
        }.start()
    }

    private fun scrollPane(vBox: VBox): ScrollPane {
        val scroll = ScrollPane(karte)
        scroll.minWidth = 3000.0
        scroll.minWidth = 3000.0
        scroll.isPannable = false
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.addEventFilter(MouseEvent.MOUSE_MOVED) {
            val sensitivity = 150
            val h = (scroll.hmax - scroll.hmin) / 100
//            val h = scroll.minWidth / vBox.width / 100
            if (it.x < sensitivity) {
                scroll.hvalue -= h
            }
            if (it.x > vBox.width - sensitivity) {
                scroll.hvalue += h
            }
            val v = (scroll.vmax - scroll.vmin) / 100
//            val v = scroll.minHeight / vBox.height / 100
            if (it.y < sensitivity) {
                scroll.vvalue -= v
            }
            if (it.y > vBox.height - sensitivity) {
                scroll.vvalue += v
            }
        }
        return scroll
    }

    private fun auswahlHotkeys(scene: Scene, text: String?, schift: Boolean) {
        val wählen = KommandoWählen.values().singleOrNull { it.hotkey == text }
        if (wählen != null) {
            kommandoWählen = wählen
            scene.cursor = Cursor.CROSSHAIR
            return
        } else {
            val k = kommandoHotKeys[text]
            if (k != null) {
                ausgewaehlt.forEach {
                    neuesKommando(
                        einheit = it,
                        kommando = k(),
                        schiftcommand = schift
                    )
                }
            }
        }
    }

    private fun laborGekauft() {
        kaufButton("LV " + (mensch.schadensUpgrade + 1) + " Schaden", 2000 + 400 * mensch.schadensUpgrade) {
            mensch.schadensUpgrade += 1
            it.text = "LV " + (mensch.schadensUpgrade + 1) + " Schaden"

            if (mensch.schadensUpgrade == 5) {
                buttonLeiste.children.remove(it)
            }
        }
        kaufButton("LV " + (mensch.panzerungsUprade + 1) + " Panzerug", 2000 + 400 * mensch.panzerungsUprade) {
            mensch.panzerungsUprade += 1
            it.text = "LV " + (mensch.panzerungsUprade + 1) + " Panzerug"

            if (mensch.panzerungsUprade == 5) {
                buttonLeiste.children.remove(it)
            }
        }
        einmalKaufen("Ansturm", 1500) {
            mBerserker.laufweite = 1.0
            mBerserker.springen = 150
        }
        einmalKaufen("Verbesserte Zielsysteme", 1500) {
            mPanzer.reichweite = 500.0
        }
        einmalKaufen("Fusionsantrieb", 1500) {
            mJäger.laufweite = 1.2
            mKampfschiff.laufweite = 0.3
        }
        einmalKaufen("Verstärkte Heilmittel", 1500) {
            mSanitäter.schaden = 3.0
            mensch.vertärkteHeilmittel = true
        }
        einmalKaufen("Strahlungsheilung", 1500) {
            mSanitäter.reichweite = 140.01
            mensch.strahlungsheilung = true
        }
        einmalKaufen("Flammenwurf", 1500) {
            mFlammenwerfer.flächenschaden = 40
            mFlammenwerfer.schaden = 2.5
        }
    }

    private fun produktionsgebäude(gebäude: Gebäude) {
        einmalKaufen(gebäude.name, gebäude.kristalle) {
            techgebäude.filter { it.gebäude == gebäude }.forEach { gebäude ->
                einmalKaufen(gebäude.name, gebäude.kristalle) {
                    kaufbareEinheiten.filter { it.techGebäude == gebäude }.forEach { it.button!!.isDisable = false }
                }
            }
            kaufbareEinheiten.filter { it.gebäude == gebäude }.forEach { typ ->
                val button = kaufButton(typ.name, typ.kristalle) {
                    spiel.neueEinheit(mensch, typ)
                }
                if (typ.techGebäude != null) {
                    button.isDisable = true
                }
                typ.button = button
            }
        }
    }

    private fun `auswahl löschen`() {
        ausgewaehlt.forEach {
            karte.children.remove(it.auswahlkreis)
            it.auswahlkreis = null
        }
        ausgewaehlt.clear()
    }

    private fun laufBefehl(einheit: Einheit, event: MouseEvent, laufbefehl: Laufbefehl, schiftcommand: Boolean) {

        val x = event.x
        val y = event.y
        val letzterPunkt = if (!schiftcommand || einheit.kommandoQueue.isEmpty()) {
            einheit.punkt()
        } else {
            einheit.punkt()
        }

        val kommando = when (laufbefehl) {
            Laufbefehl.Attackmove -> {
                Kommando.Attackmove(zielPunkt = Punkt(x, y))
            }
            Laufbefehl.Bewegen -> {
                Kommando.Bewegen(Punkt(x, y))
            }
            else -> {
                Kommando.Patrolieren(letzterPunkt, Punkt(x, y))
            }
        }
        neuesKommando(einheit, kommando, schiftcommand)
        zielpunktKreisUndLinieHinzufügen(kommando, einheit)
    }

    private fun neuesKommando(einheit: Einheit, kommando: Kommando, schiftcommand: Boolean) {
        einheit.kommandoQueue.toList().forEach {
            if (!schiftcommand) {
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
            karte.children.add(this)
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
            karte.children.add(this)
        }
    }

    private fun kommandoPosition(letztesKommando: Kommando?, einheit: Einheit) = when (letztesKommando) {
        null -> Punkt(einheit.x, einheit.y)
        is Kommando.Angriff -> Punkt(letztesKommando.ziel.x, letztesKommando.ziel.y)
        is Kommando.Bewegen -> letztesKommando.zielPunkt
        is Kommando.Attackmove -> letztesKommando.zielPunkt
        is Kommando.Patrolieren -> letztesKommando.punkt2
        is Kommando.HoldPosition -> throw AssertionError("kann nicht passieren")
        is Kommando.Stopp -> throw AssertionError("kann nicht passieren")
    }

    private fun initSpieler(spieler: Spieler) {
        spieler.einheiten.forEach {
            einheitUiErstellen(it)
        }
        karte.children.add(spieler.kristalleText)
        karte.children.add(spieler.minenText)
    }

    private fun einheitUiErstellen(einheit: Einheit) {
        val spieler = einheit.spieler

        einheit.kuerzel = Text(einheit.typ.kuerzel)
        einheit.lebenText = Text()
        einheit.bild.fill = spieler.farbe
        karte.children.add(einheit.bild)
        karte.children.add(einheit.lebenText)
        karte.children.add(einheit.kuerzel)

        einheitMouseHandler(spieler, einheit.bild, einheit)
        einheitMouseHandler(spieler, einheit.kuerzel!!, einheit)
    }

    private fun einheitMouseHandler(spieler: Spieler, imageView: Node, einheit: Einheit) {
        imageView.mausTaste(MouseButton.PRIMARY, filter = { kommandoWählen == KommandoWählen.Attackmove }) {
            `ziel auswählen`(einheit, schiftcommand = it.isShiftDown)
        }

        if (spieler.mensch) {
            imageView.mausTaste(MouseButton.PRIMARY, filter = { kommandoWählen == null }) {
                `auswahl löschen`()
                auswählen(einheit)
            }
        }

        imageView.mausTaste(MouseButton.SECONDARY, filter = { kommandoWählen == null }) {
            `ziel auswählen`(einheit, schiftcommand = it.isShiftDown)
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
            val auswahlKreis: Arc = kreis(x = -100.0, y = -100.0, radius = 25.0)
            karte.children.add(auswahlKreis)
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


        if (einheit.kommandoQueue.size > 1) {
            einheit.kommandoQueue[0].zielpunktLinie?.apply {
                startX = einheit.x
                startY = einheit.y
            }
        }

        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando != null && kommando is Kommando.Angriff) {
            val ziel = kommando.ziel

            kommando.zielpunktLinie?.apply {
                endX = ziel.x
                endY = ziel.y
            }
            kommando.zielpunktkreis?.apply {
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
                kristalle = 100.0,
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
