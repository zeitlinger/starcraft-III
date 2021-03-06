@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection"
)

import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.*


class Spiel(
    val mensch: Spieler,
    val computer: Spieler,
    val rundenLimit: Int? = null,
    var runde: Int = 0,
    val warteZeit: Long = 15,
    var einheitProduziert: (Einheit) -> Unit = {}
) {

    fun runde() {
        computer.kristalle += 1.0 + 0.2 * computer.minen
        mensch.kristalle += 1.0 + 0.2 * mensch.minen
        produzieren(spieler = computer, einheitenTyp = cBerserker)
        bewegeSpieler(computer, mensch)
        bewegeSpieler(mensch, computer)

        schiessen(computer, mensch)
        schiessen(mensch, computer)

        computer.einheiten.toList().forEach { einheitEntfernen(it, computer, mensch) }
        mensch.einheiten.toList().forEach { einheitEntfernen(it, mensch, computer) }

        if (computer.einheiten.none { it.typ == cBasis }) {
            box.children.add(Text("Sieg").apply {
                x = 700.0
                y = 500.0
                font = Font(200.0)
            })
        }
        if (mensch.einheiten.none { it.typ == mBasis }) {
            box.children.add(Text("Niderlage").apply {
                x = 300.0
                y = 500.0
                font = Font(200.0)
            })
        }
        mensch.einheiten.forEach {
            rundenende(it)
        }
        computer.einheiten.forEach {
            rundenende(it)
        }
    }

    private fun rundenende(it: Einheit) {
        if (it.kommandoQueue.firstOrNull { it is Kommando.Stopp } != null) {
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

    fun produzieren(spieler: Spieler, einheitenTyp: EinheitenTyp) {
        kaufen(einheitenTyp.kristalle, spieler) {
            neueEinheit(spieler, einheitenTyp)
        }
    }

    fun neueEinheit(spieler: Spieler, einheitenTyp: EinheitenTyp) {
        val einheit = spieler.einheit(
                x = spieler.startpunkt.x,
                y = spieler.startpunkt.y,
                einheitenTyp = einheitenTyp
        )
        einheitProduziert(einheit)
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { einheit -> bewege(einheit, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        if (einheit.kommandoQueue.firstOrNull { it is Kommando.HoldPosition } != null) {
            return
        }
        val kommando = einheit.kommandoQueue.getOrNull(0)
        val laufweite = richtigeLaufweite(einheit)
        if (kommando is Kommando.Bewegen) {
            val zielPunkt = kommando.zielPunkt
            bewege(einheit, zielPunkt, laufweite)

            if (zielPunkt == einheit.punkt()) {
                kommandoEntfernen(einheit, kommando)
            }
            return
        }

        val ziel = zielauswaehlenBewegen(gegner, einheit)
        if (ziel == null) {
            if (kommando is Kommando.Attackmove) {
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

    private fun richtigeLaufweite(einheit: Einheit): Double {
        val verlangsamerung = if (einheit.verlangsamt > 0) {
            2
        } else {
            1
        }
        val laufweite = einheit.typ.laufweite / verlangsamerung
        return laufweite
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
    }

    private fun zielauswaehlenSchießen(gegner: Spieler, einheit: Einheit): Einheit? {
        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando is Kommando.Angriff) {
            return kommando.ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return heilen(gegner, einheit)
        }

        val l = gegner.einheiten.filter { `ist in Reichweite`(einheit, it) }.sortedBy { angriffspriorität(einheit, it) }
        if (l.isNotEmpty()) {
            val p = angriffspriorität(einheit, l.first())
            val e = l.filter { angriffspriorität(einheit, it) == p }.minBy { entfernung(einheit, it) }
            return e
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
        if (kommando is Kommando.Angriff) {
            return kommando.ziel
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

        if (gegner.mensch) {
            return zielAuswählenKI(mensch, einheit)
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

        return liste.minWith(compareBy({ angriffspriorität(einheit, it) }, { entfernung(einheit, it) }))

    }


    private fun `nächste Einheit zum Heilen`(gegner: Spieler, einheit: Einheit): Einheit? {
        val ziel = gegner(gegner).einheiten
            .filter {
                it.leben < it.typ.leben &&
                        entfernung(einheit, it) <= 300 &&
                        einheit != it &&
                        (it.heiler == null || it.heiler == einheit) &&
                        (it.typ.typ == Typ.biologisch || !gegner(gegner).vertärkteHeilmittel) &&
                        (it.zuletztGetroffen > 1 || gegner(gegner).strahlungsheilung)
            }
            .minBy { entfernung(einheit, it) }
        if (ziel != null) {
            ziel.heiler = einheit
        }
        return ziel
    }

    fun gegner(spieler: Spieler): Spieler {
        if (spieler === mensch) {
            return computer
        }
        return mensch
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        if (entfernung(
                einheit,
                ziel
            ) <= einheit.typ.reichweite && einheit.schusscooldown <= 0.0 && !einheit.hatSichBewegt
        ) {
            einheit.schusscooldown = einheit.typ.schusscooldown
            if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
                if ((ziel.heiler == null || ziel.heiler == einheit) &&
                    (ziel.typ.typ == Typ.biologisch || !spieler.vertärkteHeilmittel) && ziel.leben < ziel.typ.leben &&
                    (ziel.zuletztGetroffen > 1 || spieler.strahlungsheilung)
                ) {
                    ziel.leben = min(ziel.leben + einheit.typ.schaden, ziel.typ.leben)
                    ziel.heiler = einheit
                    if (spieler.vertärkteHeilmittel) {
                        ziel.vergiftet = 0.0
                        ziel.verlangsamt = 0.0
                    }
                    ziel.wirdGeheilt = 2
                }
            } else if (einheit.typ.flächenschaden == null) {
                ziel.leben -= max(
                    (einheit.typ.schaden + spieler.schadensUpgrade / 10.0 - (max(
                        ziel.panzerung + gegner(spieler).panzerungsUprade / 10.0,
                        0.0
                    ))) * if (ziel.wirdGeheilt > 0 && gegner(
                            spieler
                        ).strahlungsheilung
                    ) 0.7 else 1.0, 0.5
                )
                ziel.zuletztGetroffen = 0.0
                if (einheit.typ.machtZustand == MachtZustand.vergiftung && ziel.vergiftet <= 0) {
                    ziel.vergiftet = 10.0
                } else if (einheit.typ.machtZustand == MachtZustand.langsamkeit && ziel.verlangsamt <= 0) {
                    ziel.verlangsamt = 10.0
                }
            } else {
                val getroffeneEinheiten = gegner(spieler).einheiten.filter {
                    entfernung(it, ziel) <= einheit.typ.flächenschaden!!
                }
                getroffeneEinheiten.forEach {
                    it.leben -= max(
                        (einheit.typ.schaden + spieler.schadensUpgrade / 10.0 - (max(
                            ziel.panzerung + gegner(spieler).panzerungsUprade / 10.0,
                            0.0
                        ))) * if (ziel.wirdGeheilt > 0 && gegner(
                                spieler
                            ).strahlungsheilung
                        ) 0.7 else 1.0, 0.5
                    )
                    it.zuletztGetroffen = 0.0
                    if (einheit.typ.machtZustand == MachtZustand.vergiftung && ziel.vergiftet <= 0) {
                        ziel.vergiftet = 10.0
                    } else if (einheit.typ.machtZustand == MachtZustand.langsamkeit && ziel.verlangsamt <= 0) {
                        ziel.verlangsamt = 10.0
                    }
                }
            }
        }
    }

    private fun einheitEntfernen(einheit: Einheit, spieler: Spieler, gegner: Spieler) {
        if (einheit.leben <= 0) {
            spieler.einheiten.remove(einheit)

            if (ausgewaehlt.contains(einheit)) {
                val kreis = einheit.auswahlkreis
                ausgewaehlt.remove(einheit)
                box.children.remove(kreis)
            }

            gegner.einheiten.forEach { gegnerEinheit ->
                gegnerEinheit.kommandoQueue.toList().forEach {
                    if (it is Kommando.Angriff && it.ziel == einheit) {
                        kommandoEntfernen(gegnerEinheit, it)
                    }
                }
            }
            box.children.remove(einheit.bild)
            box.children.remove(einheit.lebenText)
            box.children.remove(einheit.kuerzel)
            einheit.kommandoQueue.toList().forEach {
                kommandoEntfernen(einheit, it)
            }
        }
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

fun Spieler.einheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) =
    Einheit(
        spieler = this,
        leben = einheitenTyp.leben,
        x = x,
        y = y,
        panzerung = einheitenTyp.panzerung,
        typ = einheitenTyp
    ).also { einheiten.add(it) }

fun kommandoEntfernen(einheit: Einheit, kommando: Kommando) {
    if (kommando.zielpunktkreis != null) {
        box.children.remove(kommando.zielpunktkreis)
    }
    if (kommando.zielpunktLinie != null) {
        box.children.remove(kommando.zielpunktLinie)
    }
    einheit.kommandoQueue.remove(kommando)
}

fun smaller(a: Double, b: Double): Double {
    if (a.absoluteValue <= b.absoluteValue) {
        return a
    }
    return b
}

//Bugs:
//rechtsclick wird beim loslassen ausgeführt
//Wenn man eine Einheit als Ziel auswählt und dann mit Shift ein anderes Befehl gibt wird die zweite Zielpunktlinie nicht aktualisiert
//Man kann nicht als Shiftbedfehl eine Einheit als Ziel wählen, die sich bewegt
//wegpunkte werden auch angezeigt wenn die Einheit nicht ausgewählt ist

//Features:
//Wenn man Attackmove macht und dann auf eine Einheit klickt, soll die Einheit als Ziel ausgewählt werden
//patrollieren
//größere Karte
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
//Einheitengröße
//Physik (Einheiten nicht übereinander)
//Wasser + Schiffe
//bessere Grafik mit 3D-Moddelen und Animationen
//sound
//Hintergrundmusik
//totorial
//multiplayer
//kampagne
//mehr Einheiten + Upgrades
//balance
//KI
//Punkte auf der Karte die von Spielern besetzt werden können (z.B. verlassene Minen die repariert werden können)
//programm-optiemierung

//Rassen:
//Silikoiden: Eine Ressource mehr als die anderen Rassen (silizium); high tech; teure, große, schnelle Einheiten;
//            Punkte auf der Karte die nur für eine Rasse sichtbar sind (Siliziumvorkommen) die durch eine Rafinerie abbauen können; Einheiten fusionieren; viele Upgrades
//Terraner: Mechs, Infantrie, Panzer; “vanilla”; Heimatwelt-boni
//Psilons: psionische Einheiten; Templer; Helden-Einheiten; Mana für Zaubersprüche; Archiv um Zaubersprüche um für die Templer zu erlernen; XP für Einheiten;
//         unerfahrene Einheiten können nur einfache; Entscheidungen über tech tree für Upgrades
//Alkari: Nur biologische Einheiten; Larven; Billige Einheiten; nur eine Ressource (biomasse); können statt Forschung spezialeinheiten bauen; genmutationen
