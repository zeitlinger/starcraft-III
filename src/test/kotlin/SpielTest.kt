@file:Suppress("NonAsciiCharacters")

import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import javafx.scene.paint.Color

class SpielTest : FreeSpec({

    "Heiler soll automatisch heilen und Zustände entfernen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
                verlangsamt = 2.0
            }
            einheit(x = 20.0, y = 0.0, einheitenTyp = mSanitäter)
            vertärkteHeilmittel = true
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 12.0
        spiel.mensch.einheiten[1].verlangsamt shouldBe 0.0
    }

    "Heiler soll nur heilen wenn die Einheit nicht angegriffen wird" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                leben = 10.0
                zuletztGetroffen = 1.0
            }
            einheit(x = 20.0, y = 0.0, einheitenTyp = mSanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 10.0
    }

    "Einheit soll nur von einen Heiler geheilt werden" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
            }
            einheit(x = 20.0, y = 0.0, einheitenTyp = mSanitäter)
            einheit(x = 20.0, y = 0.0, einheitenTyp = mSanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 12.0
    }

    "nur ein Heiler geht zum Ziel" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                leben = 10.0
                zuletztGetroffen = 2.0
            }
            einheit(x = 200.0, y = 0.0, einheitenTyp = mSanitäter)
            einheit(x = 200.0, y = 0.0, einheitenTyp = mSanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[2].x shouldBe 199.3
        spiel.mensch.einheiten[3].x shouldBe 200.0
    }

    "beschützen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
            einheit(x = 300.0, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[2].punkt() shouldBe Punkt(x = 299.55278640450007, y = 0.22360679774997896)
    }

    "hold Position" {
        //todo sollte fehlschlagen
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 200.0, y = 0.0, einheitenTyp = mInfantrie)//.apply { kommandoQueue.add(Kommando.HoldPosition()) }
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x = 200.0, y = 0.0)
    }

    "Einheiten in Reichweite angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 999.125
        spiel.computer.einheiten[1].leben shouldBe 999.125
    }

    "Heilungsmodifikator" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie).apply { wirdGeheilt = 2 }
            strahlungsheilung = true
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 999.125
        spiel.computer.einheiten[1].leben shouldBe 999.3875
    }

    "vergiftung" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply { vergiftet = 1.0 }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 995.0
    }

    "verlangsamerung" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                verlangsamt = 1.0
                kommandoQueue.add(Kommando.Bewegen(Punkt(x = 0.5, y = 0.0)))
            }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].x shouldBe 0.25
    }

    "Nicht in einer Runde laufen und angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 150.5, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 1000.0
    }

    "Schusscooldown" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie.copy(schusscooldown = 0.03))
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.125

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.125

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 998.25
    }

    "Flächenschaden" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mPanzer)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
            einheit(x = 25.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 995.125
        spiel.computer.einheiten[2].leben shouldBe 995.125
    }

    "schadensupgrade" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
            schadensUpgrade = 1
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.025
    }

    "panzerugsupgrade" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
            panzerungsUprade = 1
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.225
    }

    "einheit stirbt" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie).apply { leben = 0.1 }
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = mInfantrie
            ).apply { kommandoQueue.add(Kommando.Angriff(spiel.computer.einheiten[1])) }
        }

        spielen(spiel)

        spiel.computer.einheiten.size shouldBe 1
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
            println(testFall.toString())

            val spiel = neuesSpiel()
            spiel.mensch.apply {
                einheit(
                    x = testFall.start.x,
                    y = testFall.start.y,
                    einheitenTyp = mInfantrie
                ).apply { kommandoQueue.add(Kommando.Bewegen(Punkt(0.0, 1000.0))) }
            }

            spielen(spiel)

            spiel.mensch.einheiten[1].punkt() shouldBe testFall.ziel
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
                einheit(x = testFall.start.x, y = testFall.start.y, einheitenTyp = mInfantrie).apply {
                    kommandoQueue.add(Kommando.Attackmove(Punkt(0.0, 1000.0)))
                }
            }

            spielen(spiel)

            spiel.mensch.einheiten[1].punkt() shouldBe testFall.ziel
        }
    }

    "attackmove angreifen" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                kommandoQueue.add(Kommando.Attackmove(Punkt(0.0, 1000.0)))
            }
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x = 0.0, y = 0.0)
        spiel.computer.einheiten[1].leben shouldBe 999.125
    }

    "computer greift nächste einheit an" {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 200.0, einheitenTyp = cInfantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].punkt() shouldBe Punkt(x = 0.0, y = 199.5)
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
            TestFall(start = 140.0, ziel = 40.0, leben = 988.0, `alter cooldown` = 0.0, `neuer cooldown` = 9.985),
            TestFall(start = 141.0, ziel = 140.5, leben = 1000.0, `alter cooldown` = 0.0, `neuer cooldown` = 0.0),
            TestFall(start = 139.0, ziel = 40.0, leben = 988.0, `alter cooldown` = 0.0, `neuer cooldown` = 9.985),
            TestFall(start = 140.0, ziel = 139.5, leben = 1000.0, `alter cooldown` = 34.0, `neuer cooldown` = 33.985)
        ) { testFall ->
            val spiel = neuesSpiel()

            spiel.computer.apply {
                einheit(x = 0.0, y = 0.0, einheitenTyp = cInfantrie)
            }
            spiel.mensch.apply {
                einheit(x = 0.0, y = testFall.start, einheitenTyp = mBerserker).apply {
                    typ.springen = 100
                    `springen cooldown` = testFall.`alter cooldown`
                }
            }
            spielen(spiel)

            spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x = 0.0, y = testFall.ziel)
            spiel.computer.einheiten[1].leben shouldBe testFall.leben
            spiel.mensch.einheiten[1].`springen cooldown` shouldBe testFall.`neuer cooldown`
        }
    }

    "zu ziel laufen" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = cInfantrie)
            einheit(x = 0.0, y = 1334.0, einheitenTyp = cInfantrie)
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = mInfantrie
            ).apply { kommandoQueue.add(Kommando.Angriff(spiel.computer.einheiten[2])) }
        }
        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 1000.0
        spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x = 0.0, y = 500.5)
    }

    "kommando entfernen (bewegen)" {
        val spiel = neuesSpiel()

        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mInfantrie).apply {
                kommandoQueue.add(
                    Kommando.Bewegen(
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
            einheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = mInfantrie
            ).apply { kommandoQueue.add(Kommando.Attackmove(Punkt(0.0, 0.5))) }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].kommandoQueue.size shouldBe 0
    }

    "kommando entfernen (angreifen)" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = cInfantrie).apply { leben = 0.1 }
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 0.0,
                einheitenTyp = mInfantrie
            ).apply { kommandoQueue.add(Kommando.Angriff(spiel.computer.einheiten[1])) }
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].kommandoQueue.size shouldBe 0
    }

    "ziel angreifen" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 510.0, einheitenTyp = cInfantrie)
            einheit(x = 0.0, y = 520.0, einheitenTyp = cInfantrie)
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = mInfantrie
            ).apply { kommandoQueue.add(Kommando.Angriff(spiel.computer.einheiten[2])) }
        }
        spielen(spiel)

        spiel.computer.einheiten[2].leben shouldBe 999.125
    }

    "höchste Priorität die in reichweite ist angreifen" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 510.0, einheitenTyp = cArbeiter)
            einheit(x = 0.0, y = 520.0, einheitenTyp = cBerserker)
            einheit(x = 0.0, y = 530.0, einheitenTyp = cBerserker)
            einheit(x = 0.0, y = 660.0, einheitenTyp = cInfantrie)
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = mJäger
            )
        }
        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 800
        spiel.computer.einheiten[2].leben shouldBe 1997.25
        spiel.computer.einheiten[3].leben shouldBe 2000
        spiel.computer.einheiten[4].leben shouldBe 1000
    }

    "zu Einheit mit höchste Priorität laufen" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 510.0, einheitenTyp = cArbeiter)
            einheit(x = 0.0, y = 660.0, einheitenTyp = cPanzer)
        }
        spiel.mensch.apply {
            einheit(
                x = 0.0,
                y = 500.0,
                einheitenTyp = mInfantrie
            )
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].y shouldBe 500.5
    }

    "kann Lufteinheit nicht angreifen" {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = cPanzer)
        }
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = mJäger)
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 800.0
    }

})

private fun spielen(spiel: Spiel) {
    spiel.runde()
}

private fun neuesSpiel(): Spiel {
    val computer = Spieler(
        kristalle = 0.0,
        angriffspunkte = 20,
        verteidiegungspunkte = 10,
        minen = 0,
        startpunkt = Punkt(x = 900.0, y = 115.0),
        farbe = Color.RED,
        mensch = false,
        schadensUpgrade = 0,
        panzerungsUprade = 0
    ).apply {
        einheit(x = 900.0, y = 970.0, einheitenTyp = cBasis)
    }

    val mensch = Spieler(
        kristalle = 10000000000000000000.0,
        angriffspunkte = 20,
        verteidiegungspunkte = 10,
        minen = 0,
        startpunkt = Punkt(x = 900.0, y = 905.0),
        farbe = Color.BLUE,
        mensch = true,
        schadensUpgrade = 0,
        panzerungsUprade = 0
    ).apply {
        einheit(x = 900.0, y = 970.0, einheitenTyp = mBasis)
    }

    return Spiel(mensch, computer, rundenLimit = 1)
}
