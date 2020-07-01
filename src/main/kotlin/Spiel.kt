@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis", "LiftReturnOrAssignment", "NonAsciiCharacters", "PropertyName", "EnumEntryName", "SpellCheckingInspection")

import javafx.scene.control.Button
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


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
        val kristalle: Int,
        val gebäude: Gebäude
)

val schmiede = TechGebäude(name = "Schmiede", kristalle = 2000, gebäude = kaserne)
val fusionskern = TechGebäude(name = "Fusionskern", kristalle = 3000, gebäude = raumhafen)
val akademie = TechGebäude(name = "Akademie", kristalle = 2500, gebäude = kaserne)
val reaktor = TechGebäude(name = "Reaktor", kristalle = 2000, gebäude = fabrik)
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
        val reichweite: Double,
        val leben: Double,
        var laufweite: Double,
        val kristalle: Int,
        val kuerzel: String,
        val panzerung: Double,
        val kannAngreifen: KannAngreifen,
        val luftBoden: LuftBoden = LuftBoden.boden,
        val gebäude: Gebäude?,
        val techGebäude: TechGebäude? = null,
        val name: String,
        val hotkey: String?,
        var button: Button? = null,
        var springen: Int? = null
)

data class Einheit(
        val spieler: Spieler,
        val typ: EinheitenTyp,
        val reichweite: Double,
        var leben: Double,
        var x: Double,
        var y: Double,
        val bild: Circle = einheitenBild(),
        var kuerzel: Text? = null,
        var lebenText: Text? = null,
        var ziel: Einheit? = null,
        var zielPunkt: Punkt? = null,
        var zielpunktkreis: Arc? = null,
        var zielpunktLinie: Line? = null,
        var panzerung: Double,
        var auswahlkreis: Arc? = null,
        var `springen cooldown`: Int = 0
) {
    fun punkt() = Punkt(x = x, y = y)
    override fun toString(): String {
        return "Einheit(spieler=$spieler, typ=${typ.kuerzel}, x=$x, y=$y)"
    }


}

data class Punkt(
        var x: Double,
        var y: Double
)


data class Spieler(
        val einheiten: MutableList<Einheit> = mutableListOf(),
        var kristalle: Double,
        var angriffspunkte: Int,
        var verteidiegungspunkte: Int,
        var minen: Int,
        var startpunkt: Punkt,
        val farbe: Color,
        val mensch: Boolean,
        val kristalleText: Text = Text(),
        val minenText: Text = Text(),
        var schadensUpgrade: Int,
        var panzerungsUprade: Int
){
    override fun toString(): String {
        return "Spieler(mensch=$mensch)"
    }
}


val infantrie = EinheitenTyp(name = "Infantrie", reichweite = 150.0, leben = 1000.0, schaden = 1.0, laufweite = 0.5, kristalle = 500, kuerzel = "INF", panzerung = 0.125,
        kannAngreifen = KannAngreifen.alles, gebäude = kaserne, hotkey = "q")
val eliteinfantrie = EinheitenTyp(name = "Elite-Infantrie", reichweite = 250.0, leben = 1200.0, schaden = 1.8, laufweite = 0.8, kristalle = 1400, kuerzel = "ELI", panzerung = 0.1,
        kannAngreifen = KannAngreifen.alles, gebäude = kaserne, techGebäude = akademie, hotkey = "w")
val berserker = EinheitenTyp(name = "Berserker", reichweite = 40.01, leben = 2000.0, schaden = 4.0, laufweite = 0.5, kristalle = 1000, kuerzel = "BER", panzerung = 0.25,
        kannAngreifen = KannAngreifen.boden, gebäude = kaserne, techGebäude = schmiede, hotkey = "e")
val flammenwerfer = EinheitenTyp(name = "Flammenwerfer", reichweite = 100.0, leben = 2600.0, schaden = 2.0, laufweite = 1.2, kristalle = 2200, kuerzel = "FLA", panzerung = 0.3,
        kannAngreifen = KannAngreifen.boden, gebäude = fabrik, hotkey = "s")
val panzer = EinheitenTyp(name = "Panzer", reichweite = 500.0, leben = 10000.0, schaden = 5.0, laufweite = 0.25, kristalle = 2500, kuerzel = "PAN", panzerung = 0.4,
        kannAngreifen = KannAngreifen.boden, gebäude = fabrik, techGebäude = reaktor, hotkey = "d")
val basis = EinheitenTyp(name = "Basis", reichweite = 500.0, leben = 30000.0, schaden = 12.0, laufweite = 0.0, kristalle = 0, kuerzel = "BAS", panzerung = 0.45,
        kannAngreifen = KannAngreifen.alles, gebäude = null, hotkey = null)
val jäger = EinheitenTyp(name = "Jäger", reichweite = 120.0, leben = 800.0, schaden = 3.0, laufweite = 0.8, kristalle = 1800, kuerzel = "JÄG", panzerung = 0.14,
        kannAngreifen = KannAngreifen.alles, luftBoden = LuftBoden.luft, gebäude = raumhafen, hotkey = "f")
val sanitäter = EinheitenTyp(name = "Sanitäter", reichweite = 40.01, leben = 800.0, schaden = 2.0, laufweite = 0.7, kristalle = 1100, kuerzel = "SAN", panzerung = 0.0,
        kannAngreifen = KannAngreifen.heilen, gebäude = kaserne, techGebäude = akademie, hotkey = "r")
val kampfschiff = EinheitenTyp(name = "Kampfschiff", reichweite = 250.0, leben = 20000.0, schaden = 9.0, laufweite = 0.2, kristalle = 3500, kuerzel = "KSF", panzerung = 0.5,
        kannAngreifen = KannAngreifen.alles, luftBoden = LuftBoden.luft, gebäude = raumhafen, techGebäude = fusionskern, hotkey = "g")
val arbeiter = EinheitenTyp(name = "Arbeiter", reichweite = 0.0, leben = 800.0, schaden = 0.0, laufweite = 0.7, kristalle = 1000, kuerzel = "ARB", panzerung = 0.0,
        kannAngreifen = KannAngreifen.alles, gebäude = null, hotkey = "y")
val späher = EinheitenTyp(name = "Späher", reichweite = 0.0, leben = 800.0, schaden = 0.0, laufweite = 0.7, kristalle = 200, kuerzel = "SPÄ", panzerung = 0.0,
        kannAngreifen = KannAngreifen.alles, gebäude = kaserne, hotkey = "x")
val kaufbareEinheiten = listOf(infantrie, eliteinfantrie, berserker, panzer, jäger, sanitäter, kampfschiff, flammenwerfer, arbeiter, späher)


class Spiel(
        val mensch: Spieler,
        val computer: Spieler,
        val rundenLimit: Int? = null,
        var runde: Int = 0,
        val warteZeit: Long = 15,
        var einheitProduziert: (Einheit) -> Unit = {}) {

    fun runde() {
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
        mensch.einheiten.forEach{
            if (it.`springen cooldown` > 0) {
                it.`springen cooldown`--
            }
        }
        computer.einheiten.forEach{
            if (it.`springen cooldown` > 0) {
                it.`springen cooldown`--
            }
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
            val einheit = spieler.einheit(
                    x = spieler.startpunkt.x,
                    y = spieler.startpunkt.y,
                    einheitenTyp = einheitenTyp)
            einheitProduziert(einheit)
        }
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { einheit -> bewege(einheit, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        val zielPunkt = zielpunktAuswaehlen(gegner, einheit)
        if (zielPunkt == null) {
            return
        }

        val a = zielPunkt.y - einheit.y
        val b = zielPunkt.x - einheit.x
        val e = entfernung(einheit, zielPunkt)

        val ziel = zielauswaehlenBewegen(gegner, einheit)
        if (e > einheit.reichweite) {
            val springen = einheit.typ.springen
            val mindestabstand = e - if (einheit.zielPunkt == null) 40 else 0
            val `max laufweite` = if (einheit.`springen cooldown` == 0 && einheit.zielPunkt == null && springen != null && e <= springen + 40) {
                ziel!!.leben -= einheit.typ.schaden * 15
                einheit.`springen cooldown` = 100
                springen.toDouble()
            } else {
                einheit.typ.laufweite
            }
            val laufweite = min(mindestabstand, `max laufweite`)

            einheit.x += smaller(b, b * laufweite / e)
            einheit.y += smaller(a, a * laufweite / e)
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
        val ziel = einheit.ziel
        if (ziel != null && !ziel.spieler.mensch) {
            return ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return heilen(gegner, einheit)
        }

        val naechsteEinheit = gegner.einheiten.minBy { entfernung(einheit, it) }
        //automatisch auf Einheiten in Reichweite schiessen
        if (naechsteEinheit != null) {
            if (`ist in Reichweite`(einheit, naechsteEinheit)) {
                return naechsteEinheit
            }
        }
        return ziel
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
                entfernung(einheit, verbündeter.punkt()) <= 300 &&
                kannAngreifen(einheit, gegner)
    }

    private fun `nächste Einheit zum Heilen`(gegner: Spieler, einheit: Einheit) =
            gegner(gegner).einheiten
                    .filter {
                        it.leben < it.typ.leben &&
                                entfernung(einheit, it) <= 300 &&
                                einheit != it
                    }
                    .minBy { entfernung(einheit, it) }

    fun gegner(spieler: Spieler): Spieler {
        if (spieler === mensch) {
            return computer
        }
        return mensch
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        if (entfernung(einheit, ziel) <= einheit.reichweite) {
            if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
                ziel.leben = min(ziel.leben + einheit.typ.schaden, ziel.typ.leben)
            } else {
                ziel.leben -= einheit.typ.schaden + spieler.schadensUpgrade / 10.0 - ziel.panzerung - gegner(spieler).panzerungsUprade / 10.0
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
        return entfernung(einheit, ziel) <= einheit.reichweite
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

}

fun kaufen(kristalle: Int, spieler: Spieler, aktion: () -> Unit) {
    if (spieler.kristalle >= kristalle) {
        aktion()
        spieler.kristalle -= kristalle
    }
}

fun Spieler.einheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) =
        Einheit(spieler = this,
                reichweite = einheitenTyp.reichweite,
                leben = einheitenTyp.leben,
                x = x,
                y = y,
                panzerung = einheitenTyp.panzerung,
                typ = einheitenTyp
        ).also { einheiten.add(it) }