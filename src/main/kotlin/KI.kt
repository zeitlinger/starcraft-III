fun zielAuswählenKI(spieler: Spieler, gegner: Spieler, einheit: Einheit): Einheit? {
    val nächsteEinheit = gegner.einheiten
        .filter { kannAngreifen(einheit, it) }
        .minByOrNull { entfernung(einheit, it) }

    val einheitenAnzahl = spieler.einheiten.size

    if (einheitenAnzahl > 20) {
        return nächsteEinheit
    } else {
        return null
    }
}

fun einheitenProduzierenKI(spieler: Spieler): EinheitenTyp {
    return infantrie
}