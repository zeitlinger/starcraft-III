@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection", "FunctionName", "ClassName", "LocalVariableName", "ObjectPropertyName"
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

lateinit var karte: ObservableList<Node>

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

fun kreis(punkt: Punkt, radius: Double): Arc {
    return Arc().apply {
        radiusX = radius
        radiusY = radius
        fill = Color.TRANSPARENT
        stroke = Color.BLACK
        type = ArcType.OPEN
        length = 360.0
    }.apply {
        this.punkt = punkt
    }
}

var Arc.punkt: Punkt
    get() = Punkt(centerX, centerY)
    set(value) {
        centerX = value.x
        centerY = value.y
    }

sealed class KommandoWählen(val hotkey: String, val filter: (Punkt) -> Set<Einheit> = { ausgewaehlt }) {
    object Bewegen : KommandoWählen("b")
    object Attackmove : KommandoWählen("a")
    object Patrolieren : KommandoWählen("p")
    object Yamatokanone : KommandoWählen("y", { ziel ->
        setOfNotNull(
            ausgewaehlt
                .filter { it.typ.yamatokanone != null && it.`yamatokane cooldown` == 0.0 }
                .minByOrNull { entfernung(it, ziel) }
        )
    })
}

class GebäudePlazieren(val gebäudeTyp: GebäudeTyp) : KommandoWählen("")

val einheitenKommandoWählen = KommandoWählen::class.sealedSubclasses.mapNotNull { k -> k.objectInstance }

enum class Laufbefehl(val wählen: KommandoWählen) {
    Bewegen(KommandoWählen.Bewegen),
    Attackmove(KommandoWählen.Attackmove),
    Patrolieren(KommandoWählen.Patrolieren)
}

data class Gebäude(
    val einheit: Einheit,
    val buttons: List<Button>,
    val sammelpunkt: Arc,
    val produktionsQueue: MutableList<EinheitenTyp> = mutableListOf(),
    var produktionsZeit: ZeitInSec = 0.0
)

@Suppress("SpellCheckingInspection")
class App : Application() {
    lateinit var scene: Scene
    val gegner = spiel.gegner
    val mensch = spiel.mensch
    val kaufbareEinheiten = mensch.einheitenTypen.values
    var kommandoWählen: KommandoWählen? = null

    lateinit var buttonLeiste: ObservableList<Node>
    var sammelpunkt: Arc? = null
    var produktionsUpdate: (() -> Unit)? = null
    val kristalleText: Label = Label().apply { minWidth = 100.0 }
    val minenText: Label = Label().apply { minWidth = 100.0 }
    val kommandoAnzeige: Label = Label().apply { minWidth = 200.0 }


    fun button(name: String, aktion: (Button) -> Unit): Button = Button(name).apply {
        this.mausTaste(MouseButton.PRIMARY) {
            aktion(this)
        }
    }

    fun kaufButton(leiste: MutableList<Button>, name: String, kritalle: Int, aktion: (Button) -> Unit): Button =
        kaufButton(leiste, { name }, { kritalle }, aktion = aktion)

    fun kaufButton(
        leiste: MutableList<Button>,
        name: () -> String,
        kritalle: () -> Int,
        bezahlen: Boolean = true,
        aktion: (Button) -> Unit,
    ): Button =
        button(name()) {
            kaufen(kritalle(), mensch, bezahlen = bezahlen) {
                aktion(it)
                it.text = name()
            }
        }.apply {
            mensch.addKristallObserver {
                this.isDisable = it < kritalle()
            }
            leiste.add(this)
        }

    fun einmalKaufen(leiste: MutableList<Button>, name: String, kritalle: Int, aktion: () -> Unit): Button =
        kaufButton(leiste, name, kritalle) {
            aktion()
            entfernen(leiste, it)
        }

    private fun entfernen(leiste: MutableList<Button>, button: Button) {
        (button.parent as Pane).children.remove(button)
        leiste.remove(button)
    }

    override fun start(stage: Stage) {
        val kartenPane = Pane().apply {
            prefWidth = 3850.0
            prefHeight = 2950.0
        }
        karte = kartenPane.children

        stage.title = spiel.mensch.spielerTyp.name

        spiel.einheitProduziert = { einheitUiErstellen(it) }
        spiel.einheitEntfernt = { einheit ->
            karte.remove(einheit.bild)
            karte.remove(einheit.lebenText)
            karte.remove(einheit.kuerzel)
            einheit.auswahlkreis?.let { karte.remove(it) }
        }
        spiel.kommandoEntfernt = {
            zeigeKommands()
            kommandoAnzeigeEntfernen(it)
        }

        val hBox = HBox()
        val buttonLeiste = HBox().apply {
            minWidth = 300.0
        }
        this.buttonLeiste = buttonLeiste.children

        hBox.children.add(kristalleText)
        hBox.children.add(minenText)
        hBox.children.add(buttonLeiste)
        hBox.children.add(kommandoAnzeige)

        initSpieler(gegner)
        initSpieler(mensch)

        val vBox = VBox(10.0)

        scene = Scene(vBox, 1850.0, 1000.0)
        scene.fill = null

        stage.scene = scene

        stage.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (nurBasisAusgewählt()) {
                kaufbareEinheiten.singleOrNull { event.text == it.hotkey }?.button?.fire()
            } else {
                if (ausgewaehlt.size > 0 && ausgewaehlt.none { it.typ.einheitenArt == EinheitenArt.struktur }) {
                    auswahlHotkeys(scene, event.text, event.isShiftDown)
                }
            }
        }

        vBox.children.add(scrollPane(vBox, kartenPane))
        vBox.children.add(hBox)
        kartenPane.mausTaste(MouseButton.SECONDARY) {
            if (kommandoWählen != null) {
                scene.cursor = Cursor.DEFAULT
                kommandoWählen = null
            } else {
                ausgewaehlt.forEach { einheit ->
                    val gebäude = einheit.gebäude
                    if (gebäude != null) {
                        gebäude.sammelpunkt.punkt = it.punkt
                        zeigeSammelpunkt(gebäude)
                    } else {
                        laufBefehl(
                            einheit,
                            laufbefehl = Laufbefehl.Bewegen,
                            schiftcommand = it.isShiftDown,
                            punkt = it.punkt
                        )
                    }
                }
            }
        }

        var auswahlStart: Punkt? = null
        var auswahlRechteck: Rectangle? = null

        kartenPane.mausTaste(MouseButton.PRIMARY) { event ->
            val laufbefehl = Laufbefehl.values().singleOrNull { it.wählen == kommandoWählen }
            val wählen = kommandoWählen
            when {
                laufbefehl != null -> {
                    ausgewaehlt.forEach { einheit ->
                        laufBefehl(
                            einheit = einheit,
                            laufbefehl = laufbefehl,
                            schiftcommand = event.isShiftDown,
                            punkt = event.punkt
                        )
                    }
                }
                wählen is GebäudePlazieren -> {
                    val typ = wählen.gebäudeTyp
                    kaufen(typ.kristalle, mensch) {
                        plaziereGebäude(typ, event.punkt, mensch)
                    }
                    scene.cursor = Cursor.DEFAULT

                }
                else -> {
                    auswahlStart = Punkt(event.x, event.y)
                }
            }
            scene.cursor = Cursor.DEFAULT
            kommandoWählen = null
        }
        kartenPane.mausTaste(MouseButton.PRIMARY, MouseEvent.MOUSE_DRAGGED) {
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
                    karte.add(auswahlRechteck)
                }
            }
        }
        kartenPane.mausTaste(MouseButton.PRIMARY, MouseEvent.MOUSE_RELEASED) {
            auswahlRechteck?.let { r ->
                neueAuswahl {
                    mensch.einheiten.forEach {
                        if (it.bild.boundsInParent.intersects(r.boundsInParent)) {
                            auswählen(it)
                        }
                    }
                }

                karte.remove(r)
            }
            auswahlStart = null
            auswahlRechteck = null
        }

        stage.isFullScreen = true
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
                Thread.sleep((spiel.warteZeit * 1000).toLong())
            }
        }.start()
    }

    private fun nurBasisAusgewählt() = ausgewaehlt.size == 1 && ausgewaehlt.iterator().next().typ.name == basis.name

    private fun aktuelleButtons(
        buttons: List<Button>
    ) {
        buttonLeiste.clear()
        buttonLeiste.addAll(buttons)
    }

    private fun scrollPane(vBox: VBox, kartenPane: Pane): ScrollPane {
        val scroll = ScrollPane(kartenPane)
        scroll.isPannable = false
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vBox.addEventFilter(MouseEvent.MOUSE_MOVED) {
            val sensitivity = 1
            val h = (scroll.hmax - scroll.hmin) / 100
            if (it.x <= sensitivity) {
                scroll.hvalue -= h
            }
            if (it.x >= scroll.width - sensitivity) {
                scroll.hvalue += h
            }
            val v = (scroll.vmax - scroll.vmin) / 100
            if (it.y <= sensitivity) {
                scroll.vvalue -= v
            }
            if (it.y >= scroll.height - sensitivity && it.y <= scroll.height) {
                scroll.vvalue += v
            }
        }
        return scroll
    }

    private fun auswahlHotkeys(scene: Scene, text: String?, shift: Boolean) {
        val wählen = einheitenKommandoWählen.singleOrNull { it.hotkey == text }
        if (wählen != null) {
            if (wählen.filter(Punkt(0.0, 0.0)).isEmpty()) {
                return
            }
            kommandoWählen = wählen
            scene.cursor = Cursor.CROSSHAIR
            return
        } else {
            val lowercase = text?.toLowerCase()
            val k = kommandoHotKeys[lowercase]

            if (k != null) {
                ausgewaehlt.forEach {
                    neuesKommando(
                        einheit = it,
                        kommando = k(),
                        shift = shift
                    )
                }
            }
        }
    }

    private fun plaziereGebäude(gebäudeTyp: GebäudeTyp, punkt: Punkt, spieler: Spieler) {
        val buttons = mutableListOf<Button>()
        val gebäude = spiel.neuesGebäude(spieler, gebäudeTyp, buttons, punkt)

        if (gebäudeTyp == basis) {
            kaufButton(buttons, "Mine", 2000 + 400 * mensch.minen) {
                mensch.minen += 1
            }
            gebäudeTypen.filter { it != basis }.forEach { typ ->
                kaufButton(buttons, { typ.name }, { typ.kristalle }, bezahlen = false) {
                    kommandoWählen = GebäudePlazieren(typ)
                    scene.cursor = Cursor.HAND
                }
            }
        }

        techgebäude.filter { it.gebäudeTyp == gebäudeTyp }.forEach { techGebäude ->
            einmalKaufen(buttons, techGebäude.name, techGebäude.kristalle) {
                kaufbareEinheiten.filter { it.techGebäude == techGebäude }.forEach { it.button!!.isDisable = false }
            }
        }
        kaufbareEinheiten.filter { it.gebäudeTyp == gebäudeTyp }.forEach { typ ->
            val button = kaufButton(buttons, typ.name, typ.kristalle) {
                if (gebäude.produktionsQueue.isEmpty()) {
                    gebäude.produktionsZeit = typ.produktionsZeit
                }
                gebäude.produktionsQueue.add(typ)
                produktionsUpdate!!()
            }
            if (typ.techGebäude != null) {
                button.isDisable = true
            }
            typ.button = button
        }
        upgrades.filter { it.gebäudeTyp == gebäudeTyp }.forEach { upgrade ->
            kaufButton(buttons, { upgrade.name(mensch.upgrades) }, { upgrade.kritalle(mensch.upgrades) }) {
                var remove = false

                upgrade.eiheitenUpgrades.forEach { (neutralerTyp, aktion) ->
                    val value = mensch.einheitenTypen.getValue(neutralerTyp.name)
                    remove = remove || value.aktion()
                    spiel.multiplayer.upgrade(value)
                }

                remove = remove || upgrade.spielerUpgrade(mensch.upgrades)
                spiel.multiplayer.upgrade(mensch)

                if (remove) {
                    entfernen(buttons, it)
                }
            }
        }
    }

    private fun neueAuswahl(aktion: () -> Unit) {
        `auswahl löschen`()
        aktion()
        zeigeKommands()
        produktionsUpdate?.let {
            spiel.rundeVorbei.remove(it)
            produktionsUpdate = null
        }

        ausgewaehlt.singleOrNull()?.let { einheit ->
            löscheSammelpunkt()
            buttonLeiste.clear()

            val gebäude = einheit.gebäude
            if (gebäude != null) {
                buttonLeiste.clear()
                buttonLeiste.addAll(gebäude.buttons)

                produktionsUpdate = {
                    kommandoAnzeige.text = gebäude.produktionsQueue.joinToString { it.name } +
                        if (gebäude.produktionsZeit > 0) " ${gebäude.produktionsZeit}" else ""
                }.also {
                    it()
                    spiel.rundeVorbei.add(it)
                }

                zeigeSammelpunkt(gebäude)
            }

            einheit.kommandoQueue.forEachIndexed { index, kommando ->
                zielpunktUndKreisHinzufügen(einheit, kommando, einheit.kommandoQueue.getOrNull(index - 1))
            }
        }
    }

    private fun löscheSammelpunkt() {
        sammelpunkt?.let { karte.remove(it) }
    }

    private fun zeigeSammelpunkt(gebäude: Gebäude) {
        löscheSammelpunkt()
        karte.add(gebäude.sammelpunkt)
        sammelpunkt = gebäude.sammelpunkt
    }

    private fun `auswahl löschen`() {
        ausgewaehlt.forEach { einheit ->
            karte.remove(einheit.auswahlkreis)
            einheit.auswahlkreis = null
            einheit.kommandoQueue.forEach { kommando ->
                kommandoAnzeigeEntfernen(kommando)
            }
        }
        ausgewaehlt.clear()
    }

    private fun laufBefehl(einheit: Einheit, laufbefehl: Laufbefehl, schiftcommand: Boolean, punkt: Punkt) {
        val letzterPunkt = if (!schiftcommand || einheit.kommandoQueue.isEmpty()) {
            einheit.punkt
        } else {
            einheit.punkt
        }

        val kommando = when (laufbefehl) {
            Laufbefehl.Attackmove -> {
                Attackmove(zielPunkt = punkt)
            }
            Laufbefehl.Bewegen -> {
                Bewegen(punkt)
            }
            else -> {
                Patrolieren(letzterPunkt, punkt, punkt)
            }
        }
        neuesKommando(einheit, kommando, schiftcommand)
        zielpunktKreisUndLinieHinzufügen(kommando, einheit)
    }

    val MouseEvent.punkt: Punkt
        get() = Punkt(x, y)

    private fun neuesKommando(einheit: Einheit, kommando: EinheitenKommando, shift: Boolean) {
        einheit.kommandoQueue.toList().forEach {
            if (!shift || it is HoldPosition || it is Stopp) {
                spiel.kommandoEntfernen(einheit, it)
            }
        }
        spiel.neuesKommando(einheit, kommando)
        if (ausgewaehlt.singleOrNull() == einheit) {
            zeigeKommands()
        }
    }

    private fun zeigeKommands() {
        ausgewaehlt.singleOrNull()?.let { einheit ->
            kommandoAnzeige.text = einheit.kommandoQueue.map { it::class.simpleName }.joinToString(",")
        }
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
        kommando.zielpunktkreis = kreis(zielPunkt, radius = 5.0).apply {
            karte.add(this)
        }

        if (einheit.kommandoQueue.size < 2) {
            return
        }

        val startPunkt = kommandoPosition(letztesKommando, einheit)

        if (einheit.kommandoQueue.size == 2) {
            linieHinzufügen(einheit.kommandoQueue.first(), einheit.punkt, startPunkt)
        }

        linieHinzufügen(kommando, startPunkt, zielPunkt)
    }

    private fun linieHinzufügen(kommando: EinheitenKommando, startPunkt: Punkt, zielPunkt: Punkt) {
        kommando.zielpunktLinie = Line().apply {
            startX = startPunkt.x
            startY = startPunkt.y
            endX = zielPunkt.x
            endY = zielPunkt.y
            karte.add(this)
        }
    }

    private fun kommandoPosition(letztesKommando: EinheitenKommando?, einheit: Einheit) = when (letztesKommando) {
        null -> einheit.punkt
        is Angriff -> letztesKommando.ziel.punkt
        is Bewegen -> letztesKommando.zielPunkt
        is Attackmove -> letztesKommando.zielPunkt
        is Patrolieren -> letztesKommando.punkt2
        is HoldPosition -> einheit.punkt
        is Stopp -> einheit.punkt
        is Yamatokanone -> letztesKommando.ziel.punkt
    }

    private fun initSpieler(spieler: Spieler) {
        val spielerTyp = spieler.spielerTyp
        if (spieler == spiel.gegner && spielerTyp != SpielerTyp.computer) {
            //gegner schickt einheiten
            return
        }

        plaziereGebäude(basis, Punkt(900.0, y = spieler.startpunkt.y - 60 * nachVorne(spielerTyp)), spieler)

        fun neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) {
            spiel.neueEinheit(spieler, einheitenTyp, Punkt(x, y))
        }

        neueEinheit(x = 1050.0, y = spieler.startpunkt.y, einheitenTyp = späher)
        neueEinheit(x = 750.0, y = spieler.startpunkt.y, einheitenTyp = arbeiter)
        neueEinheit(x = 850.0, y = spieler.startpunkt.y, einheitenTyp = infantrie)
        neueEinheit(x = 950.0, y = spieler.startpunkt.y, einheitenTyp = infantrie)
    }

    private fun einheitUiErstellen(einheit: Einheit) {
        val spieler = einheit.spieler

        einheit.kuerzel = Text(einheit.typ.kuerzel)
        einheit.lebenText = Text()
        einheit.bild.fill = spieler.farbe
        karte.add(einheit.bild)
        karte.add(einheit.lebenText)
        karte.add(einheit.kuerzel)

        einheitMouseHandler(spieler, einheit.bild, einheit)
        einheitMouseHandler(spieler, einheit.kuerzel!!, einheit)
    }

    private fun einheitMouseHandler(spieler: Spieler, imageView: Node, einheit: Einheit) {
        imageView.mausTaste(MouseButton.PRIMARY, filter = { kommandoWählen == KommandoWählen.Attackmove }) {
            `angriffsziel auswählen`(einheit, schiftcommand = it.isShiftDown)
            scene.cursor = Cursor.DEFAULT
            kommandoWählen = null
        }

        imageView.mausTaste(MouseButton.PRIMARY, filter = { kommandoWählen == KommandoWählen.Yamatokanone }) {
            val angreifer = kommandoWählen!!.filter(einheit.punkt).singleOrNull()
            if (angreifer != null) {
                kommandoMitZielpunktKreis(angreifer, Yamatokanone(einheit), it.isShiftDown)
            }
        }

        if (spieler == spiel.mensch) {
            imageView.mausTaste(MouseButton.PRIMARY, filter = { kommandoWählen == null }) {
                neueAuswahl {
                    auswählen(einheit)
                }
            }
        }

        imageView.mausTaste(MouseButton.SECONDARY, filter = { kommandoWählen == null }) {
            `angriffsziel auswählen`(einheit, schiftcommand = it.isShiftDown)
        }
    }

    private fun `angriffsziel auswählen`(ziel: Einheit, schiftcommand: Boolean) {
        ausgewaehlt.forEach {
            kommandoMitZielpunktKreis(it, Angriff(ziel = ziel), schiftcommand)
        }
    }

    private fun kommandoMitZielpunktKreis(it: Einheit, kommando: EinheitenKommando, schiftcommand: Boolean) {
        neuesKommando(einheit = it, kommando = kommando, shift = schiftcommand)
        zielpunktKreisUndLinieHinzufügen(kommando, it)
        scene.cursor = Cursor.DEFAULT
        kommandoWählen = null
    }

    private fun auswählen(einheit: Einheit) {
        if (einheit.leben > 0 && einheit.auswahlkreis == null) {
            val auswahlKreis: Arc = kreis(Punkt(-100.0, -100.0), radius = 25.0)
            karte.add(auswahlKreis)
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

        kreis.layoutX = einheit.punkt.x
        kreis.layoutY = einheit.punkt.y

        val lebenText = einheit.lebenText!!
        lebenText.x = einheit.punkt.x - 10
        lebenText.y = einheit.punkt.y - 30
        lebenText.text = einheit.leben.toInt().toString()

        einheit.kuerzel!!.x = einheit.punkt.x - 12
        einheit.kuerzel!!.y = einheit.punkt.y


        val queue = einheit.kommandoQueue
        if (queue.size >= 1) {
            queue[0].zielpunktLinie?.apply {
                startX = einheit.punkt.x
                startY = einheit.punkt.y
            }
        }

        queue.forEachIndexed { index, kommando ->
            if (kommando is Angriff) {
                val ziel = kommando.ziel

                kommando.zielpunktLinie?.apply {
                    endX = ziel.punkt.x
                    endY = ziel.punkt.y
                }
                if (index < queue.size - 1) {
                    queue[index + 1].zielpunktLinie?.apply {
                        startX = ziel.punkt.x
                        startY = ziel.punkt.y
                    }
                }
                kommando.zielpunktkreis?.apply {
                    punkt = ziel.punkt
                }
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
            )

            val spielerTyp = multiplayer.spielerTyp
            val mensch = Spieler(
                kristalle = 30000.0,
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

            spiel = Spiel(mensch, gegner, multiplayer = multiplayer)

            launch(App::class.java)
        }

        private fun spielerFarbe(spielerTyp: SpielerTyp) =
            if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer) Color.RED else Color.BLUE

        private fun startPunkt(spielerTyp: SpielerTyp) =
            if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer)
                Punkt(x = 900.0, y = 115.0) else Punkt(x = 900.0, y = 905.0)


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

fun kommandoAnzeigeEntfernen(kommando: EinheitenKommando) {
    if (kommando.zielpunktLinie != null) {
        karte.remove(kommando.zielpunktLinie)
        kommando.zielpunktLinie = null
    }
    if (kommando.zielpunktkreis != null) {
        karte.remove(kommando.zielpunktkreis)
        kommando.zielpunktkreis = null
    }
}

