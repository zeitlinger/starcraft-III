fun zielAuswählenKI(gegner: Spieler, einheit: Einheit): Einheit? {
    val nächsteEinheit = gegner.einheiten
        .filter { kannAngreifen(einheit, it) }
        .minBy { entfernung(einheit, it) }

    return nächsteEinheit
}