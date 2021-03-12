@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "NonAsciiCharacters")

import javafx.scene.control.Button
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.properties.Delegates


fun gebäudeEinheitenTyp(gebäude: Gebäude) = EinheitenTyp(
    name = gebäude.name,
    reichweite = 0.0,
    leben = 3000.0,
    schaden = 0.0,
    laufweite = 0.0,
    kristalle = gebäude.kristalle,
    kuerzel = gebäude.kuerzel,
    panzerung = 5.0,
    kannAngreifen = KannAngreifen.alles,
    gebäude = null,
    hotkey = null,
    typ = Typ.struktur,
    durchschlag = 0.0
)

val neutraleEinheitenTypen: MutableMap<String, EinheitenTyp> = mutableMapOf()

@Serializable
data class Gebäude(
    val name: String,
    val kuerzel: String,
    val kristalle: Int,
) {
    init {
        gebäude.add(this)
        gebäudeEinheitenTyp(this)
    }
}

val gebäude = mutableListOf<Gebäude>()

val basis = Gebäude(
    name = "Basis",
    kristalle = 0,
    kuerzel = "BAS",
)

val kaserne = Gebäude(name = "Kaserne", kuerzel = "KAS", kristalle = 1500)
val fabrik = Gebäude(name = "Fabrik", kuerzel = "FAB", kristalle = 2000)
val raumhafen = Gebäude(name = "Raumhafen", kuerzel = "RAU", kristalle = 2500)
val brutstätte = Gebäude(name = "Brutstätte", kuerzel = "BRU", kristalle = 2500)
val labor = Gebäude(name = "Labor", kuerzel = "LAB", kristalle = 2800)

@Serializable
data class TechGebäude(
    val name: String,
    val kristalle: Int,
    val gebäude: Gebäude
) {
    init {
        techgebäude.add(this)
    }
}

val techgebäude = mutableListOf<TechGebäude>()

val schmiede = TechGebäude(name = "Schmiede", kristalle = 2000, gebäude = kaserne)
val fusionskern = TechGebäude(name = "Fusionskern", kristalle = 3000, gebäude = raumhafen)
val akademie = TechGebäude(name = "Akademie", kristalle = 2500, gebäude = kaserne)
val reaktor = TechGebäude(name = "Reaktor", kristalle = 2000, gebäude = fabrik)
val vipernbau = TechGebäude(name = "Vipernbau", kristalle = 2000, gebäude = brutstätte)

enum class KannAngreifen {
    alles, boden, luft, heilen
}

enum class LuftBoden {
    luft, boden, wasser
}

enum class Typ {
    biologisch, mechanisch, psionisch, struktur
}

enum class MachtZustand {
    langsamkeit, vergiftung
}

@Serializable
data class EinheitenTyp(
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
    val gebäude: Gebäude?,
    val techGebäude: TechGebäude? = null,
    val name: String,
    val hotkey: String?,
    @Transient
    var button: Button? = null,
    var springen: Int? = null,
    var yamatokanone: Int? = null,
    val typ: Typ,
    var flächenschaden: Double? = null,
    var schusscooldown: Double = 1.0,
    val firstShotDeley: Double = 0.5,
    var machtZustand: MachtZustand? = null,
    val zivileEinheit: Boolean = false,
    val spielerTyp: SpielerTyp = SpielerTyp.mensch
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
    var x: Double,
    var y: Double,
    val bild: Circle = einheitenBild(),
    var kuerzel: Text? = null,
    var lebenText: Text? = null,
    var panzerung: Double,
    var auswahlkreis: Arc? = null,
    var `springen cooldown`: Double = 0.0,
    var heiler: Einheit? = null,
    var wirdGeheilt: Int = 0,
    var zuletztGetroffen: Double = 0.0,
    var schusscooldown: Double = 0.0,
    var firstShotCooldown: Double = 0.0,
    var hatSichBewegt: Boolean = false,
    var vergiftet: Double = 0.0,
    var verlangsamt: Double = 0.0,
    val kommandoQueue: MutableList<EinheitenKommando> = mutableListOf(),
    val nummer: Int,
    var letzterAngriff: Einheit? = null
) {
    fun punkt() = Punkt(x = x, y = y)
    override fun toString(): String {
        return "Einheit(spieler=$spieler, typ=${typ.kuerzel}, x=$x, y=$y)"
    }
}

@Serializable
data class Punkt(
    var x: Double,
    var y: Double
)


typealias DoubleObserver = (Double) -> Unit


@Serializable
sealed class EinheitenKommando(
    @Transient
    var zielpunktLinie: Line? = null,
    @Transient
    var zielpunktkreis: Arc? = null
) {
    @Serializable
    class Bewegen(val zielPunkt: Punkt) : EinheitenKommando()

    @Serializable
    class Attackmove(val zielPunkt: Punkt) : EinheitenKommando()

    @Serializable
    class Angriff(val ziel: Einheit) : EinheitenKommando()

    @Serializable
    class Patrolieren(val punkt1: Punkt, val punkt2: Punkt) : EinheitenKommando()

    @Serializable
    class HoldPosition : EinheitenKommando()

    @Serializable
    class Stopp : EinheitenKommando()

    @Serializable
    class Yamatokanone(val ziel: Einheit) : EinheitenKommando()
}

val kommandoHotKeys = mapOf(
    "s" to { EinheitenKommando.Stopp() },
    "h" to { EinheitenKommando.HoldPosition() }
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
    gebäude = kaserne,
    hotkey = "q",
    typ = Typ.biologisch,
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
    gebäude = kaserne,
    techGebäude = akademie,
    hotkey = "w",
    typ = Typ.biologisch,
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
    gebäude = kaserne,
    techGebäude = schmiede,
    hotkey = "e",
    typ = Typ.biologisch,
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
    gebäude = fabrik,
    hotkey = "r",
    typ = Typ.mechanisch,
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
    gebäude = fabrik,
    techGebäude = reaktor,
    hotkey = "t",
    typ = Typ.mechanisch,
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
    gebäude = raumhafen,
    hotkey = "a",
    typ = Typ.mechanisch,
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
    gebäude = kaserne,
    techGebäude = akademie,
    hotkey = "s",
    typ = Typ.biologisch,
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
    gebäude = raumhafen,
    techGebäude = fusionskern,
    hotkey = "d",
    typ = Typ.mechanisch,
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
    gebäude = null,
    hotkey = "f",
    typ = Typ.biologisch,
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
    gebäude = kaserne,
    hotkey = "g",
    typ = Typ.biologisch,
    durchschlag = 0.0,
    yamatokanone = 300
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
    gebäude = null,
    hotkey = "y",
    typ = Typ.mechanisch,
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
    gebäude = null,
    hotkey = "x",
    typ = Typ.biologisch,
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
    gebäude = null,
    hotkey = "c",
    typ = Typ.mechanisch,
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
    gebäude = null,
    hotkey = "v",
    typ = Typ.mechanisch,
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
    gebäude = brutstätte,
    techGebäude = vipernbau,
    hotkey = "b",
    typ = Typ.mechanisch,
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
    private val kristallObservers: MutableList<DoubleObserver> = mutableListOf(),
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

    fun einheit(nummer: Int): Einheit {
        return einheiten.single { it.nummer == nummer }
    }

    override fun toString(): String {
        return "Spieler(typ=$spielerTyp)"
    }
}

data class Upgrade(
    val gebäude: Gebäude,
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


