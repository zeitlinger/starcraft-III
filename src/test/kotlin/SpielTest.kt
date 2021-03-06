@file:Suppress("NonAsciiCharacters")

import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import javafx.scene.paint.Color
import kotlin.math.sqrt

class SpielTest : FreeSpec({

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
        durchschlag = 0.0,
        firstShotDelay = 0.0,
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
        durchschlag = 0.0,
        firstShotDelay = 0.0,
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
        durchschlag = 0.0,
        firstShotDelay = 0.0,
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
        durchschlag = 0.0,
        firstShotDelay = 0.0,
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
        durchschlag = 0.0,
        firstShotDelay = 0.0,
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
        gebäudeTyp = null,
        hotkey = "f",
        einheitenArt = EinheitenArt.biologisch,
        zivileEinheit = true,
        durchschlag = 0.0,
        firstShotDelay = 0.0,
    )

    "Heiler soll automatisch heilen und Zustände entfernen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
                verlangsamt = 2.0
            }
            neueEinheit(x = 20.0, y = 0.0, einheitenTyp = sanitäter)
            upgrades.vertärkteHeilmittel = true
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 22.0
        spiel.mensch.einheiten[1].verlangsamt shouldBe 0.0
    }

    "Heiler soll nur heilen wenn die Einheit nicht angegriffen wird" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                leben = 10.0
                zuletztGetroffen = 1.0
            }
            neueEinheit(x = 20.0, y = 0.0, einheitenTyp = sanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 10.0
    }

    "Einheit soll nur von einen Heiler geheilt werden" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
            }
            neueEinheit(x = 20.0, y = 0.0, einheitenTyp = sanitäter)
            neueEinheit(x = 20.0, y = 0.0, einheitenTyp = sanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 22.0
    }

    "nur ein Heiler geht zum Ziel" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
            }
            neueEinheit(x = 200.0, y = 0.0, einheitenTyp = sanitäter)
            neueEinheit(x = 200.0, y = 0.0, einheitenTyp = sanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[2].punkt.x shouldBe 199.3
        spiel.mensch.einheiten[3].punkt.x shouldBe 200.0
    }

    "beschützen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            neueEinheit(x = 300.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[2].punkt shouldBe Punkt(x = 299.55278640450007, y = 0.22360679774997896)
    }

    "hold Position" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(
                x = 200.0,
                y = 0.0,
                einheitenTyp = infantrie
            ).apply { kommandoQueue.add(HoldPosition()) }
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].punkt shouldBe Punkt(x = 200.0, y = 0.0)
    }

    "Einheiten in Reichweite angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie).apply {

                letzterAngriff = spiel.mensch.einheiten[1]
            }
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBeLessThan 100.0
        spiel.gegner.einheiten[1].leben shouldBeLessThan 100.0
    }

    "Heilungsmodifikator" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie).apply {
                wirdGeheilt = 2

                letzterAngriff = spiel.mensch.einheiten[1]
            }
            upgrades.strahlungsheilung = true
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBeGreaterThan spiel.mensch.einheiten[1].leben
    }

    "vergiftung" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply { vergiftet = 1.0 }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 95.0
    }

    "verlangsamerung" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                verlangsamt = 1.0
                kommandoQueue.add(Bewegen(Punkt(x = 0.5, y = 0.0)))
            }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].punkt.x shouldBe 0.25
    }

    "Nicht in einer Runde laufen und angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 150.5, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBe 100.0
    }

    "Schusscooldown" {
        val spiel = neuesSpiel()

        spiel.mensch.apply {
            einheitenTypen.getValue(infantrie.name).schusscooldown = 0.03
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie).apply {

                letzterAngriff = spiel.mensch.einheiten[1]
            }
        }
        val ziel = spiel.gegner.einheiten[1]
        spiel.mensch.einheiten[1].letzterAngriff = ziel

        spielen(spiel)

        ziel.leben shouldBeLessThan 100.0

        val l = ziel.leben

        spielen(spiel)

        ziel.leben shouldBe l

        spielen(spiel)

        ziel.leben shouldBeLessThan l
    }

    "Flächenschaden" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = panzer)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
            neueEinheit(x = 25.0, y = 150.0, einheitenTyp = infantrie)
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBeLessThan 100.0
        spiel.gegner.einheiten[2].leben shouldBeLessThan 100.0
    }

    "schadensupgrade" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            upgrades.schadensUpgrade = 1
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBeLessThan spiel.mensch.einheiten[1].leben
    }

    "panzerugsupgrade" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
            upgrades.panzerungsUprade = 1
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBeGreaterThan spiel.mensch.einheiten[1].leben
    }

    "einheit stirbt" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie).apply { leben = 0.1 }
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = infantrie
            ).apply { kommandoQueue.add(Angriff(spiel.gegner.einheiten[1])) }
        }

        spielen(spiel)

        spiel.gegner.einheiten.size shouldBe 1
        spiel.mensch.einheiten[0].kommandoQueue.size shouldBe 0
    }

    "zum Zielpunkt laufen" - {
        data class TestFall(val start: Punkt, val ziel: Punkt)

        forAll(
            TestFall(
                start = Punkt(x = 1000.0, y = 0.0),
                ziel = Punkt(x = 999.6464466094068, y = 0.35355339059327373)
            ),
            TestFall(start = Punkt(x = 0.0, y = 1000.0), ziel = Punkt(x = 0.0, y = 1000.0)),
            TestFall(start = Punkt(x = 0.0, y = 999.0), ziel = Punkt(x = 0.0, y = 999.5)),
            TestFall(start = Punkt(x = 0.0, y = 999.999), ziel = Punkt(x = 0.0, y = 1000.0))
        ) { testFall ->
            val spiel = neuesSpiel()
            spiel.mensch.apply {
                neueEinheit(
                    x = testFall.start.x,
                    y = testFall.start.y,
                    einheitenTyp = infantrie
                ).apply { kommandoQueue.add(Bewegen(Punkt(0.0, 1000.0))) }
            }

            spielen(spiel)

            spiel.mensch.einheiten[1].punkt shouldBe testFall.ziel
        }
    }

    "attackmove laufen" - {
        data class TestFall(val start: Punkt, val ziel: Punkt)

        forAll(
            TestFall(start = Punkt(x = 1000.0, y = 0.0), ziel = Punkt(x = 999.6464466094068, y = 0.35355339059327373)),
            TestFall(start = Punkt(x = 0.0, y = 1000.0), ziel = Punkt(x = 0.0, y = 1000.0)),
            TestFall(start = Punkt(x = 0.0, y = 999.0), ziel = Punkt(x = 0.0, y = 999.5)),
            TestFall(start = Punkt(x = 0.0, y = 999.999), ziel = Punkt(x = 0.0, y = 1000.0))
        ) { testFall ->
            val spiel = neuesSpiel()
            spiel.mensch.apply {
                neueEinheit(x = testFall.start.x, y = testFall.start.y, einheitenTyp = infantrie).apply {
                    kommandoQueue.add(Attackmove(Punkt(0.0, 1000.0)))
                }
            }

            spielen(spiel)

            spiel.mensch.einheiten[1].punkt shouldBe testFall.ziel
        }
    }

    "attackmove angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                kommandoQueue.add(Attackmove(Punkt(0.0, 1000.0)))
            }
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]

        spielen(spiel)

        spiel.mensch.einheiten[1].punkt shouldBe Punkt(x = 0.0, y = 0.0)
        spiel.gegner.einheiten[1].leben shouldBeLessThan 100.0
    }

    "computer greift nächste einheit an" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 200.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.gegner.einheiten[1].punkt shouldBe Punkt(x = 0.0, y = 199.5)
    }

    "springen" - {
        data class TestFall(
            val start: Double,
            val ziel: Double,
            val leben: Double,
            val `alter cooldown`: Double,
            val `neuer cooldown`: Double
        )

        forAll(
            TestFall(start = 140.0, ziel = 40.0, leben = 25.0, `alter cooldown` = 0.0, `neuer cooldown` = 9.985),
            TestFall(start = 141.0, ziel = 140.5, leben = 100.0, `alter cooldown` = 0.0, `neuer cooldown` = 0.0),
            TestFall(start = 139.0, ziel = 40.0, leben = 25.0, `alter cooldown` = 0.0, `neuer cooldown` = 9.985),
            TestFall(start = 140.0, ziel = 139.5, leben = 100.0, `alter cooldown` = 34.0, `neuer cooldown` = 33.985)
        ) { testFall ->
            val spiel = neuesSpiel()

            spiel.gegner.apply {
                neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            }
            spiel.mensch.apply {
                neueEinheit(x = 0.0, y = testFall.start, einheitenTyp = berserker).apply {
                    typ.springen = 100
                    `springen cooldown` = testFall.`alter cooldown`
                }
            }
            spielen(spiel)

            spiel.mensch.einheiten[1].punkt shouldBe Punkt(x = 0.0, y = testFall.ziel)
            spiel.gegner.einheiten[1].leben shouldBe testFall.leben
            spiel.mensch.einheiten[1].`springen cooldown` shouldBe testFall.`neuer cooldown`
        }
    }

    "zu ziel laufen" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
            neueEinheit(x = 0.0, y = 1334.0, einheitenTyp = infantrie)
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = infantrie
            ).apply { kommandoQueue.add(Angriff(spiel.gegner.einheiten[2])) }
        }
        spielen(spiel)

        spiel.gegner.einheiten[1].leben shouldBe 100.0
        spiel.mensch.einheiten[1].punkt shouldBe Punkt(x = 0.0, y = 500.5)
    }

    "kommando entfernen (bewegen)" {
        val spiel = neuesSpiel()

        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply {
                kommandoQueue.add(
                    Bewegen(
                        Punkt(
                            0.0,
                            0.5
                        )
                    )
                )
            }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].kommandoQueue.size shouldBe 0
    }

    "kommando entfernen (attackmove)" {
        val spiel = neuesSpiel()

        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = infantrie
            ).apply { kommandoQueue.add(Attackmove(Punkt(0.0, 0.5))) }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].kommandoQueue.size shouldBe 0
    }

    "kommando entfernen (angreifen)" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply { leben = 0.1 }
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = infantrie
            ).apply { kommandoQueue.add(Angriff(spiel.gegner.einheiten[1])) }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].kommandoQueue.size shouldBe 0
    }

    "ziel angreifen" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 510.0, einheitenTyp = infantrie)
            neueEinheit(x = 0.0, y = 520.0, einheitenTyp = infantrie)
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = infantrie
            ).apply {
                kommandoQueue.add(Angriff(spiel.gegner.einheiten[2]))
            }
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]
        spielen(spiel)

        spiel.gegner.einheiten[2].leben shouldBeLessThan 100.0
    }

    "höchste Priorität die in reichweite ist angreifen" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 510.0, einheitenTyp = arbeiter)
            neueEinheit(x = 0.0, y = 520.0, einheitenTyp = berserker)
            neueEinheit(x = 0.0, y = 530.0, einheitenTyp = berserker)
            neueEinheit(x = 0.0, y = 660.0, einheitenTyp = infantrie)
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = jäger
            )
        }
        spiel.mensch.einheiten[1].letzterAngriff = spiel.gegner.einheiten[1]
        spielen(spiel)

        spiel.gegner.einheiten.map { it.leben } shouldBe listOf(3000.0, 80.0, 182.0, 200.0, 100.0)
    }

    "gebäudeBauenKI" {
        val spiel = neuesSpiel()

        spiel.gegner.apply { kristalle = 1500.0 }

        spielen(spiel)
        spiel.gegner.gebäude.values.single().typ shouldBe fabrik
    }

    "zu Einheit mit höchster Priorität laufen" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 510.0, einheitenTyp = arbeiter)
            neueEinheit(x = 0.0, y = 660.0, einheitenTyp = panzer)
        }
        spiel.mensch.apply {
            neueEinheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = infantrie
            )
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].punkt.y shouldBe 500.5
    }

    "kann Lufteinheit nicht angreifen" {
        val spiel = neuesSpiel()

        spiel.gegner.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = panzer)
        }
        spiel.mensch.apply {
            neueEinheit(x = 0.0, y = 0.0, einheitenTyp = jäger)
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 80.0
    }

    "gegenüberliegenden punkt finden" - {
        data class TestFall(val einheit: Punkt, val gegner: Punkt, val entfernung: Double, val ergebnis: Punkt)

        forAll(
            TestFall(
                einheit = Punkt(x = 0.0, y = 0.0), gegner = Punkt(x = 1.0, y = 1.0),
                entfernung = sqrt(2.0), ergebnis = Punkt(-1.0, -1.0)
            ),
            TestFall(
                einheit = Punkt(x = 1.0, y = 1.0), gegner = Punkt(x = 0.0, y = 0.0),
                entfernung = sqrt(2.0), ergebnis = Punkt(2.0, 2.0)
            ),
            TestFall(
                einheit = Punkt(x = 1.0, y = 1.0), gegner = Punkt(x = 0.0, y = 0.0),
                entfernung = 2 * sqrt(2.0), ergebnis = Punkt(3.0, 3.0)
            ),
        ) { testFall ->

            gegenüberliegendenPunktFinden(
                testFall.einheit,
                testFall.gegner,
                testFall.entfernung
            ) shouldBe testFall.ergebnis
        }
    }

    "Einheiten Mittelpunkt finden" - {
        data class TestFall(val einheiten: List<Punkt>, val ergebnis: Punkt)

        forAll(
            TestFall(
                einheiten = listOf(Punkt(0.0, 0.0), Punkt(2.0, 2.0), Punkt(2.0, 0.0)),
                ergebnis = Punkt(4.0 / 3.0, 2.0 / 3.0),
            ),
        ) { testFall ->
            val spiel = neuesSpiel()

            val einheiten = spiel.gegner.run {
                testFall.einheiten.map { neueEinheit(it, einheitenTyp = panzer) }
            }

            einheitenMittelpunkt(einheiten) shouldBe testFall.ergebnis
        }
    }
})

private fun spielen(spiel: Spiel) {
    spiel.runde()
}

private fun neuesSpiel(): Spiel {
    val computer = Spieler(
        kristalle = 0.0,
        minen = 0,
        startpunkt = Punkt(x = 900.0, y = 115.0),
        farbe = Color.RED,
        spielerTyp = SpielerTyp.computer,
        upgrades = SpielerUpgrades(
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            schadensUpgrade = 0,
            panzerungsUprade = 0
        )
    ).apply {
        neueEinheit(x = 900.0, y = 970.0, einheitenTyp = gebäudeEinheitenTyp(basis))
    }

    val mensch = Spieler(
        kristalle = 0.0,
        minen = 0,
        startpunkt = Punkt(x = 900.0, y = 905.0),
        farbe = Color.BLUE,
        spielerTyp = SpielerTyp.mensch,
        upgrades = SpielerUpgrades(
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            schadensUpgrade = 0,
            panzerungsUprade = 0
        )
    ).apply {
        neueEinheit(x = 900.0, y = 970.0, einheitenTyp = gebäudeEinheitenTyp(basis))
    }

    return Spiel(mensch, computer, rundenLimit = 1, multiplayer = Multiplayer(null, null))
}
