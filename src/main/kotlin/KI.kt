import kotlin.math.max

enum class Strategie {
    Angreifen,
    Verteidigen,
    Expandieren
}

fun produzierenKI(spieler: Spieler, gegner: Spieler): Pair<EinheitenTyp?, GebäudeTyp?> {
    val gStrategie = scoutingAuswerten(gegner)
    val einheit = if (gStrategie[0] + gStrategie[1] * 2 + (minOf(
            gStrategie[2] / 5,
            gebäudeAnzahl(gegner, fabrik)
        ) - minOf(spieler.minen, gebäudeAnzahl(spieler, kaserne) * 2)) <= 30
    ) {
        infantrie
    } else if (gStrategie[0] + minOf(gStrategie[2] / 5, gebäudeAnzahl(gegner, kaserne) * 2) >= einheitenAnzahl(
            spieler,
            infantrie
        ) + einheitenAnzahl(spieler, panzer) * 4 + minOf(spieler.minen, gebäudeAnzahl(spieler, fabrik))
    ) {
        panzer
    } else {
        null
    }
    if (einheit != null && einheit.gebäudeTyp != null) {
        if (gebäudeAnzahl(spieler, einheit.gebäudeTyp) == 0) {
            return null to einheit.gebäudeTyp
        }
    }
    return einheit to null
}

fun zielAuswählenKI(spieler: Spieler, einheit: Einheit, gegner: Spieler): Punkt? {
    if (spieler.einheiten.size > 10) {
        return gegner.startpunkt
    }
    return null
}

val konterStrategie = mapOf(
    Strategie.Angreifen to Strategie.Verteidigen,
    Strategie.Verteidigen to Strategie.Expandieren,
    Strategie.Expandieren to Strategie.Angreifen
)

fun scoutingAuswerten(gegner: Spieler): List<Int> {
    var angriff = gegner.einheiten.size - einheitenAnzahl(gegner, panzer)
    var verteidigung = einheitenAnzahl(gegner, panzer) * 2
    var expansion = gegner.minen * 5
    val gesamtwert = angriff + verteidigung + expansion
    angriff /= gesamtwert
    angriff *= 100
    verteidigung /= gesamtwert
    verteidigung *= 100
    expansion /= gesamtwert
    expansion *= 100
    return listOf(angriff, verteidigung, expansion)
}