
import io.kotest.matchers.shouldBe
import javafx.scene.paint.Color
import org.junit.Test

class SpielTest {

    @Test
    fun `Heiler soll automatisch heilen`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply { leben = 10.0 }
            einheit(x = 20.0, y = 0.0, einheitenTyp = sanitäter)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 12.0
    }

    @Test
    fun `Beschützen`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            einheit(x = 300.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[2].punkt() shouldBe Punkt(x=299.55278640450007, y=0.22360679774997896)
    }

    @Test
    fun `Einheiten in Reichweite angreifen`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 999.125
        spiel.computer.einheiten[1].leben shouldBe 999.125
    }
    @Test
    fun `schadensupgrade`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            schadensUpgrade = 1
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.025
    }
    @Test
    fun `panzerugsupgrade`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
            panzerungsUprade = 1
        }

        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 999.225
    }
    @Test
    fun `einheit stirbt`() {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie).apply { leben = 0.1 }
        }
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply { ziel = spiel.computer.einheiten[1] }
        }

        spielen(spiel)

        spiel.computer.einheiten.size shouldBe 1
        spiel.mensch.einheiten[0].ziel shouldBe null
    }
    @Test
    fun `zum Zielpunkt laufen`() {
        data class TestFall(val start: Double, val ziel: Double)
        val parameter = listOf(
                TestFall(start = 140.0, ziel = 40.0),
                TestFall(start = 141.0, ziel = 140.5)
        )
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie).apply { zielPunkt = Punkt(3415.0, 21434.0) }
        }

        spielen(spiel)

        spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x=0.0786708845494479, y=0.49377210525120535)
    }
    @Test
    fun `computer greift nächste einheit an`() {
        val spiel = neuesSpiel()
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
        }
        spiel.computer.apply {
            einheit(x = 0.0, y = 200.0, einheitenTyp = infantrie)
        }

        spielen(spiel)

        spiel.computer.einheiten[1].punkt() shouldBe Punkt(x=0.0, y= 199.5)
    }
    @Test
    fun `springen`() {
        data class TestFall(val start: Double, val ziel: Double, val leben: Double, val `alter cooldown`: Int, val `neuer cooldown`: Int)
        val parameter = listOf(
                TestFall(start = 140.0, ziel = 40.0, leben = 936.125, `alter cooldown` = 0, `neuer cooldown` = 99),
                TestFall(start = 141.0, ziel = 140.5, leben = 1000.0, `alter cooldown` = 0, `neuer cooldown` = 0),
                TestFall(start = 139.0, ziel = 40.0, leben = 936.125, `alter cooldown` = 0, `neuer cooldown` = 99),
                TestFall(start = 140.0, ziel = 139.5, leben = 1000.0, `alter cooldown` = 34, `neuer cooldown` = 33)
        )
        parameter.forEach { testFall ->
            val spiel = neuesSpiel()

            spiel.computer.apply {
                einheit(x = 0.0, y = 0.0, einheitenTyp = infantrie)
            }
            spiel.mensch.apply {
                einheit(x = 0.0, y = testFall.start, einheitenTyp = berserker).apply { typ.springen = 100
                `springen cooldown` = testFall.`alter cooldown`}
            }
            spielen(spiel)

            spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x=0.0, y=testFall.ziel)
            spiel.computer.einheiten[1].leben shouldBe testFall.leben
            spiel.mensch.einheiten[1].`springen cooldown` shouldBe testFall.`neuer cooldown`
        }
    }

    @Test
    fun `zu ziel laufen`() {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 150.0, einheitenTyp = infantrie)
            einheit(x = 0.0,y = 1334.0, einheitenTyp = infantrie)
        }
        spiel.mensch.apply {
            einheit(x = 0.0, y = 500.0, einheitenTyp = infantrie).apply { ziel = spiel.computer.einheiten[2] }
        }
        spielen(spiel)

        spiel.computer.einheiten[1].leben shouldBe 1000.0
        spiel.mensch.einheiten[1].punkt() shouldBe Punkt(x=0.0, y=500.5)
    }

    @Test
    fun `ziel angreifen`() {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 510.0, einheitenTyp = infantrie)
            einheit(x = 0.0,y = 520.0, einheitenTyp = infantrie)
        }
        spiel.mensch.apply {
            einheit(x = 0.0, y = 500.0, einheitenTyp = infantrie).apply { ziel = spiel.computer.einheiten[2] }
        }
        spielen(spiel)

        spiel.computer.einheiten[2].leben shouldBe 999.125
    }

    @Test
    fun `kann Lufteinheit nicht angreifen`() {
        val spiel = neuesSpiel()

        spiel.computer.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = panzer)
        }
        spiel.mensch.apply {
            einheit(x = 0.0, y = 0.0, einheitenTyp = jäger)
        }
        spielen(spiel)

        spiel.mensch.einheiten[1].leben shouldBe 800.0
    }

}

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
        einheit(x = 900.0, y = 970.0, einheitenTyp = basis)
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
        einheit(x = 900.0, y = 970.0, einheitenTyp = basis)
    }

    return Spiel(mensch, computer, warteZeit = 0, rundenLimit = 1)
}