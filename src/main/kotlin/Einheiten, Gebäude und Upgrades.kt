@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "NonAsciiCharacters", "PropertyName", "FunctionName")

import javafx.scene.control.Button
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.properties.Delegates


fun gebäudeEinheitenTyp(gebäudeTyp: GebäudeTyp) = EinheitenTyp(
    name = gebäudeTyp.name,
    reichweite = 0.0,
    leben = 3000.0,
    schaden = 0.0,
    laufweite = 0.0,
    kristalle = gebäudeTyp.kristalle,
    kuerzel = gebäudeTyp.kuerzel,
    panzerung = 5.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = null,
    hotkey = null,
    einheitenArt = EinheitenArt.struktur,
    durchschlag = 0.0
)

val neutraleEinheitenTypen: MutableMap<String, EinheitenTyp> = mutableMapOf()

@Serializable
data class GebäudeTyp(
    val name: String,
    val kuerzel: String,
    val kristalle: Int,
) {
    init {
        gebäudeTypen.add(this)
        gebäudeEinheitenTyp(this)
    }
}

val gebäudeTypen = mutableListOf<GebäudeTyp>()

val basis = GebäudeTyp(
    name = "Basis",
    kristalle = 0,
    kuerzel = "BAS",
)

val kaserne = GebäudeTyp(name = "Kaserne", kuerzel = "KAS", kristalle = 1500)
val fabrik = GebäudeTyp(name = "Fabrik", kuerzel = "FAB", kristalle = 2000)
val raumhafen = GebäudeTyp(name = "Raumhafen", kuerzel = "RAU", kristalle = 2500)
val brutkolonie = GebäudeTyp(name = "Brutstätte", kuerzel = "BRU", kristalle = 2500)
val labor = GebäudeTyp(name = "Labor", kuerzel = "LAB", kristalle = 2800)

@Serializable
data class TechGebäude(
    val name: String,
    val kuerzel: String,
    val kristalle: Int,
    val vorraussetzung: GebäudeTyp,
) {
    init {
        techgebäude[this] = GebäudeTyp(name, kuerzel, kristalle)
    }
}

val techgebäude = mutableMapOf<TechGebäude, GebäudeTyp>()

val schmiede = TechGebäude(name = "Schmiede", kuerzel = "SMD", kristalle = 2000, vorraussetzung = kaserne)
val fusionskern = TechGebäude(name = "Fusionskern", kuerzel = "FSK", kristalle = 3000, vorraussetzung = raumhafen)
val weltraumAkademie = TechGebäude(name = "Akademie", kuerzel = "AKA", kristalle = 2500, vorraussetzung = kaserne)
val reaktor = TechGebäude(name = "Reaktor", kuerzel = "REA", kristalle = 2000, vorraussetzung = fabrik)
val vipernbau = TechGebäude(name = "Vipernbau", kuerzel = "VIP", kristalle = 2000, vorraussetzung = brutkolonie)

enum class KannAngreifen {
    alles, boden, luft, heilen
}

enum class LuftBoden {
    luft, boden, wasser
}

enum class EinheitenArt {
    biologisch, mechanisch, psionisch, struktur
}

enum class MachtZustand {
    langsamkeit, vergiftung
}

typealias ZeitInSec = Double

@Serializable
data class EinheitenTyp constructor(
    var schaden: Double,
    var reichweite: Double,
    val leben: Double,
    var laufweite: Double,
    val kristalle: Int,
    val kuerzel: String,
    val panzerung: Double,
    var durchschlag: Double,
    val kannAngreifen: KannAngreifen,
    val luftBoden: LuftBoden = LuftBoden.boden,
    val gebäudeTyp: GebäudeTyp?,
    val techGebäude: TechGebäude? = null,
    val name: String,
    val hotkey: String?,
    @Transient
    var button: Button? = null,
    var springen: Int? = null,
    var yamatokanone: Int? = null,
    val einheitenArt: EinheitenArt,
    var flächenschaden: Double? = null,
    var schusscooldown: ZeitInSec = 1.0,
    val firstShotDelay: ZeitInSec = 0.5,
    var machtZustand: MachtZustand? = null,
    val zivileEinheit: Boolean = false,
    val produktionsZeit: ZeitInSec = 5.0,
    val spielerTyp: SpielerTyp = SpielerTyp.mensch,
) {
    init {
        if (spielerTyp == SpielerTyp.mensch) {
            neutraleEinheitenTypen[name] = this
        }
    }
}

var einheitenNummer: MutableMap<SpielerTyp, Int> = mutableMapOf()

@Serializable(with = EinheitSerializer::class)
data class Einheit(
    val spieler: Spieler,
    val typ: EinheitenTyp,
    var leben: Double,
    var punkt: Punkt,
    val bild: Circle = einheitenBild(),
    var kuerzel: Text? = null,
    var lebenText: Text? = null,
    var panzerung: Double,
    var auswahlkreis: Arc? = null,
    var `springen cooldown`: ZeitInSec = 0.0,
    var `yamatokane cooldown`: ZeitInSec = 0.0,
    var heiler: Einheit? = null,
    var wirdGeheilt: Int = 0,
    var zuletztGetroffen: ZeitInSec = 0.0,
    var schusscooldown: ZeitInSec = 0.0,
    var firstShotCooldown: ZeitInSec = 0.0,
    var hatSichBewegt: Boolean = false,
    var vergiftet: ZeitInSec = 0.0,
    var verlangsamt: ZeitInSec = 0.0,
    val kommandoQueue: MutableList<EinheitenKommando> = mutableListOf(),
    val nummer: Int,
    var letzterAngriff: Einheit? = null
) {
    val gebäude: Gebäude?
        get() = spieler.gebäude(nummer)

    override fun toString(): String {
        return "Einheit(spieler=$spieler, typ=${typ.kuerzel}, punkt=$punkt)"
    }
}

@Serializable
data class Punkt(
    val x: Double,
    val y: Double
)


typealias DoubleObserver = (Double) -> Unit

typealias EinheitenBauObserver = (Einheit) -> Unit


@Serializable
sealed class EinheitenKommando(
    @Transient
    val zielpunktLinie: MutableList<Line> = mutableListOf(),
    @Transient
    val zielpunktkreis: MutableList<Arc> = mutableListOf(),
)

@Serializable
class Bewegen(val zielPunkt: Punkt) : EinheitenKommando()

@Serializable
class Attackmove(val zielPunkt: Punkt) : EinheitenKommando()

@Serializable
class Angriff(val ziel: Einheit) : EinheitenKommando()

@Serializable
class Patroullieren(
    val punkte: MutableList<Punkt>,
    var nächsterPunkt: Punkt,
    var nächsterPunktNumer: Int,
    var vorwärtsGehen: Boolean,
    var imKreisGehen: Boolean
) : EinheitenKommando()

@Serializable
class HoldPosition : EinheitenKommando()

@Serializable
class Stopp : EinheitenKommando()

@Serializable
class Yamatokanone(val ziel: Einheit) : EinheitenKommando()

val kommandoHotKeys = mapOf(
    "s" to { Stopp() },
    "h" to { HoldPosition() }
)

val infantrie = EinheitenTyp(
    name = "Infantrie",
    reichweite = 150.0,
    leben = 100.0,
    schaden = 7.0,
    laufweite = 0.5,
    kristalle = 500,
    kuerzel = "INF",
    panzerung = 1.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = kaserne,
    hotkey = "q",
    einheitenArt = EinheitenArt.biologisch,
    durchschlag = 0.0
)
val eliteInfantrie = EinheitenTyp(
    name = "Elite-Infantrie",
    reichweite = 250.0,
    leben = 120.0,
    schaden = 12.0,
    laufweite = 0.8,
    kristalle = 1400,
    kuerzel = "ELI",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = kaserne,
    techGebäude = weltraumAkademie,
    hotkey = "w",
    einheitenArt = EinheitenArt.biologisch,
    durchschlag = 0.0
)
val berserker = EinheitenTyp(
    name = "Berserker",
    reichweite = 40.01,
    leben = 200.0,
    schaden = 25.0,
    laufweite = 0.5,
    kristalle = 1000,
    kuerzel = "BER",
    panzerung = 2.0,
    kannAngreifen = KannAngreifen.boden,
    gebäudeTyp = kaserne,
    techGebäude = schmiede,
    hotkey = "e",
    einheitenArt = EinheitenArt.biologisch,
    durchschlag = 0.0
)
val flammenwerfer = EinheitenTyp(
    name = "Flammenwerfer",
    reichweite = 100.0,
    leben = 260.0,
    schaden = 12.0,
    laufweite = 1.2,
    kristalle = 2200,
    kuerzel = "FLA",
    panzerung = 2.0,
    kannAngreifen = KannAngreifen.boden,
    gebäudeTyp = fabrik,
    hotkey = "r",
    einheitenArt = EinheitenArt.mechanisch,
    durchschlag = 0.0
)
val panzer = EinheitenTyp(
    name = "Panzer",
    reichweite = 350.0,
    leben = 1000.0,
    schaden = 30.0,
    laufweite = 4.0,
    kristalle = 2500,
    kuerzel = "PAN",
    panzerung = 0.4,
    kannAngreifen = KannAngreifen.boden,
    gebäudeTyp = fabrik,
    techGebäude = reaktor,
    hotkey = "t",
    einheitenArt = EinheitenArt.mechanisch,
    flächenschaden = 25.0,
    durchschlag = 0.0
)

val jäger = EinheitenTyp(
    name = "Jäger",
    reichweite = 120.0,
    leben = 80.0,
    schaden = 20.0,
    laufweite = 0.8,
    kristalle = 1800,
    kuerzel = "JÄG",
    panzerung = 2.0,
    kannAngreifen = KannAngreifen.alles,
    luftBoden = LuftBoden.luft,
    gebäudeTyp = raumhafen,
    hotkey = "a",
    einheitenArt = EinheitenArt.mechanisch,
    durchschlag = 0.0
)
val sanitäter = EinheitenTyp(
    name = "Sanitäter",
    reichweite = 40.01,
    leben = 80.0,
    schaden = 12.0,
    laufweite = 0.7,
    kristalle = 1100,
    kuerzel = "SAN",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.heilen,
    gebäudeTyp = kaserne,
    techGebäude = weltraumAkademie,
    hotkey = "s",
    einheitenArt = EinheitenArt.biologisch,
    zivileEinheit = true,
    durchschlag = 0.0
)
val kampfschiff = EinheitenTyp(
    name = "Kampfschiff",
    reichweite = 250.0,
    leben = 2000.0,
    schaden = 60.0,
    laufweite = 0.2,
    kristalle = 5000,
    kuerzel = "KSF",
    panzerung = 4.0,
    kannAngreifen = KannAngreifen.alles,
    luftBoden = LuftBoden.luft,
    gebäudeTyp = raumhafen,
    techGebäude = fusionskern,
    hotkey = "d",
    einheitenArt = EinheitenArt.mechanisch,
    durchschlag = 0.0
)
val arbeiter = EinheitenTyp(
    name = "Arbeiter",
    reichweite = 0.0,
    leben = 80.0,
    schaden = 0.0,
    laufweite = 0.7,
    kristalle = 1000,
    kuerzel = "ARB",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = basis,
    hotkey = "f",
    einheitenArt = EinheitenArt.biologisch,
    zivileEinheit = true,
    durchschlag = 0.0
)
val späher = EinheitenTyp(
    name = "Späher",
    reichweite = 0.0,
    leben = 80.0,
    schaden = 0.0,
    laufweite = 0.7,
    kristalle = 200,
    kuerzel = "SPÄ",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = kaserne,
    hotkey = "g",
    einheitenArt = EinheitenArt.biologisch,
    zivileEinheit = true,
    durchschlag = 0.0
)
val sonde = EinheitenTyp(
    name = "Sonde",
    reichweite = 0.0,
    leben = 80.0,
    schaden = 0.0,
    laufweite = 0.7,
    kristalle = 1000,
    kuerzel = "SON",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = null,
    hotkey = "y",
    einheitenArt = EinheitenArt.mechanisch,
    zivileEinheit = true,
    durchschlag = 0.0
)
val wissenschaftler = EinheitenTyp(
    name = "Wissenschaftler",
    reichweite = 0.0,
    leben = 80.0,
    schaden = 0.0,
    laufweite = 0.7,
    kristalle = 2000,
    kuerzel = "WIS",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = null,
    hotkey = "x",
    einheitenArt = EinheitenArt.biologisch,
    zivileEinheit = true,
    durchschlag = 0.0
)
val kreuzschiff = EinheitenTyp(
    name = "Kreuzschiff",
    reichweite = 300.0,
    leben = 1000.0,
    schaden = 25.0,
    laufweite = 0.5,
    kristalle = 3000,
    kuerzel = "KSF",
    panzerung = 3.0,
    kannAngreifen = KannAngreifen.boden,
    gebäudeTyp = null,
    hotkey = "c",
    einheitenArt = EinheitenArt.mechanisch,
    luftBoden = LuftBoden.wasser,
    durchschlag = 0.0
)
val forschungsschiff = EinheitenTyp(
    name = "Forschungsschiff",
    reichweite = 0.0,
    leben = 100.0,
    schaden = 0.0,
    laufweite = 0.9,
    kristalle = 2500,
    kuerzel = "FSF",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäudeTyp = null,
    hotkey = "v",
    einheitenArt = EinheitenArt.mechanisch,
    luftBoden = LuftBoden.wasser,
    zivileEinheit = true,
    durchschlag = 0.0
)
val viper = EinheitenTyp(
    name = "Viper",
    reichweite = 120.0,
    leben = 150.0,
    schaden = 10.0,
    laufweite = 1.0,
    kristalle = 1500,
    kuerzel = "VIP",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.boden,
    gebäudeTyp = brutkolonie,
    techGebäude = vipernbau,
    hotkey = "b",
    einheitenArt = EinheitenArt.mechanisch,
    luftBoden = LuftBoden.boden,
    machtZustand = MachtZustand.vergiftung,
    durchschlag = 0.0
)

enum class SpielerTyp {
    mensch,
    computer,
    client,
    server
}

@Serializable
data class SpielerUpgrades(
    var angriffspunkte: Int,
    var verteidiegungspunkte: Int,
    var schadensUpgrade: Int,
    var panzerungsUprade: Int,
    var vertärkteHeilmittel: Boolean = false,
    var strahlungsheilung: Boolean = false,
)

class Spieler(
    val einheiten: MutableList<Einheit> = mutableListOf(),
    val gebäude: MutableMap<Int, Gebäude> = mutableMapOf(),
    private val kristallObservers: MutableList<DoubleObserver> = mutableListOf(),
    private val einheitenObservers: MutableList<EinheitenBauObserver> = mutableListOf({ einheit ->
        gebäudeTypen.filter { it.name == einheit.typ.name && gebäude[einheit.nummer] == null }.forEach { typ ->
            val punkt = einheit.punkt
            val sammelpunkt = punkt.copy(y = punkt.y + 70 * nachVorne(spielerTyp))
            gebäude[einheit.nummer] = Gebäude(typ, einheit, sammelpunkt = kreis(sammelpunkt, radius = 5.0))
        }
    }),
    kristalle: Double,
    var upgrades: SpielerUpgrades,
    var minen: Int,
    var startpunkt: Punkt,
    val farbe: Color,
    val spielerTyp: SpielerTyp,
    val einheitenTypen: MutableMap<String, EinheitenTyp> =
        neutraleEinheitenTypen.values.map { it.copy(spielerTyp = spielerTyp) }.associateBy { it.name }.toMutableMap()
) {
    var kristalle: Double by Delegates.observable(kristalle) { _, _, new ->
        this.kristallObservers.forEach { it(new) }
    }

    fun addKristallObserver(o: DoubleObserver) {
        this.kristallObservers.add(o)
        o(kristalle)
    }

    fun addEinheitenObserver(o: EinheitenBauObserver) {
        this.einheitenObservers.add(o)
    }

    fun einheit(nummer: Int): Einheit {
        return einheiten.single { it.nummer == nummer }
    }

    fun gebäude(nummer: Int): Gebäude? {
        return gebäude[nummer]
    }

    fun gebäudeEinheitenTyp(gebäudeTyp: GebäudeTyp) = einheitenTypen.getValue(gebäudeTyp.name)

    override fun toString(): String {
        return "Spieler(typ=$spielerTyp)"
    }

    fun neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp, nummer: Int? = null): Einheit {
        return neueEinheit(Punkt(x, y), einheitenTyp, nummer)
    }

    fun neueEinheit(punkt: Punkt, einheitenTyp: EinheitenTyp, nummer: Int? = null): Einheit {
        val spielerTyp = einheitenTypen.getValue(einheitenTyp.name)
        return Einheit(
            spieler = this,
            leben = spielerTyp.leben,
            punkt = punkt,
            panzerung = spielerTyp.panzerung,
            typ = spielerTyp,
            nummer = nummer ?: einheitenNummer.getOrDefault(this.spielerTyp, 0)
                .also { einheitenNummer[this.spielerTyp] = it + 1 }
        ).also { einheit ->
            einheiten.add(einheit)
            einheitenObservers.forEach { it(einheit) }
        }
    }
}

data class Upgrade(
    val gebäudeTyp: GebäudeTyp,
    val name: (SpielerUpgrades) -> String,
    val kritalle: (SpielerUpgrades) -> Int,
    val eiheitenUpgrades: Map<EinheitenTyp, EinheitenTyp.() -> Boolean> = emptyMap(),
    val spielerUpgrade: SpielerUpgrades.() -> Boolean = { false }
)

val upgrades: List<Upgrade> = listOf(
    Upgrade(
        labor,
        { "LV " + (it.schadensUpgrade + 1) + " Schaden" },
        { 2000 + 400 * it.schadensUpgrade },
        spielerUpgrade = {
            schadensUpgrade += 1
            schadensUpgrade == 5
        }
    ),
    Upgrade(
        labor,
        { "LV " + (it.panzerungsUprade + 1) + " Panzerug" },
        { 2000 + 400 * it.panzerungsUprade },
        spielerUpgrade = {
            panzerungsUprade += 1
            panzerungsUprade == 5
        }
    ),
    Upgrade(
        labor,
        { "Ansturm" },
        { 1500 },
        mapOf(berserker to {
            laufweite = 1.0
            springen = 150
            true
        })
    ),
    Upgrade(
        labor,
        { "Verbesserte Zielsysteme" },
        { 1500 },
        mapOf(panzer to {
            reichweite = 500.0
            true
        })
    ),
    Upgrade(
        labor,
        { "Fusionsantrieb" },
        { 1500 },
        mapOf(
            jäger to {
                laufweite = 1.2
                true
            },
            kampfschiff to {
                laufweite = 0.3
                true
            },
        )
    ),
    Upgrade(
        labor,
        { "Verstärkte Heilmittel" },
        { 1500 },
        mapOf(sanitäter to {
            schaden = 3.0
            true
        }),
        spielerUpgrade = {
            vertärkteHeilmittel = true
            true
        },
    ),
    Upgrade(
        labor,
        { "Strahlungsheilung" },
        { 1500 },
        mapOf(sanitäter to {
            reichweite = 140.01
            true
        }),
        spielerUpgrade = {
            strahlungsheilung = true
            true
        },
    ),
    Upgrade(
        labor,
        { "Flammenwurf" },
        { 1500 },
        mapOf(flammenwerfer to {
            flächenschaden = 40.0
            schaden = 2.5
            true
        })
    )
)
