@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis", "LiftReturnOrAssignment")

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.WindowedMean
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt


data class EinheitenTyp(
    val schaden: Int,
    val reichweite: Int,
    val leben: Int,
    val laufweite: Double,
    val kristalle: Int
)

data class Einheit(
    val reichweite: Int,
    var leben: Int,
    val schaden: Int,
    var x: Double,
    var y: Double,
    val laufweite: Double,
    val bild: Texture = einheitenBild(),
//    val lebenText: Text = Text(),
    var ziel: Einheit? = null,
    var zielPunkt: Punkt? = null
//    ,
//    var zielpunktkreis: Arc? = null,
//    var zielpunktLinie: Line? = null
) {
    fun punkt() = Punkt(x = x, y = y)
}

data class Punkt(
    var x: Double,
    var y: Double
)

data class Spieler(
    val einheiten: MutableList<Einheit>,
    var kristalle: Int,
    var angriffspunkte: Int,
    var verteidiegungspunkte: Int,
    var minen: Int,
    var startpunkt: Punkt,
//    val farbe: Color,
    val mensch: Boolean
//    ,
//    val kristalleText: Text = Text()
)


@Suppress("SpellCheckingInspection")
class DesktopLauncher {
    companion object {
        @JvmStatic
        fun main(arg: Array<String>) {
            val config = LwjglApplicationConfiguration()
            config.title = "StartCarft III"
            config.width = 800
            config.height = 480
            LwjglApplication(Main(), config)
        }

    }
}

fun einheitenBild(): Texture {
    return Texture(Gdx.files.internal("panzer_oben.png"))
}


class Main : ApplicationAdapter() {

    lateinit var renderer: ShapeRenderer

    val infantrie = EinheitenTyp(reichweite = 100, leben = 50, schaden = 1, laufweite = 8.0, kristalle = 50)
    val berserker = EinheitenTyp(reichweite = 20, leben = 100, schaden = 2, laufweite = 10.0, kristalle = 80)
    val panzer = EinheitenTyp(reichweite = 500, leben = 500, schaden = 5, laufweite = 5.0, kristalle = 250)

    lateinit var computer: Spieler

    private fun einheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) =
        Einheit(reichweite = einheitenTyp.reichweite,
            leben = einheitenTyp.leben,
            schaden = einheitenTyp.schaden,
            x = x,
            y = y, laufweite = einheitenTyp.laufweite)


    lateinit var mensch: Spieler
//    val box: Pane = Pane().apply {
//        prefWidth = 1850.0
//        prefHeight = 950.0
//    }

    var ausgewaehlt: Einheit? = null

    lateinit var cam: OrthographicCamera
    internal var physicsMean = WindowedMean(10)
    internal var renderMean = WindowedMean(10)
    internal var startTime = TimeUtils.nanoTime()
//
//    override fun create() {
//        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
//        renderer = GLFieldRenderer()
//        field = Field()
//        field.resetForLevel(level)
//        Gdx.input.inputProcessor = object : InputAdapter() {
//            override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
//                field.removeDeadBalls()
//                if (field.getBalls().size != 0) field.setAllFlippersEngaged(true)
//                return false
//            }
//
//            override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
//                field.removeDeadBalls()
//                if (field.getBalls().size == 0) field.launchBall()
//                field.setAllFlippersEngaged(false)
//                return false
//            }
//
//            override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
//                if (field.getBalls().size != 0) field.setAllFlippersEngaged(true)
//                return false
//            }
//        }
//    }

    override fun resume() {

    }
//
//    override fun render() {
//        val gl = Gdx.gl
//
//        val startPhysics = TimeUtils.nanoTime()
//        field.tick((Gdx.graphics.deltaTime * 3000).toLong(), 4)
//        physicsMean.addValue((TimeUtils.nanoTime() - startPhysics) / 1000000000.0f)
//
//        gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
//        cam.viewportWidth = field.getWidth()
//        cam.viewportHeight = field.getHeight()
//        cam.position.set(field.getWidth() / 2, field.getHeight() / 2, 0f)
//        cam.update()
//        renderer.setProjectionMatrix(cam.combined)
//
//        val startRender = TimeUtils.nanoTime()
//        renderer.begin()
//        val len = field.getFieldElements().size
//        for (i in 0 until len) {
//            val element = field.getFieldElements().get(i)
//            element.draw(renderer)
//        }
//        renderer.end()
//
//        renderer.begin()
//        field.drawBalls(renderer)
//        renderer.end()
//        renderMean.addValue((TimeUtils.nanoTime() - startRender) / 1000000000.0f)
//
//        if (TimeUtils.nanoTime() - startTime > 1000000000) {
//            Gdx.app.log("Bouncy", "fps: " + Gdx.graphics.framesPerSecond + ", physics: " + physicsMean.mean * 1000
//                + ", rendering: " + renderMean.mean * 1000)
//            startTime = TimeUtils.nanoTime()
//        }
//    }

//    var auswahlKreis: Arc = kreis(x = -100.0, y = -100.0, radaius = 15.0)

    //    private fun kreis(x: Double, y: Double, radaius: Double): Arc {
//        return Arc().apply {
//            centerX = x
//            centerY = y
//            radiusX = radaius
//            radiusY = radaius
//            fill = Color.TRANSPARENT
//            stroke = Color.BLACK
//            type = ArcType.OPEN
//            length = 360.0
//        }
//    }
//
//    private val SCALE = 2.0f
//    val PIXEL_PER_METER = 32f
//    private val TIME_STEP = 1 / 60f
//    private val VELOCITY_ITERATIONS = 6
//    private val POSITION_ITERATIONS = 2
//    private val VELOCITY_Y = -9.85f
//    private val VELOCITY_X = 0f
//    private var orthographicCamera: OrthographicCamera? = null
//    private var box2DDebugRenderer: Box2DDebugRenderer? = null
//    private var world: World? = null
//    private var player: Player? = null
    lateinit var batch: SpriteBatch
//    private var texture: Texture? = null                                   2

    //
//    override fun create() {
//        orthographicCamera = OrthographicCamera()
//        orthographicCamera.setToOrtho(false, Gdx.graphics.width / SCALE, Gdx.graphics.height / SCALE)
//        world = World(Vector2(VELOCITY_X, VELOCITY_Y), false)
//        batch = SpriteBatch()
//        texture = Texture(Player.PLAYER_IMG_PATH)
//        box2DDebugRenderer = Box2DDebugRenderer()
//        player = Player(world)
//    }
//
    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()
        male()
        batch.end()
    }
//
//    private fun update() {
//        world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
//        cameraUpdate()
//        batch.setProjectionMatrix(orthographicCamera.combined)
//    }
//
//    private fun cameraUpdate() {
//        val position = orthographicCamera.position
//        position.x = player.getBody().getPosition().x * PIXEL_PER_METER
//        position.y = player.getBody().getPosition().y * PIXEL_PER_METER
//        orthographicCamera.position.set(position)
//        orthographicCamera.update()
//    }
//
//    override fun resize(width: Int, height: Int) {
//        orthographicCamera.setToOrtho(false, width / SCALE, height / SCALE)
//    }
//
//    override fun dispose() {
//        texture.dispose()
//        batch.dispose()
//        box2DDebugRenderer.dispose()
//        world.dispose()
//    }

    override fun create() {
        batch = SpriteBatch()
//        box.children.add(auswahlKreis)

//        val world = World(Vector2(VELOCITY_X, VELOCITY_Y), false)

        mensch = Spieler(einheiten = mutableListOf(
                einheit(x = 900.0, y = 950.0, einheitenTyp = infantrie),
                einheit(x = 450.0, y = 950.0, einheitenTyp = berserker),
                einheit(x = 100.0, y = 950.0, einheitenTyp = panzer)
            ),
                kristalle = 0,
                angriffspunkte = 20,
                verteidiegungspunkte = 10,
                minen = 3, startpunkt = Punkt(x = 925.0, y = 950.0),
        //        farbe = Color.BLUE,
                mensch = true)

        computer = Spieler(einheiten = mutableListOf(
            einheit(x = 900.0, y = 10.0, einheitenTyp = infantrie),
            einheit(x = 450.0, y = 10.0, einheitenTyp = berserker),
            einheit(x = 100.0, y = 10.0, einheitenTyp = panzer)
        ),
            kristalle = 0,
            angriffspunkte = 20,
            verteidiegungspunkte = 10,
            minen = 3,
            startpunkt = Punkt(x = 925.0, y = 10.0),
            //        farbe = Color.RED,
            mensch = false
        )

        initSpieler(computer)
        initSpieler(mensch)

//        mensch.kristalleText.x = 1800.0
//        mensch.kristalleText.y = 30.0

//        val vBox = VBox(10.0)
//
//        val scene = Scene(vBox, 1850.0, 1000.0)
//        scene.fill = null
//
//        val hBox = HBox()
//        hBox.children.add(Button("Panzer").apply {
//            onMouseClicked = EventHandler {
//                produzieren(spieler = mensch, einheitenTyp = panzer)
//            }
//        })
//        hBox.children.add(Button("berserker").apply {
//            onMouseClicked = EventHandler {
//                produzieren(spieler = mensch, einheitenTyp = berserker)
//            }
//        })
//        hBox.children.add(Button("infantrie").apply {
//            onMouseClicked = EventHandler {
//                produzieren(spieler = mensch, einheitenTyp = infantrie)
//            }
//        })
//
//        vBox.children.add(box)
//        vBox.children.add(hBox)
//
//        box.onMouseClicked = EventHandler { event ->
//            ausgewaehlt?.let { einheit ->
//                if (einheit.leben >= 1) {
//                    laufBefehl(einheit, event)
//                }
//            }
//
//        }
//
//        Thread(Runnable {
//            while (true) {
//                Platform.runLater {
//                    runde()
//                    male()
//                }
//                Thread.sleep(200)
//            }
//        }).start()
    }

//    private fun laufBefehl(einheit: Einheit, event: MouseEvent) {
//        einheit.zielPunkt = Punkt(x = event.x, y = event.y)
//        einheit.ziel = null
//
//        val z = einheit.zielpunktkreis
//        if (z != null) {
//            box.children.remove(z)
//        }
//        einheit.zielpunktLinie?.let { box.children.remove(it) }
//
//        einheit.zielpunktkreis = kreis(x = event.x, y = event.y, radaius = 5.0).apply {
//            box.children.add(this)
//        }
//        einheit.zielpunktLinie = Line().apply {
//            startX = einheit.x
//            startY = einheit.y
//            endX = event.x
//            endY = event.y
//            box.children.add(this)
//        }
//    }

    private fun initSpieler(spieler: Spieler) {
        spieler.einheiten.forEach {
            bildHinzufuegen(spieler, it)
        }
//        box.children.add(spieler.kristalleText)
    }

    private fun bildHinzufuegen(spieler: Spieler, einheit: Einheit) {
//        box.children.add(einheit.bild)
//        box.children.add(einheit.lebenText)

//        val imageView = einheit.bild.children.first()
//        val imageView = einheit.bild

//        if (spieler.mensch) {
//            imageView.onMouseClicked = EventHandler { event ->
//                if (einheit.leben > 0) {
//                    ausgewaehlt = einheit
//                    event.consume()
//                }
//            }
//        } else {
//            imageView.onMouseClicked = EventHandler { event ->
//                ausgewaehlt?.let {
//                    if (einheit.leben > 0) {
//                        it.ziel = einheit
//                        it.zielPunkt = null
//                        event.consume()
//                    }
//                }
//            }
//        }
    }

    private fun runde() {
        computer.kristalle += 1
        mensch.kristalle += 1
        produzieren(spieler = computer, einheitenTyp = panzer)
        bewegeSpieler(computer, mensch)
        bewegeSpieler(mensch, computer)

        schiessen(computer, mensch)
        schiessen(mensch, computer)

        computer.einheiten.toList().forEach { entfernen(it, computer) }
        mensch.einheiten.toList().forEach { entfernen(it, mensch) }

//        if (computer.einheiten.isEmpty()) {
//            box.children.add(Text("Sieg").apply {
//                x = 700.0
//                y = 500.0
//                font = Font(200.0)
//            })
//        }
//        if (mensch.einheiten.isEmpty()) {
//            box.children.add(Text("Game Over").apply {
//                x = 300.0
//                y = 500.0
//                font = Font(200.0)
//            })
//        }
    }

    private fun schiessen(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach {
            val ziel = zielauswaehlen(gegner, it)
            if (ziel != null) {
                schiessen(it, ziel)
            }
        }
    }

    fun produzieren(spieler: Spieler, einheitenTyp: EinheitenTyp) {
        if (spieler.kristalle >= einheitenTyp.kristalle) {
            val einheit = einheit(
                x = spieler.startpunkt.x,
                y = spieler.startpunkt.y,
                einheitenTyp = einheitenTyp)
            spieler.einheiten.add(einheit)
            spieler.kristalle -= einheitenTyp.kristalle
            bildHinzufuegen(spieler, einheit)
        }
    }

    fun bewegeSpieler(spieler: Spieler, gegner: Spieler) {
        spieler.einheiten.forEach { bewege(it, gegner) }
    }

    fun bewege(einheit: Einheit, gegner: Spieler) {
        val ziel = zielpunktAuswaehlen(gegner, einheit)
        if (ziel == null) {
            return
        }

        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x
        val A = entfernung(einheit, ziel)

        val reichweite = if (zielauswaehlen(gegner, einheit) != null) einheit.reichweite else 0
        if (A - reichweite >= 1) {
            einheit.x += smaller(b, b * einheit.laufweite / A)
            einheit.y += smaller(a, a * einheit.laufweite / A)
        }
    }

    fun smaller(a: Double, b: Double): Double {
        if (a.absoluteValue <= b.absoluteValue) {
            return a
        }
        return b
    }

    private fun zielpunktAuswaehlen(gegner: Spieler, einheit: Einheit): Punkt? {
        val ziel = zielauswaehlen(gegner, einheit)
        if (ziel != null) {
            return ziel.punkt()
        } else {
            return einheit.zielPunkt
        }
    }

    private fun zielauswaehlen(gegner: Spieler, einheit: Einheit): Einheit? {
        if (gegner.mensch) {
            return gegner.einheiten.minBy { entfernung(einheit, it.punkt()) }
        } else {
            return einheit.ziel
        }
    }

    private fun schiessen(einheit: Einheit, ziel: Einheit) {
        if (entfernung(einheit, ziel.punkt()) - einheit.reichweite < 1) {
            ziel.leben -= einheit.schaden
        }
    }

    private fun entfernen(einheit: Einheit, spieler: Spieler) {
//        if (einheit.leben < 1) {
//            spieler.einheiten.remove(einheit)
//            box.children.remove(einheit.bild)
//            box.children.remove(einheit.lebenText)
//            einheit.zielpunktLinie?.let { box.children.remove(it) }
//            einheit.zielpunktkreis?.let { box.children.remove(it) }
//        }
    }

    fun entfernung(einheit: Einheit, ziel: Punkt): Double {
        val a = ziel.y - einheit.y
        val b = ziel.x - einheit.x
        val A = sqrt(a.pow(2) + b.pow(2))

        return A
    }

    fun male() {
        computer.einheiten.toList().forEach { maleEinheit(it) }
        mensch.einheiten.toList().forEach { maleEinheit(it) }
//        mensch.kristalleText.text = mensch.kristalle.toString()

//        ausgewaehlt?.let {
//            auswahlKreis.centerX = it.bild.layoutX
//            auswahlKreis.centerY = it.bild.layoutY
//        }
    }

    fun maleEinheit(einheit: Einheit) {
        batch.draw(einheit.bild, 10f, 10f)

//        val kreis = einheit.bild

//        kreis.layoutX = einheit.x
//        kreis.layoutY = einheit.y

//        val lebenText = einheit.lebenText
//        lebenText.x = einheit.x
//        lebenText.y = einheit.y - 30
//        lebenText.text = einheit.leben.toString()

//        einheit.zielpunktLinie?.apply {
//            startX = einheit.x
//            startY = einheit.y
//        }
    }


}