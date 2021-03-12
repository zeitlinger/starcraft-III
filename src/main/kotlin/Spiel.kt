@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection", "FunctionName", "LocalVariableName", "FunctionName",
)

import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class Spiel(
    val mensch: Spieler,
    val gegner: Spieler,
    val rundenLimit: Int? = null,
    var runde: Int = 0,
    val warteZeit: Long = 15,
    val multiplayer: Multiplayer,
    var einheitProduziert: (Einheit) -> Unit = {},
    var kommandoEntfernt: () -> Unit = {}
) {
    private var started = mensch.spielerTyp == SpielerTyp.mensch

    fun spieler(typ: SpielerTyp): Spieler = if (typ == mensch.spielerTyp) mensch else gegner

    fun runde() {
        multiplayer.empfangeneKommandosVerarbeiten { kommando ->
            when (kommando) {
                is NeueEinheit ->
                    gegner.neueEinheit(
                        kommando.x,
                        kommando.y,
                        neutraleEinheitenTypen.getValue(kommando.einheitenTyp)
                    ).also { einheitProduziert(it) }
                is NeueKommandos ->
                    kommando.einheit.kommandoQueue.apply {
                        clear()
                        addAll(kommando.kommandos)
                    }
                ClientJoined -> started = true
                is NeueSpielerUpgrades -> gegner.upgrades = kommando.spielerUpgrades
                is NeueUpgrades -> gegner.einheitenTypen[kommando.einheitenTyp.name] = kommando.einheitenTyp
            }
        }
        if (!started) {
            if (mensch.spielerTyp == SpielerTyp.client) {
                multiplayer.sendeStartAnServer()
                started = true
            } else {
                //auf client warten
                return
            }
        }

        gegner.kristalle += 1.0 + 0.2 * gegner.minen
        mensch.kristalle += 1.0 + 0.2 * mensch.minen
        if (gegner.spielerTyp == SpielerTyp.computer) {
            produzieren(spieler = gegner, einheitenProduzierenKI())
        }
        bewegeSpieler(gegner, mensch)
        bewegeSpieler(mensch, gegner)

        schiessen(gegner, mensch)
        schiessen(mensch, gegner)

        gegner.einheiten.toList().forEach { einheitEntfernen(it, gegner, mensch) }
        mensch.einheiten.toList().forEach { einheitEntfernen(it, mensch, gegner) }

        if (gegner.einheiten.none { it.typ.name == basis.name }) {
            karte.add(Text("Sieg").apply {
                x = 700.0
                y = 500.0
                font = Font(200.0)
            })
        }
        if (mensch.einheiten.none { it.typ.name == basis.name }) {
            karte.add(Text("Niederlage").apply {
                x = 300.0
                y = 500.0
                font = Font(200.0)
            })
        }
        mensch.einheiten.forEach {
            rundenende(it)
        }
        gegner.einheiten.forEach {
            rundenende(it)
        }
    }

    private fun rundenende(it: Einheit) {
        if (erstesKommandoIst<EinheitenKommando.Stopp>(it)) {
            it.kommandoQueue.toList().forEach { kommando ->
                kommandoEntfernen(it, kommando)
            }
        }
        if (it.vergiftet > 0.0 && it.vergiftet.rem(1.0) == 0.0) {
            it.leben -= 5.0
        }
        if (it.wirdGeheilt > 0) {
            it.wirdGeheilt -= 1
        }
        if (it.`springen cooldown` > 0) {
            it.`springen cooldown` -= warteZeit.toDouble() / 1000.0
        }
        if (it.schusscooldown > 0) {
            it.schusscooldown -= warteZeit.toDouble() / 1000.0
        }
        it.heiler = null
        if (it.typ.typ == Typ.biologisch && it.leben < it.typ.leben && it.zuletztGetroffen >= 10) {
            it.leben = min(it.typ.leben, it.leben + 0.5)
        }
        it.zuletztGetroffen += warteZeit.toDouble() / 1000.0
        it.hatSichBewegt = false
        if (it.verlangsamt > 0) {
            it.verlangsamt -= warteZeit.toDouble() / 1000.0
        }
        if (it.vergiftet > 0) {
            it.vergiftet -= warteZeit.toDouble() / 1000.0
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

    fun produzieren(spieler: Spieler, neutraleTyp: EinheitenTyp) {
        val einheitenTyp = spieler.einheitenTypen.getValue(neutraleTyp.name)
        kaufen(einheitenTyp.kristalle, spieler) {
            neueEinheit(spieler, einheitenTyp)
        }
    }

    fun neueEinheit(spieler: Spieler, einheitenTyp: EinheitenTyp, punkt: Punkt = spieler.startpunkt) {
        val einheit = spieler.neueEinheit(
            x = punkt.x,
            y = punkt.y,
            einheitenTyp = einheitenTyp
        )
        multiplayer.neueEinheit(einheit.x, einheit.y, einheit)
        einheitProduziert(einheit)
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { einheit -> bewege(einheit, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        if (erstesKommandoIst<EinheitenKommando.HoldPosition>(einheit)) {
            return
        }
        val kommando = einheit.kommandoQueue.getOrNull(0)
        val laufweite = richtigeLaufweite(einheit)
        if (kommando is EinheitenKommando.Bewegen) {
            val zielPunkt = kommando.zielPunkt
            bewege(einheit, zielPunkt, laufweite)

            if (zielPunkt == einheit.punkt()) {
                kommandoEntfernen(einheit, kommando)
            }
            return
        }

        val ziel = zielauswaehlenBewegen(gegner, einheit)
        if (ziel == null) {
            if (kommando is EinheitenKommando.Attackmove) {
                bewege(einheit, kommando.zielPunkt, laufweite)
                if (kommando.zielPunkt == einheit.punkt()) {
                    kommandoEntfernen(einheit, kommando)
                }
            }
            return
        }

        val e = entfernung(einheit, ziel.punkt())

        if (e > einheit.typ.reichweite) {
            val springen = einheit.typ.springen
            val mindestabstand = e - 40
            val `max laufweite` = if (einheit.`springen cooldown` <= 0 && springen != null && e <= springen + 40) {
                ziel.leben -= einheit.typ.schaden * 3
                einheit.`springen cooldown` = 10.0
                springen.toDouble()
            } else {
                laufweite
            }

            bewege(einheit, ziel.punkt(), min(mindestabstand, `max laufweite`))
        }
    }

    private inline fun <reified T> erstesKommandoIst(einheit: Einheit) =
        einheit.kommandoQueue.size >= 1 && einheit.kommandoQueue[0] is T

    private fun richtigeLaufweite(einheit: Einheit): Double {
        val verlangsamerung = if (einheit.verlangsamt > 0) {
            2
        } else {
            1
        }
        return einheit.typ.laufweite / verlangsamerung
    }

    private fun bewege(einheit: Einheit, zielPunkt: Punkt, laufweite: Double) {
        val entfernung = entfernung(einheit, zielPunkt)
        if (entfernung == 0.0) {
            return
        }

        val y = zielPunkt.y - einheit.y
        val x = zielPunkt.x - einheit.x
        einheit.x += smaller(x, x * laufweite / entfernung)
        einheit.y += smaller(y, y * laufweite / entfernung)
        einheit.hatSichBewegt = true
        einheit.letzterAngriff = null
    }

    private fun zielauswaehlenSchießen(gegner: Spieler, einheit: Einheit): Einheit? {
        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando is EinheitenKommando.Angriff) {
            return kommando.ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return heilen(gegner, einheit)
        }

        val l = gegner.einheiten.filter { `ist in Reichweite`(einheit, it) }.sortedBy { angriffspriorität(einheit, it) }
        if (l.isNotEmpty()) {
            val p = angriffspriorität(einheit, l.first())
            return l.filter { angriffspriorität(einheit, it) == p }.minByOrNull { entfernung(einheit, it) }
            //automatisch auf Einheiten in Reichweite mit der höchsten Angriffspriorität schiessen
        }
        return null
    }

    private fun angriffspriorität(einheit: Einheit, ziel: Einheit): Int {
        if (kannAngreifen(ziel, einheit) && (ziel.typ.reichweite >= entfernung(
                einheit,
                ziel
            ) || (ziel.typ.reichweite < einheit.typ.reichweite && entfernung(einheit, ziel) >= einheit.typ.reichweite))
            && !ziel.typ.zivileEinheit
        ) {
            return 1
        }
        if (ziel.typ.zivileEinheit) {
            return 4
        }
        if (ziel.typ.typ == Typ.struktur) {
            return 5
        }
        val l = mutableListOf<Einheit>()
        einheit.spieler.einheiten.forEach {
            if (entfernung(einheit, it) <= 300) {
                l.add(it)
            }
        }
        l.forEach {
            if (kannAngreifen(ziel, it) && (ziel.typ.reichweite >= entfernung(
                    it,
                    ziel
                ) || (ziel.typ.reichweite < it.typ.reichweite && entfernung(
                    einheit,
                    ziel
                ) >= einheit.typ.reichweite) && !ziel.typ.zivileEinheit)
            ) {
                return 2
            }
        }
        return 3
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
        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando is EinheitenKommando.Angriff) {
            return kommando.ziel
        }

        if (einheit.spieler.spielerTyp == SpielerTyp.computer) {
            return zielAuswählenKI(einheit.spieler, gegner, einheit)
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return `nächste Einheit zum Heilen`(gegner, einheit)
        }

        if (einheit.typ.zivileEinheit) {
            return null
        }

        val gEinheiten = `verbündetem helfen`(gegner, einheit)
        if (gEinheiten != null) {
            return gEinheiten
        }
        return null
    }

    private fun `verbündetem helfen`(gegner: Spieler, einheit: Einheit): Einheit? {
        val l = mutableListOf<Einheit>()
        gegner(gegner).einheiten.forEach {
            if (entfernung(einheit, it) <= 300) {
                l.add(it)
            }
        }
        val liste = mutableListOf<Einheit>()
        l.forEach {
            gegner.einheiten.forEach { gEinheit ->
                if ((`ist in Reichweite`(gEinheit, it) || `ist in Reichweite`(it, gEinheit) || (!kannAngreifen(
                        gEinheit,
                        it
                    ) && entfernung(gEinheit, it) <= 300)) && kannAngreifen(einheit, gEinheit)
                ) {
                    liste.add(gEinheit)
                }
            }
        }

        return liste.minWithOrNull(compareBy({ angriffspriorität(einheit, it) }, { entfernung(einheit, it) }))

    }


    private fun `nächste Einheit zum Heilen`(gegner: Spieler, einheit: Einheit): Einheit? {
        val ziel = gegner(gegner).einheiten
            .filter {
                it.leben < it.typ.leben &&
                    entfernung(einheit, it) <= 300 &&
                    einheit != it &&
                    (it.heiler == null || it.heiler == einheit) &&
                        (it.typ.typ == Typ.biologisch || !gegner(gegner).upgrades.vertärkteHeilmittel) &&
                        (it.zuletztGetroffen > 1 || gegner(gegner).upgrades.strahlungsheilung)
            }
            .minByOrNull { entfernung(einheit, it) }
        if (ziel != null) {
            ziel.heiler = einheit
        }
        return ziel
    }

    fun gegner(spieler: Spieler): Spieler {
        if (spieler === mensch) {
            return gegner
        }
        return mensch
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        if (`ist in Reichweite`(einheit, ziel) && einheit.schusscooldown <= 0.0 && !einheit.hatSichBewegt) {
            if (einheit.firstShotCooldown <= 0.0) {
                einheit.schusscooldown = einheit.typ.schusscooldown
                if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
                    heilen(einheit, ziel, spieler)
                } else if (einheit.typ.flächenschaden == null) {
                    schadenAusteilen(einheit, ziel, spieler)
                } else {
                    val getroffeneEinheiten = gegner(spieler).einheiten.filter {
                        entfernung(it, ziel) <= einheit.typ.flächenschaden!!
                    }
                    getroffeneEinheiten.forEach {
                        schadenAusteilen(einheit, it, spieler)
                    }
                }
            } else if (einheit.letzterAngriff == ziel) {
                einheit.firstShotCooldown -= warteZeit.toDouble()
            } else {
                einheit.firstShotCooldown = einheit.typ.firstShotDeley
                einheit.letzterAngriff = ziel
            }
        }
    }

    private fun schadenAusteilen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        ziel.leben -= max(
            (einheit.typ.schaden + spieler.upgrades.schadensUpgrade / 10.0 - (max(
                ziel.panzerung + gegner(spieler).upgrades.panzerungsUprade / 10.0,
                0.0
            ))) * if (ziel.wirdGeheilt > 0 && gegner(
                    spieler
                ).upgrades.strahlungsheilung
            ) {
                0.7
            } else 1.0, 0.5
        )
        ziel.zuletztGetroffen = 0.0
        if (einheit.typ.machtZustand == MachtZustand.vergiftung && ziel.vergiftet <= 0) {
            ziel.vergiftet = 10.0
        } else if (einheit.typ.machtZustand == MachtZustand.langsamkeit && ziel.verlangsamt <= 0) {
            ziel.verlangsamt = 10.0
        }
    }

    private fun heilen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        if ((ziel.heiler == null || ziel.heiler == einheit) &&
            (ziel.typ.typ == Typ.biologisch || !spieler.upgrades.vertärkteHeilmittel) && ziel.leben < ziel.typ.leben &&
            (ziel.zuletztGetroffen > 1 || spieler.upgrades.strahlungsheilung)
        ) {
            ziel.leben = min(ziel.leben + einheit.typ.schaden, ziel.typ.leben)
            ziel.heiler = einheit
            if (spieler.upgrades.vertärkteHeilmittel) {
                ziel.vergiftet = 0.0
                ziel.verlangsamt = 0.0
            }
            ziel.wirdGeheilt = 2
        }
    }

    private fun einheitEntfernen(einheit: Einheit, spieler: Spieler, gegner: Spieler) {
        if (einheit.leben <= 0) {
            spieler.einheiten.remove(einheit)

            if (ausgewaehlt.contains(einheit)) {
                val kreis = einheit.auswahlkreis
                ausgewaehlt.remove(einheit)
                karte.remove(kreis)
            }

            gegner.einheiten.forEach { gegnerEinheit ->
                gegnerEinheit.kommandoQueue.toList().forEach {
                    if (it is EinheitenKommando.Angriff && it.ziel == einheit) {
                        kommandoEntfernen(gegnerEinheit, it)
                    }
                }
            }
            karte.remove(einheit.bild)
            karte.remove(einheit.lebenText)
            karte.remove(einheit.kuerzel)
            einheit.kommandoQueue.toList().forEach {
                kommandoEntfernen(einheit, it)
            }
        }
    }

    fun kommandoEntfernen(einheit: Einheit, kommando: EinheitenKommando) {
        kommandoAnzeigeEntfernen(kommando)
        einheit.kommandoQueue.remove(kommando)
        kommandoEntfernt()
    }
}

fun `ist in Reichweite`(einheit: Einheit, ziel: Einheit): Boolean {
    return entfernung(einheit, ziel) <= einheit.typ.reichweite
}

fun entfernung(einheit: Einheit, ziel: Einheit): Double {
    if (!kannAngreifen(einheit, ziel)) {
        return 7000000000000000000.0
    }

    return entfernung(einheit, ziel.punkt())
}

fun kannAngreifen(einheit: Einheit, ziel: Einheit) =
    !(ziel.typ.luftBoden == LuftBoden.luft && einheit.typ.kannAngreifen == KannAngreifen.boden)

fun entfernung(einheit: Einheit, ziel: Punkt): Double {
    val a = ziel.y - einheit.y
    val b = ziel.x - einheit.x

    return sqrt(a.pow(2) + b.pow(2))
}

fun kaufen(kristalle: Int, spieler: Spieler, aktion: () -> Unit) {
    if (spieler.kristalle >= kristalle) {
        aktion()
        spieler.kristalle -= kristalle
    }
}

fun Spieler.neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp, nummer: Int? = null): Einheit {
    val spielerTyp = einheitenTypen.getValue(einheitenTyp.name)
    return Einheit(
        spieler = this,
        leben = spielerTyp.leben,
        x = x,
        y = y,
        panzerung = spielerTyp.panzerung,
        typ = spielerTyp,
        nummer = nummer ?: einheitenNummer.getOrDefault(this.spielerTyp, 0)
            .also { einheitenNummer[this.spielerTyp] = it + 1 }
    ).also { einheiten.add(it) }
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

fun smaller(a: Double, b: Double): Double {
    if (a.absoluteValue <= b.absoluteValue) {
        return a
    }
    return b
}

//Bugs:
//wenn man holdposition mit shiftcomand ausführt, wird es sofort ausgeführt
//wenn man mit zwei Einheiten unterschiedliche Kommandos ausführt und dann beide auswählt und mit shift ein neues Kommando gibt, werden die alten kommandos nicht vollständig angezeigt
//Die Tests mit angreifen funktioieren nicht

//Features:
//Einheiten sollen von angriffen wegrennen wenn sie nicht zurück angreifen können
//Wenn eine Einheit ein automatisches Ziel hat und man mit shift ein anderes Ziel gibt soll das automatische Ziel zuerst ausgeführt werden
//patrollieren
//Chat
//Kriegsnebel
//Sichtweite für Einheiten
//Minnimap
//kontrollgruppen
//spells
//Konsole
//lebensanzeige(lebensbalken)
//Gebäude platzieren
//gebäude auswählen
//produktionszeit
//recourssen auf der Karte (wissenschafts-und produktionsressoursen)
//arbeiter und wissenschafter können ressoursen abbauen bzw. erforschen und zu außenposten bringen
//Einheitengröße anpassen
//Physik (Einheiten nicht übereinander)
//Wasser + Schiffe
//bessere Grafik mit 3D-Moddelen und Animationen
//sound
//Hintergrundmusik
//totorial
//verbessertes Multiplayer
//kampagne
//mehr Einheiten + Upgrades
//balance
//KI
//Punkte auf der Karte die von Spielern besetzt werden können (z.B. verlassene Minen die repariert werden können)
//programm-optiemierung

//Rassen:
//Silikoiden:
//Eine Ressource mehr als die anderen Rassen (silizium); high tech; teure, große, schnelle Einheiten;
//Punkte auf der Karte die nur für eine Rasse sichtbar sind (Siliziumvorkommen) die durch eine Rafinerie abbauen können; Einheiten fusionieren; viele Upgrades
//Terraner:
//Mechs, Infantrie, Panzer; “vanilla”; Heimatwelt-boni
//Psilons:
//psionische Einheiten; Templer; Helden-Einheiten; Mana für Zaubersprüche; Archiv um Zaubersprüche um für die Templer zu erlernen; XP für Einheiten;
//unerfahrene Einheiten können nur einfache; Entscheidungen über tech tree für Upgrades
//Alkari:
//Nur biologische Einheiten; Larven; Billige Einheiten; nur eine Ressource (biomasse); können statt Forschung spezialeinheiten bauen; genmutationen
