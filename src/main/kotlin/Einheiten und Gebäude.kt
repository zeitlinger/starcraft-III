@file:Suppress("SpellCheckingInspection")

import javafx.scene.control.Button
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Text
import kotlin.properties.Delegates

data class Gebäude(
    val name: String,
    val kristalle: Int
)

val kaserne = Gebäude(name = "Kaserne", kristalle = 1500)
val fabrik = Gebäude(name = "Fabrik", kristalle = 2000)
val raumhafen = Gebäude(name = "Raumhafen", kristalle = 2500)
val brutstätte = Gebäude(name = "Brutstätte", kristalle = 2500)
val prodoktionsgebäude = listOf(
    kaserne,
    fabrik,
    raumhafen,
    brutstätte
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
val vipernbau = TechGebäude(name = "Vipernbau", kristalle = 2000, gebäude = brutstätte)
val techgebäude = listOf(
    schmiede,
    fusionskern,
    akademie,
    reaktor,
    vipernbau
)

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
    var button: Button? = null,
    var springen: Int? = null,
    val typ: Typ,
    var flächenschaden: Int? = null,
    var schusscooldown: Double = 1.0,
    var machtZustand: MachtZustand? = null,
    val zivileEinheit: Boolean = false
)

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
    var hatSichBewegt: Boolean = false,
    var vergiftet: Double = 0.0,
    var verlangsamt: Double = 0.0,
    var stim: Double = 0.0,
    val kommandoQueue: MutableList<Kommando> = mutableListOf()
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


typealias DoubleObserver = (Double) -> Unit

class Spieler(
    val einheiten: MutableList<Einheit> = mutableListOf(),
    private val kristallObservers: MutableList<DoubleObserver> = mutableListOf(),
    kristalle: Double,
    var angriffspunkte: Int,
    var verteidiegungspunkte: Int,
    var minen: Int,
    var startpunkt: Punkt,
    val farbe: Color,
    val mensch: Boolean,
    val kristalleText: Text = Text(),
    val minenText: Text = Text(),
    var schadensUpgrade: Int,
    var panzerungsUprade: Int,
    var vertärkteHeilmittel: Boolean = false,
    var strahlungsheilung: Boolean = false
) {
    var kristalle: Double by Delegates.observable(kristalle) { _, _, new ->
        this.kristallObservers.forEach { it(new) }
    }

    fun addKristallObserver(o: DoubleObserver) {
        this.kristallObservers.add(o)
        o(kristalle)
    }

    override fun toString(): String {
        return "Spieler(mensch=$mensch)"
    }
}


sealed class Kommando(var zielpunktLinie: Line? = null, var zielpunktkreis: Arc? = null) {
    class Bewegen(val zielPunkt: Punkt) : Kommando()
    class Attackmove(val zielPunkt: Punkt) : Kommando()
    class Angriff(val ziel: Einheit) : Kommando()
    class Patrolieren(val punkt1: Punkt, val punkt2: Punkt) : Kommando()
    class HoldPosition : Kommando()
}

val mInfantrie = EinheitenTyp(
    name = "Infantrie",
    reichweite = 150.0,
    leben = 1000.0,
    schaden = 1.0,
    laufweite = 0.5,
    kristalle = 500,
    kuerzel = "INF",
    panzerung = 0.125,
    kannAngreifen = KannAngreifen.alles,
    gebäude = kaserne,
    hotkey = "q",
    typ = Typ.biologisch,
    durchschlag = 0.0
)
val mEliteinfantrie = EinheitenTyp(
    name = "Elite-Infantrie",
    reichweite = 250.0,
    leben = 1200.0,
    schaden = 1.8,
    laufweite = 0.8,
    kristalle = 1400,
    kuerzel = "ELI",
    panzerung = 0.1,
    kannAngreifen = KannAngreifen.alles,
    gebäude = kaserne,
    techGebäude = akademie,
    hotkey = "w",
    typ = Typ.biologisch,
    durchschlag = 0.0
)
val mBerserker = EinheitenTyp(
    name = "Berserker",
    reichweite = 40.01,
    leben = 2000.0,
    schaden = 4.0,
    laufweite = 0.5,
    kristalle = 1000,
    kuerzel = "BER",
    panzerung = 0.25,
    kannAngreifen = KannAngreifen.boden,
    gebäude = kaserne,
    techGebäude = schmiede,
    hotkey = "e",
    typ = Typ.biologisch,
    durchschlag = 0.0
)
val mFlammenwerfer = EinheitenTyp(
    name = "Flammenwerfer",
    reichweite = 100.0,
    leben = 2600.0,
    schaden = 2.0,
    laufweite = 1.2,
    kristalle = 2200,
    kuerzel = "FLA",
    panzerung = 0.3,
    kannAngreifen = KannAngreifen.boden,
    gebäude = fabrik,
    hotkey = "r",
    typ = Typ.mechanisch,
    durchschlag = 0.0
)
val mPanzer = EinheitenTyp(
    name = "Panzer",
    reichweite = 350.0,
    leben = 10000.0,
    schaden = 5.0,
    laufweite = 0.25,
    kristalle = 2500,
    kuerzel = "PAN",
    panzerung = 0.4,
    kannAngreifen = KannAngreifen.boden,
    gebäude = fabrik,
    techGebäude = reaktor,
    hotkey = "t",
    typ = Typ.mechanisch,
    flächenschaden = 25,
    durchschlag = 0.0
)
val mBasis = EinheitenTyp(
    name = "Basis",
    reichweite = 0.0,
    leben = 30000.0,
    schaden = 0.0,
    laufweite = 0.0,
    kristalle = 0,
    kuerzel = "BAS",
    panzerung = 0.45,
    kannAngreifen = KannAngreifen.alles,
    gebäude = null,
    hotkey = null,
    typ = Typ.struktur,
    durchschlag = 0.0
)
val mJäger = EinheitenTyp(
    name = "Jäger",
    reichweite = 120.0,
    leben = 800.0,
    schaden = 3.0,
    laufweite = 0.8,
    kristalle = 1800,
    kuerzel = "JÄG",
    panzerung = 0.14,
    kannAngreifen = KannAngreifen.alles,
    luftBoden = LuftBoden.luft,
    gebäude = raumhafen,
    hotkey = "a",
    typ = Typ.mechanisch,
    durchschlag = 0.0
)
val mSanitäter = EinheitenTyp(
    name = "Sanitäter",
    reichweite = 40.01,
    leben = 800.0,
    schaden = 2.0,
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
val mKampfschiff = EinheitenTyp(
    name = "Kampfschiff",
    reichweite = 250.0,
    leben = 20000.0,
    schaden = 9.0,
    laufweite = 0.2,
    kristalle = 3500,
    kuerzel = "KSF",
    panzerung = 0.5,
    kannAngreifen = KannAngreifen.alles,
    luftBoden = LuftBoden.luft,
    gebäude = raumhafen,
    techGebäude = fusionskern,
    hotkey = "d",
    typ = Typ.mechanisch,
    durchschlag = 0.0
)
val mArbeiter = EinheitenTyp(
    name = "Arbeiter",
    reichweite = 0.0,
    leben = 800.0,
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
val mSpäher = EinheitenTyp(
    name = "Späher",
    reichweite = 0.0,
    leben = 800.0,
    schaden = 0.0,
    laufweite = 0.7,
    kristalle = 200,
    kuerzel = "SPÄ",
    panzerung = 0.0,
    kannAngreifen = KannAngreifen.alles,
    gebäude = kaserne,
    hotkey = "g",
    typ = Typ.biologisch,
    durchschlag = 0.0
)
val mSonde = EinheitenTyp(
    name = "Sonde",
    reichweite = 0.0,
    leben = 800.0,
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
val mWissenschaftler = EinheitenTyp(
    name = "Wissenschaftler",
    reichweite = 0.0,
    leben = 800.0,
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
val mKreuzschiff = EinheitenTyp(
    name = "Kreuzschiff",
    reichweite = 300.0,
    leben = 10000.0,
    schaden = 4.0,
    laufweite = 0.5,
    kristalle = 3000,
    kuerzel = "KSF",
    panzerung = 0.3,
    kannAngreifen = KannAngreifen.boden,
    gebäude = null,
    hotkey = "c",
    typ = Typ.mechanisch,
    luftBoden = LuftBoden.wasser,
    durchschlag = 0.0
)
val mForschungsschiff = EinheitenTyp(
    name = "Forschungsschiff",
    reichweite = 0.0,
    leben = 1000.0,
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
val mViper = EinheitenTyp(
    name = "Viper",
    reichweite = 120.0,
    leben = 1500.0,
    schaden = 1.5,
    laufweite = 1.0,
    kristalle = 1500,
    kuerzel = "VIP",
    panzerung = 0.2,
    kannAngreifen = KannAngreifen.boden,
    gebäude = brutstätte,
    techGebäude = vipernbau,
    hotkey = "b",
    typ = Typ.mechanisch,
    luftBoden = LuftBoden.boden,
    machtZustand = MachtZustand.vergiftung,
    durchschlag = 0.0
)

val cInfantrie = mInfantrie.copy()
val cEliteinfantrie = mEliteinfantrie.copy()
val cBerserker = mBerserker.copy()
val cFlammenwerfer = mFlammenwerfer.copy()
val cPanzer = mPanzer.copy()
val cBasis = mBasis.copy()
val cJäger = mJäger.copy()
val cSanitäter = mSanitäter.copy()
val cKampfschiff = mKampfschiff.copy()
val cArbeiter = mArbeiter.copy()
val cSpäher = mSpäher.copy()
val cSonde = mSonde.copy()
val cWissenschaftler = mWissenschaftler.copy()
val cKreuzschiff = mKreuzschiff.copy()
val cForschungsschiff = mForschungsschiff.copy()
val cViper = mViper.copy()

val kaufbareEinheiten = listOf(
    mInfantrie,
    mEliteinfantrie,
    mBerserker,
    mPanzer,
    mJäger,
    mSanitäter,
    mKampfschiff,
    mFlammenwerfer,
    mArbeiter,
    mSpäher,
    mViper,
    mSonde,
    mWissenschaftler,
    mKreuzschiff,
    mForschungsschiff
)
