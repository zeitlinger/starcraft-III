@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "FoldInitializerAndIfToElvis",
    "LiftReturnOrAssignment",
    "NonAsciiCharacters",
    "PropertyName",
    "EnumEntryName",
    "SpellCheckingInspection", "FunctionName", "LocalVariableName", "FunctionName",
)

import javafx.scene.control.Button
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class Spiel(
    val mensch: Spieler,
    val gegner: Spieler,
    val rundenLimit: Int? = null,
    var runde: Int = 0,
    val warteZeit: ZeitInSec = 0.015,
    val multiplayer: Multiplayer,
    var einheitProduziert: (Einheit) -> Unit = {},
    var einheitEntfernt: (Einheit) -> Unit = {},
    var kommandoEntfernt: (EinheitenKommando) -> Unit = {},
    val rundeVorbei: MutableList<() -> Unit> = mutableListOf()
) {
    private var started = mensch.spielerTyp == SpielerTyp.mensch

    fun spieler(typ: SpielerTyp): Spieler = if (typ == mensch.spielerTyp) mensch else gegner

    fun runde() {
        multiplayer.empfangeneKommandosVerarbeiten(this::verarbeiteMultiplayerKommando)
        if (!started) {
            if (mensch.spielerTyp == SpielerTyp.client) {
                multiplayer.sendeStartAnServer()
            }
            //auf client oder server warten
            return
        }

        gegner.kristalle += 1.0 + 0.2 * gegner.minen
        mensch.kristalle += 1.0 + 0.2 * mensch.minen
        if (gegner.spielerTyp == SpielerTyp.computer) {
            val produzieren = produzierenKI(gegner, mensch)
            if (produzieren.first != null) {
                computerProduziert(gegner, produzieren.first!!)
            } else if (produzieren.first == null && produzieren.second == null) {
                kaufen(2000 + gegner.minen * 400, gegner) {
                    gegner.minen += 1
                }
            } else {
                kaufen(produzieren.second!!.kristalle, gegner) {
                    neuesGebäude(gegner, produzieren.second!!, emptyList(), gegner.startpunkt)
                }
            }
        }

        mensch.gebäude.values.filter { it.produktionsQueue.isNotEmpty() }.forEach { gebäude ->
            if (gebäude.produktionsZeit <= 0) {
                val typ = gebäude.produktionsQueue.removeAt(0)
                val einheit = neueEinheit(mensch, typ, gebäude.einheit.punkt)
                neuesKommando(einheit, Bewegen(gebäude.sammelpunkt.punkt))

                gebäude.produktionsQueue.getOrNull(0)?.let {
                    gebäude.produktionsZeit = it.produktionsZeit
                }
            } else {
                gebäude.produktionsZeit -= warteZeit
            }
        }

        bewegeSpieler(gegner, mensch)
        bewegeSpieler(mensch, gegner)

        spells(gegner)
        spells(mensch)

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

        rundeVorbei.forEach { it() }
    }

    fun neuesKommando(einheit: Einheit, kommando: EinheitenKommando) {
        einheit.kommandoQueue.add(kommando)
        multiplayer.neueKommandos(einheit)
    }

    private fun verarbeiteMultiplayerKommando(kommando: MultiplayerKommando) {
        when (kommando) {
            is NeueEinheit ->
                gegner.neueEinheit(
                    kommando.punkt,
                    neutraleEinheitenTypen.getValue(kommando.einheitenTyp)
                ).also { einheitProduziert(it) }
            is NeueKommandos ->
                kommando.einheit.kommandoQueue.apply {
                    clear()
                    addAll(kommando.kommandos)
                }
            ClientJoined -> {
                started = true
                multiplayer.sendeStartAnClient()
            }
            ServerJoined -> started = true
            is NeueSpielerUpgrades -> gegner.upgrades = kommando.spielerUpgrades
            is NeueUpgrades -> gegner.einheitenTypen[kommando.einheitenTyp.name] = kommando.einheitenTyp
        }
    }

    private fun rundenende(it: Einheit) {
        if (erstesKommandoIst<Stopp>(it)) {
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
            it.`springen cooldown` -= warteZeit
        }
        if (it.`yamatokane cooldown` > 0) {
            it.`yamatokane cooldown` -= warteZeit
        }
        if (it.schusscooldown > 0) {
            it.schusscooldown -= warteZeit
        }
        it.heiler = null
        if (it.typ.einheitenArt == EinheitenArt.biologisch && it.leben < it.typ.leben && it.zuletztGetroffen >= 10) {
            it.leben = min(it.typ.leben, it.leben + 0.5)
        }
        it.zuletztGetroffen += warteZeit
        it.hatSichBewegt = false
        if (it.verlangsamt > 0) {
            it.verlangsamt -= warteZeit
        }
        if (it.vergiftet > 0) {
            it.vergiftet -= warteZeit
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

    private fun spells(spieler: Spieler) {
        spieler.einheiten.forEach {
            val kommando = it.kommandoQueue.getOrNull(0)
            if (kommando is Yamatokanone) {
                spellsAusführen(it, kommando.ziel)
            }
        }
    }

    private fun spellsAusführen(einheit: Einheit, ziel: Einheit) {
        if (einheit.typ.yamatokanone!! >= entfernung(
                einheit,
                ziel
            ) && !einheit.hatSichBewegt && einheit.`yamatokane cooldown` <= 0.0
        ) {
            ziel.leben -= 800
            val kommando = einheit.kommandoQueue[0]
            kommandoEntfernen(einheit, kommando)
            einheit.`yamatokane cooldown` = 5.0
        }
    }

    fun computerProduziert(spieler: Spieler, neutraleTyp: EinheitenTyp) {
        val einheitenTyp = spieler.einheitenTypen.getValue(neutraleTyp.name)
        kaufen(einheitenTyp.kristalle, spieler) {
            neueEinheit(spieler, einheitenTyp)
        }
    }

    fun neueEinheit(spieler: Spieler, einheitenTyp: EinheitenTyp, punkt: Punkt = spieler.startpunkt): Einheit =
        spieler.neueEinheit(
            punkt,
            einheitenTyp = einheitenTyp
        ).also {
            multiplayer.neueEinheit(it.punkt, it)
            einheitProduziert(it)
        }

    fun neuesGebäude(spieler: Spieler, gebäudeTyp: GebäudeTyp, buttons: List<Button>, punkt: Punkt): Gebäude {
        val einheit = neueEinheit(spieler, spieler.gebäudeEinheitenTyp(gebäudeTyp), punkt)
        val sammelpunkt = punkt.copy(y = punkt.y + 70 * nachVorne(spieler.spielerTyp))

        val gebäude = Gebäude(gebäudeTyp, einheit, buttons, kreis(sammelpunkt, radius = 5.0))
        spieler.gebäude[einheit.nummer] = gebäude
        return gebäude
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { einheit -> bewege(einheit, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        if (erstesKommandoIst<HoldPosition>(einheit)) {
            return
        }
        val kommando = einheit.kommandoQueue.getOrNull(0)
        val laufweite = richtigeLaufweite(einheit)
        if (kommando is Bewegen) {
            val zielPunkt = kommando.zielPunkt
            bewege(einheit, zielPunkt, laufweite)

            if (zielPunkt == einheit.punkt) {
                kommandoEntfernen(einheit, kommando)
            }
            return
        }

        val ziel = zielauswählenBewegen(gegner, einheit)
        if (ziel == null) {
            if (gegner.spielerTyp == SpielerTyp.mensch && zielAuswählenKI(gegner(gegner), einheit, gegner) != null) {
                bewege(einheit, zielAuswählenKI(gegner(gegner), einheit, gegner)!!, laufweite)
            }
            if (kommando is Attackmove) {
                bewege(einheit, kommando.zielPunkt, laufweite)
                if (kommando.zielPunkt == einheit.punkt) {
                    kommandoEntfernen(einheit, kommando)
                }
            }
            if (kommando is Patroullieren) {
                bewege(einheit, kommando.nächsterPunkt, laufweite)
                if (kommando.nächsterPunkt == einheit.punkt) {
                    if (einheit.kommandoQueue.size > 1) {
                        kommandoEntfernen(einheit, kommando)
                    } else {
                        val nächsterPunktNummer = kommando.nächsterPunktNumer + 1
                        val letzterPunktNummer = kommando.nächsterPunktNumer - 1
                        if (kommando.vorwärtsGehen) {
                            kommando.nächsterPunkt = if (nächsterPunktNummer < kommando.punkte.size) {
                                kommando.nächsterPunktNumer += 1
                                kommando.punkte[nächsterPunktNummer]
                            } else {
                                if (kommando.imKreisGehen) {
                                    kommando.nächsterPunktNumer = 0
                                    kommando.punkte[0]
                                } else {
                                    kommando.vorwärtsGehen = false
                                    kommando.nächsterPunktNumer -= 1
                                    kommando.punkte[letzterPunktNummer]
                                }
                            }
                        } else {
                            kommando.nächsterPunkt = if (letzterPunktNummer >= 0) {
                                kommando.nächsterPunktNumer -= 1
                                kommando.punkte[letzterPunktNummer]
                            } else {
                                if (kommando.imKreisGehen) {
                                    kommando.nächsterPunktNumer = kommando.punkte.size
                                    kommando.punkte.last()
                                } else {
                                    kommando.vorwärtsGehen = true
                                    kommando.nächsterPunktNumer += 1
                                    kommando.punkte[nächsterPunktNummer]
                                }
                            }
                        }
                    }
                }
            }
            if (kommando == null) {
                val wegrennen = wegrennen(gegner, einheit)
                if (wegrennen != null) {
                    bewege(einheit, wegrennen, laufweite)
                }
            }
            return
        }

        val e = entfernung(einheit, ziel)

        val reichweite = when (kommando) {
            is Yamatokanone -> einheit.typ.yamatokanone!!.toDouble()
            else -> einheit.typ.reichweite
        }
        if (e > reichweite) {
            val springen = einheit.typ.springen
            val mindestabstand = e - 40
            val `max laufweite` = if (einheit.`springen cooldown` <= 0 && springen != null && e <= springen + 40) {
                ziel.leben -= einheit.typ.schaden * 3
                einheit.`springen cooldown` = 10.0
                springen.toDouble()
            } else {
                laufweite
            }

            bewege(einheit, ziel.punkt, min(mindestabstand, `max laufweite`))
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

        val punkt = einheit.punkt
        val y = zielPunkt.y - punkt.y
        val x = zielPunkt.x - punkt.x
        einheit.punkt = Punkt(
            punkt.x + smaller(x, x * laufweite / entfernung),
            punkt.y + smaller(y, y * laufweite / entfernung),
        )
        einheit.hatSichBewegt = true
        einheit.letzterAngriff = null
    }

    private fun zielauswaehlenSchießen(gegner: Spieler, einheit: Einheit): Einheit? {
        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando is Angriff) {
            return kommando.ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return heilen(einheit)
        }

        return bestesZiel(gegner.einheiten.filter { `ist in Reichweite`(einheit, it) }, einheit)
    }

    private fun angriffspriorität(einheit: Einheit, ziel: Einheit): Int {
        if (kannAngreifen(ziel, einheit) && kannBedrohungGefahrlosAngreifen(einheit, ziel) && !ziel.typ.zivileEinheit) {
            return 1
        }
        if (ziel.typ.zivileEinheit) {
            return 4
        }
        if (ziel.typ.einheitenArt == EinheitenArt.struktur) {
            return 5
        }
        val l = mutableListOf<Einheit>()
        einheit.spieler.einheiten.forEach {
            if (entfernung(einheit, it) <= 300) {
                l.add(it)
            }
        }
        l.forEach {
            if (kannAngreifen(ziel, it) && (`ist in Reichweite`(ziel, it) || (ziel.typ.reichweite < it.typ.reichweite && `ist in Reichweite`(it, ziel)) && !ziel.typ.zivileEinheit)
            ) {
                return 2
            }
        }
        return 3
    }

    private fun kannBedrohungGefahrlosAngreifen(einheit: Einheit, ziel: Einheit) =
        `ist in Reichweite`(ziel, einheit) || (ziel.typ.reichweite < einheit.typ.reichweite && `ist in Reichweite`(
            einheit,
            ziel
        ))

    private fun heilen(einheit: Einheit): Einheit? {
        val naechsteEinheit = `nächste Einheit zum Heilen`(einheit)

        if (naechsteEinheit != null) {
            if (`ist in Reichweite`(einheit, naechsteEinheit)) {
                return naechsteEinheit
            }
        }

        return null
    }

    private fun zielauswählenBewegen(gegner: Spieler, einheit: Einheit): Einheit? {
        val kommando = einheit.kommandoQueue.getOrNull(0)
        if (kommando is Angriff) {
            return kommando.ziel
        }

        if (kommando is Yamatokanone) {
            return kommando.ziel
        }

        if (einheit.typ.kannAngreifen == KannAngreifen.heilen) {
            return `nächste Einheit zum Heilen`(einheit)
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
        einheit.spieler.einheiten.forEach {
            if (entfernung(einheit, it) <= 300) {
                l.add(it)
            }
        }
        val liste = mutableListOf<Einheit>()
        l.forEach {
            gegner.einheiten.forEach { gEinheit ->
                if ((`ist in Reichweite`(gEinheit, it) || entfernung(gEinheit.punkt, it.punkt) <= 300) &&
                    kannAngreifen(einheit, gEinheit)) {
                    liste.add(gEinheit)
                }
            }
        }
        return bestesZiel(liste, einheit)
    }

    private fun bestesZiel(liste: Collection<Einheit>, einheit: Einheit) =
        liste.minWithOrNull(
            compareBy(
                { angriffspriorität(einheit, it) },
                { if (einheit.letzterAngriff == it && einheit.firstShotCooldown <= einheit.typ.firstShotDelay) 0 else 1 },
                { entfernung(einheit, it) })
        )

    private fun wegrennen(gegner: Spieler, einheit: Einheit): Punkt? {
        val liste = mutableListOf<Einheit>()
        gegner.einheiten.forEach {
            if ((`ist in Reichweite`(it, einheit) && kannAngreifen(it, einheit) && !kannAngreifen(einheit, it)) || (it.firstShotCooldown <= 0 && it.letzterAngriff == einheit) && !kannSehen(einheit, it)) {
                liste.add(it)
            }
        }
        if (liste.isNotEmpty()) {
                gegenüberliegendenPunktFinden(einheit.punkt ,einheitenMittelpunkt(liste), 100.0)
        }
        return null
    }


    private fun `nächste Einheit zum Heilen`(einheit: Einheit): Einheit? {
        val spieler = einheit.spieler
        val ziel = spieler.einheiten
            .filter {
                it.leben < it.typ.leben &&
                        entfernung(einheit, it) <= 300 &&
                        einheit != it &&
                        (it.heiler == null || it.heiler == einheit) &&
                        (it.typ.einheitenArt == EinheitenArt.biologisch || !spieler.upgrades.vertärkteHeilmittel) &&
                        (it.zuletztGetroffen > 1 || spieler.upgrades.strahlungsheilung)
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
            when {
                einheit.firstShotCooldown <= 0.0 -> {
                    einheit.schusscooldown = einheit.typ.schusscooldown
                    when {
                        einheit.typ.kannAngreifen == KannAngreifen.heilen -> {
                            heilen(einheit, ziel, spieler)
                        }
                        einheit.typ.flächenschaden == null -> {
                            schadenAusteilen(einheit, ziel, spieler)
                        }
                        else -> {
                            val getroffeneEinheiten = gegner(spieler).einheiten.filter {
                                entfernung(it, ziel) <= einheit.typ.flächenschaden!!
                            }
                            getroffeneEinheiten.forEach {
                                schadenAusteilen(einheit, it, spieler)
                            }
                        }
                    }
                }
                einheit.letzterAngriff == ziel -> {
                    einheit.firstShotCooldown -= warteZeit
                }
                else -> {
                    einheit.firstShotCooldown = einheit.typ.firstShotDelay
                    einheit.letzterAngriff = ziel
                }
            }
        }
    }

    private fun schadenAusteilen(einheit: Einheit, ziel: Einheit, spieler: Spieler) {
        ziel.leben -= max(
            (einheit.typ.schaden + spieler.upgrades.schadensUpgrade - (max(
                ziel.panzerung + gegner(spieler).upgrades.panzerungsUprade - einheit.typ.durchschlag,
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
            (ziel.typ.einheitenArt == EinheitenArt.biologisch || !spieler.upgrades.vertärkteHeilmittel) && ziel.leben < ziel.typ.leben &&
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
                ausgewaehlt.remove(einheit)
            }

            gegner.einheiten.forEach { gegnerEinheit ->
                gegnerEinheit.kommandoQueue.toList().forEach {
                    if (it is Angriff && it.ziel == einheit) {
                        kommandoEntfernen(gegnerEinheit, it)
                    }
                    if (it is Yamatokanone && it.ziel == einheit) {
                        kommandoEntfernen(gegnerEinheit, it)
                    }
                }
            }
            einheit.kommandoQueue.toList().forEach {
                kommandoEntfernen(einheit, it)
            }
            einheitEntfernt(einheit)
        }
    }

    fun kommandoEntfernen(einheit: Einheit, kommando: EinheitenKommando) {
        einheit.kommandoQueue.remove(kommando)
        kommandoEntfernt(kommando)
    }
}

fun `ist in Reichweite`(einheit: Einheit, ziel: Einheit): Boolean {
    return entfernung(einheit, ziel) <= einheit.typ.reichweite
}

fun einheitenMittelpunkt(einheiten: List<Einheit>): Punkt {
    var x = 0.0
    var y = 0.0
    val anzahl = einheiten.size
    einheiten.forEach {
        x += it.punkt.x
        y += it.punkt.y
    }
    x /= anzahl
    y /= anzahl
    return Punkt(x, y)
}


fun gegenüberliegendenPunktFinden(punkt1: Punkt, punkt2: Punkt, länge: Double): Punkt {
    val c = (punkt1.x - punkt2.x)
    val d = (punkt1.y - punkt2.y)
    val b = sqrt(länge * länge / (c * c / d * d + 1))
    val a = c / d * b
    return Punkt(punkt1.x + a * c.sign, punkt1.y + b * d.sign)
}

fun entfernung(einheit: Einheit, ziel: Einheit): Double {
    when {
        einheit.typ.kannAngreifen == KannAngreifen.heilen -> if (einheit.spieler != ziel.spieler) {
            return Double.MAX_VALUE
        }
        !kannAngreifen(einheit, ziel) -> return Double.MAX_VALUE
    }
    return entfernung(einheit, ziel.punkt)
}

fun kannAngreifen(einheit: Einheit, ziel: Einheit) =
    kannLuftOderBodenErreichen(ziel, einheit) &&
        !einheit.typ.zivileEinheit &&
        einheit.typ.einheitenArt != EinheitenArt.struktur

private fun kannLuftOderBodenErreichen(ziel: Einheit, einheit: Einheit) =
    !(ziel.typ.luftBoden == LuftBoden.luft && einheit.typ.kannAngreifen == KannAngreifen.boden)

fun kannSehen(einheit: Einheit, ziel: Einheit) =
    true

fun entfernung(einheit: Einheit, ziel: Punkt): Double {
    val a = ziel.y - einheit.punkt.y
    val b = ziel.x - einheit.punkt.x

    return sqrt(a.pow(2) + b.pow(2))
}

fun entfernung(punkt1: Punkt, punkt2: Punkt): Double {
    val a = punkt1.y - punkt2.y
    val b = punkt1.x - punkt2.x

    return sqrt(a.pow(2) + b.pow(2))
}

fun kaufen(kristalle: Int, spieler: Spieler, bezahlen: Boolean = true, aktion: () -> Unit) {
    if (spieler.kristalle >= kristalle) {
        aktion()
        if (bezahlen) {
            spieler.kristalle -= kristalle
        }
    }
}

fun Spieler.neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp, nummer: Int? = null): Einheit {
    return neueEinheit(Punkt(x, y), einheitenTyp, nummer)
}

fun Spieler.neueEinheit(punkt: Punkt, einheitenTyp: EinheitenTyp, nummer: Int? = null): Einheit {
    val spielerTyp = einheitenTypen.getValue(einheitenTyp.name)
    return Einheit(
        spieler = this,
        leben = spielerTyp.leben,
        punkt = punkt,
        panzerung = spielerTyp.panzerung,
        typ = spielerTyp,
        nummer = nummer ?: einheitenNummer.getOrDefault(this.spielerTyp, 0)
            .also { einheitenNummer[this.spielerTyp] = it + 1 }
    ).also { einheiten.add(it) }
}

fun smaller(a: Double, b: Double): Double {
    if (a.absoluteValue <= b.absoluteValue) {
        return a
    }
    return b
}

fun nachVorne(spielerTyp: SpielerTyp): Int {
    return if (spielerTyp == SpielerTyp.client || spielerTyp == SpielerTyp.computer) 1 else -1
}

fun einheitenAnzahl(spieler: Spieler, einheitenTyp: EinheitenTyp): Int {
    return spieler.einheiten.count { it.typ == einheitenTyp}
}

fun gebäudeAnzahl(spieler: Spieler, gebäudeTyp: GebäudeTyp): Int {
    return spieler.gebäude.values.count { it.typ == gebäudeTyp }
}

//Features:
//wenn man nichts mit einem Auswahlrechteck auswählt sollen die ausgewählten Einheiten nicht abgewählt werden (außer wenn man shift drückt)
//Wenn man shift drückt und Einheiten auswählt sollen die alten Einheiten nicht abewählt werden
//wenn man mit shift Einheiten auswähl, die schon ausgewählt sind sollen diese abgewählt werden
//Wenn eine Einheit ein automatisches Ziel hat und man mit shift ein anderes Ziel gibt soll das automatische Ziel zuerst ausgeführt werden
//Chat
//Kriegsnebel
//Sichtweite für Einheiten
//Minnimap
//kontrollgruppen
//Konsole
//lebensanzeige(lebensbalken)
//recourssen auf der Karte (wissenschafts-und produktionsressoursen)
//arbeiter und wissenschafter können ressoursen abbauen bzw. erforschen und zu außenposten bringen
//vershiedene Einheitengrößen
//Physik-Angin
//bessere Grafik mit 3D-Moddelen und Animationen
//tooltips
//sound
//Hintergrundmusik
//totorial
//verbessertes Multiplayer (beide Spieler starten unten und die Komandos werden gespiegelt)
//Mapeditor
//Missionseditor
//kampagne
//Einheiteneditor
//mehr Einheiten + Upgrades
//Menü
//balance
//bessere KI
//Punkte auf der Karte die von Spielern besetzt werden können (z.B. verlassene Minen die repariert werden können)
//programm-optiemierung

//Rassen:
//Silikoiden:
//Eine Ressource mehr als die anderen Rassen (silizium); high tech; teure, große, schnelle Einheiten; kosteneffizente Einheiten
//Punkte auf der Karte die nur für eine Rasse sichtbar sind (Siliziumvorkommen) die durch eine Rafinerie abbauen können; Einheiten fusionieren; viele Upgrades
//Terraner:
//Mechs, Infantrie
//Psilons:
//psionische Einheiten; Templer; Helden-Einheiten; Mana für Zaubersprüche; Archiv um Zaubersprüche um für die Templer zu erlernen; XP für Helden-Einheiten;
//unerfahrene Einheiten können nur einfache; Entscheidungen über tech tree für Upgrades; Unterstützungseinheiten
//Alkari:
//Nur biologische Einheiten; Larven; Billige Einheiten; nur eine Ressource (biomasse); können statt Forschung spezialeinheiten bauen; genmutationen; kostenineffiziente Einheiten
//meklars (KI):
//robotische Einheiten; Systeme für Einheiten zuweisen die die Rolle der Einheit angibt und boni bringt (z.B. mehr leben)
