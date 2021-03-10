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
import javafx.collections.ObservableList
import javafx.event.EventType
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
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
    consume: Boolean = true,
    aktion: (MouseEvent) -> Unit
) {
    this.addEventHandler(type) { event ->
        if (event.button == button && filter()) {
            aktion(event)
            if (consume) {
                event.consume()
            }
        }
    }
}

var ausgewaehlt: MutableSet<Einheit> = Collections.newSetFromMap(IdentityHashMap())

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
    val gegner = spiel.gegner
    val mensch = spiel.mensch
    val kaufbareEinheiten = mensch.einheitenTypen.values

    lateinit var buttonLeiste: ObservableList<Node>
    val kristalleText: Label = Label().apply { minWidth = 100.0 }
    val minenText: Label = Label().apply { minWidth = 100.0 }

    fun button(name: String, aktion: (Button) -> Unit): Button = Button(name).apply {
        this.mausTaste(MouseButton.PRIMARY) {
            aktion(this)
        }
        buttonLeiste.add(this)
    }

    fun kaufButton(name: String, kritalle: Int, aktion: (Button) -> Unit): Button =
        button(name) {
            kaufen(kritalle, mensch) {
                aktion(it)
            }
        }.apply {
            mensch.addKristallObserver { this.isDisable = it < kritalle }
        }

    fun upgradeKaufen(
        name: String,
        kritalle: Int,
        vararg upgrades: Pair<EinheitenTyp, EinheitenTyp.() -> Unit>,
        spielerUpgrade: SpielerUpgrades.() -> Unit = {}
    ): Button =
        einmalKaufen(name, kritalle) {
            upgrades.forEach { (neutralerTyp, aktion) ->
                val value = mensch.einheitenTypen.getValue(neutralerTyp.name)
                value.aktion()
                spiel.multiplayer.upgrade(value)
            }
            mensch.upgrades.spielerUpgrade()
            spiel.multiplayer.upgrade(mensch)
        }

    fun einmalKaufen(name: String, kritalle: Int, aktion: () -> Unit): Button =
        kaufButton(name, kritalle) {
            aktion()
            buttonLeiste.remove(it)
        }

    override fun start(stage: Stage) {
        stage.title = spiel.mensch.spielerTyp.name

        spiel.einheitProduziert = { einheitUiErstellen(it) }

        val hBox = HBox()
        buttonLeiste = hBox.children
        initSpieler(gegner)
        initSpieler(mensch)

        buttonLeiste.add(kristalleText)
        buttonLeiste.add(minenText)

        val vBox = VBox(10.0)

        val scene = Scene(vBox, 1850.0, 1000.0)
        scene.fill = null

        stage.scene = scene

        kaufButton("Mine", 2000 + 400 * mensch.minen) {
            mensch.minen += 1
        }
        kaufButton("Arbeiter", arbeiter.kristalle) {
            spiel.neueEinheit(mensch, arbeiter)
        }
        prodoktionsgebäude.forEach { gebäude ->
            produktionsgebäude(gebäude)
        }

        stage.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (ausgewaehlt.size == 1 && ausgewaehlt.iterator().next().typ.name == basis.name) {
                kaufbareEinheiten.singleOrNull { event.text == it.hotkey }?.button?.fire()
            } else {
                if (ausgewaehlt.size > 0 && ausgewaehlt.none { it.typ.name == basis.name }) {
                    auswahlHotkeys(scene, event.text, event.isShiftDown)
                }
            }
        }

        einmalKaufen("Labor", 2800) {
            laborGekauft()
        }

        vBox.children.add(scrollPane(vBox))
        vBox.children.add(hBox)

        karte.mausTaste(MouseButton.SECONDARY, consume = false) {
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

        karte.mausTaste(MouseButton.PRIMARY, consume = false) {
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

        stage.show()

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
        scroll.isPannable = false
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.addEventFilter(MouseEvent.MOUSE_MOVED) {
            val sensitivity = 1
            val h = (scroll.hmax - scroll.hmin) / 100
            if (it.x < sensitivity) {
                scroll.hvalue -= h
            }
            if (it.x > vBox.width - sensitivity) {
                scroll.hvalue += h
            }
            val v = (scroll.vmax - scroll.vmin) / 100
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
        val upgrades = mensch.upgrades
        kaufButton("LV " + (upgrades.schadensUpgrade + 1) + " Schaden", 2000 + 400 * upgrades.schadensUpgrade) {
            upgrades.schadensUpgrade += 1
            it.text = "LV " + (upgrades.schadensUpgrade + 1) + " Schaden"

            if (upgrades.schadensUpgrade == 5) {
                buttonLeiste.remove(it)
            }
            spiel.multiplayer.upgrade(mensch)
        }
        kaufButton("LV " + (upgrades.panzerungsUprade + 1) + " Panzerug", 2000 + 400 * upgrades.panzerungsUprade) {
            upgrades.panzerungsUprade += 1
            it.text = "LV " + (upgrades.panzerungsUprade + 1) + " Panzerug"

            if (upgrades.panzerungsUprade == 5) {
                buttonLeiste.remove(it)
            }
            spiel.multiplayer.upgrade(mensch)
        }
        upgradeKaufen("Ansturm", 1500, berserker to {
            laufweite = 1.0
            springen = 150
        })
        upgradeKaufen("Verbesserte Zielsysteme", 1500, panzer to {
            reichweite = 500.0
        })
        upgradeKaufen(
            "Fusionsantrieb", 1500,
            jäger to { laufweite = 1.2 },
            kampfschiff to { laufweite = 0.3 },
        )
        upgradeKaufen(
            "Verstärkte Heilmittel", 1500,
            sanitäter to { schaden = 3.0 },
            spielerUpgrade = { vertärkteHeilmittel = true },
        )
        upgradeKaufen(
            "Strahlungsheilung", 1500,
            sanitäter to { reichweite = 140.01 },
            spielerUpgrade = { strahlungsheilung = true },
        )
        upgradeKaufen("Flammenwurf", 1500, flammenwerfer to {
            flächenschaden = 40.0
            schaden = 2.5
        })
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
                EinheitenKommando.Attackmove(zielPunkt = Punkt(x, y))
            }
            Laufbefehl.Bewegen -> {
                EinheitenKommando.Bewegen(Punkt(x, y))
            }
            else -> {
                EinheitenKommando.Patrolieren(letzterPunkt, Punkt(x, y))
            }
        }
        neuesKommando(einheit, kommando, schiftcommand)
        zielpunktKreisUndLinieHinzufügen(kommando, einheit)
    }

    private fun neuesKommando(einheit: Einheit, kommando: EinheitenKommando, schiftcommand: Boolean) {
        einheit.kommandoQueue.toList().forEach {
            if (!schiftcommand) {
                kommandoEntfernen(einheit, it)
            }
        }
        einheit.kommandoQueue.add(kommando)
        spiel.multiplayer.neueKommandos(einheit)
    }

    private fun zielpunktKreisUndLinieHinzufügen(kommando: EinheitenKommando, einheit: Einheit) {
        val queue = einheit.kommandoQueue
        val letztesKommando = queue.getOrNull(queue.size - 2)
        zielpunktUndKreisHinzufügen(einheit, kommando, letztesKommando)
    }

    private fun zielpunktUndKreisHinzufügen(
        einheit: Einheit,
        kommando: EinheitenKommando,
        letztesKommando: EinheitenKommando?
    ) {
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

    private fun linieHinzufügen(kommando: EinheitenKommando, startPunkt: Punkt, zielPunkt: Punkt) {
        kommando.zielpunktLinie = Line().apply {
            startX = startPunkt.x
            startY = startPunkt.y
            endX = zielPunkt.x
            endY = zielPunkt.y
            karte.children.add(this)
        }
    }

    private fun kommandoPosition(letztesKommando: EinheitenKommando?, einheit: Einheit) = when (letztesKommando) {
        null -> Punkt(einheit.x, einheit.y)
        is EinheitenKommando.Angriff -> Punkt(letztesKommando.ziel.x, letztesKommando.ziel.y)
        is EinheitenKommando.Bewegen -> letztesKommando.zielPunkt
        is EinheitenKommando.Attackmove -> letztesKommando.zielPunkt
        is EinheitenKommando.Patrolieren -> letztesKommando.punkt2
        is EinheitenKommando.HoldPosition -> throw AssertionError("kann nicht passieren")
        is EinheitenKommando.Stopp -> throw AssertionError("kann nicht passieren")
    }

    private fun initSpieler(spieler: Spieler) {
        spieler.einheiten.forEach {
            einheitUiErstellen(it)
        }
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

        if (spieler == spiel.mensch) {
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
            val kommando = EinheitenKommando.Angriff(ziel = einheit)
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
        gegner.einheiten.toList().forEach { maleEinheit(it) }
        mensch.einheiten.toList().forEach { maleEinheit(it) }
        kristalleText.text = "Kristalle: " + mensch.kristalle.toInt().toString()
        minenText.text = "Minen: " + mensch.minen.toString()

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
        if (kommando != null && kommando is EinheitenKommando.Angriff) {
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
            ).apply {
                startEinheiten(gegnerTyp)
            }

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
            ).apply {
                startEinheiten(spielerTyp)
            }

            spiel = Spiel(mensch, gegner, multiplayer = multiplayer)

            launch(App::class.java)
        }

        private fun spielerFarbe(spielerTyp: SpielerTyp) =
            if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer) Color.RED else Color.BLUE

        private fun startPunkt(spielerTyp: SpielerTyp) =
            if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer)
                Punkt(x = 900.0, y = 115.0) else Punkt(x = 900.0, y = 905.0)

        private fun Spieler.startEinheiten(spielerTyp: SpielerTyp) {
            val vorzeichen = if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer) -1 else 1

            neueEinheit(x = 1050.0, y = startpunkt.y, einheitenTyp = späher)
            neueEinheit(x = 750.0, y = startpunkt.y, einheitenTyp = arbeiter)
            neueEinheit(x = 850.0, y = startpunkt.y, einheitenTyp = infantrie)
            neueEinheit(x = 900.0, y = startpunkt.y + 60 * vorzeichen, einheitenTyp = basis)
            neueEinheit(x = 950.0, y = startpunkt.y, einheitenTyp = infantrie)
        }
    }
}

fun einheitenBild(): Circle {
    return Circle(20.toDouble())
}

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
