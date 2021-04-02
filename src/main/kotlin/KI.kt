import kotlin.math.max

enum class Strategie {
    Angreifen,
    Verteidigen,
    Expandieren
}

val strateigieAusführen = mapOf(
    Strategie.Angreifen to infantrie,
    Strategie.Verteidigen to panzer,
    Strategie.Expandieren to null
)

fun produzierenKI(spieler: Spieler, gegner: Spieler): Pair<EinheitenTyp?, GebäudeTyp?> {
    val strategie = scoutingAuswerten(gegner)
    val einheit = strateigieAusführen[strategie]
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

fun scoutingAuswerten(gegner: Spieler): Strategie {
    val angriff = gegner.einheiten.size - einheitenAnzahl(gegner, panzer)
    val verteidigung = einheitenAnzahl(gegner, panzer) * 2
    val expansion = gegner.minen * 5
    val gegnerStrategie = mapOf(
        angriff to Strategie.Angreifen,
        verteidigung to Strategie.Verteidigen,
        expansion to Strategie.Expandieren
    )
    val strategeie = gegnerStrategie[maxOf(angriff, verteidigung, expansion)]
    return konterStrategie[strategeie]!!
}