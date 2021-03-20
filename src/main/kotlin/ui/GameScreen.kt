package ui

import Einheit
import EinheitenTyp
import Punkt
import Spiel
import Spieler
import SpielerTyp
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import paxbritannica.fighter.Fighter
import späher

class GameScreen(val spiel: Spiel) : InputProcessor, Screen {

    val einheiten: MutableMap<SpielerTyp, MutableMap<Int, Sprite>> = mutableMapOf()
    val atlas = TextureAtlas(Gdx.files.internal("data/spritepack/packhigh.pack"))

    init {
        spiel.einheitProduziert = { einheitUiErstellen(it) }
//        initSpieler(gegner)
        initSpieler(spiel.mensch)

    }

    val Punkt.vec: Vector2
        get() = Vector2(this.x.toFloat(), this.y.toFloat())

    private fun einheitUiErstellen(einheit: Einheit) {
        val spieler = einheit.spieler

//        einheit.kuerzel = Text(einheit.typ.kuerzel)
//        einheit.lebenText = Text()
//        einheit.bild.fill = spieler.farbe

        val f = Fighter(einheit.nummer, einheit.punkt.vec, einheit.punkt.vec)


        val s = atlas.createSprite(einheit.typ.bild!!)
        einheiten
            .computeIfAbsent(spieler.spielerTyp) { mutableMapOf() }[einheit.nummer] = s
//        karte.add(einheit.bild)
//        karte.add(einheit.lebenText)
//        karte.add(einheit.kuerzel)

//        einheitMouseHandler(spieler, einheit.bild, einheit)
//        einheitMouseHandler(spieler, einheit.kuerzel!!, einheit)
    }

    private fun initSpieler(spieler: Spieler) {
        val spielerTyp = spieler.spielerTyp
        if (spieler == spiel.gegner && spielerTyp != SpielerTyp.computer) {
            //gegner schickt einheiten
            return
        }

//        plaziereGebäude(basis, Punkt(900.0, y = spieler.startpunkt.y - 60 * nachVorne(spielerTyp)), spieler)

        fun neueEinheit(x: Double, y: Double, einheitenTyp: EinheitenTyp) {
            spiel.neueEinheit(spieler, einheitenTyp, Punkt(x, y))
        }

        neueEinheit(x = 50.0, y = spieler.startpunkt.y, einheitenTyp = späher)
//        neueEinheit(x = 750.0, y = spieler.startpunkt.y, einheitenTyp = arbeiter)
//        neueEinheit(x = 850.0, y = spieler.startpunkt.y, einheitenTyp = infantrie)
//        neueEinheit(x = 950.0, y = spieler.startpunkt.y, einheitenTyp = infantrie)
    }

    //    BackgroundFXRenderer backgroundFX = new BackgroundFXRenderer();
    var gameBatch: SpriteBatch? = null
    var cam: OrthographicCamera
    private var width = 800
    private var height = 480
    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        cam = OrthographicCamera(width.toFloat(), height.toFloat())
        cam.position.x = 400f
        cam.position.y = 240f
        cam.update()
        //        backgroundFX.resize(width, height);
        gameBatch = SpriteBatch()
        gameBatch!!.projectionMatrix.set(cam.combined)
    }

    override fun show() {}
    override fun render(delta: Float) {
        var delta = delta
        delta = Math.min(0.06f, delta)


//        backgroundFX.render();

//        Collision.collisionCheck();
        gameBatch!!.begin()
        // Bubbles
//        GameInstance.getInstance().bubbleParticles.draw(gameBatch);
//        GameInstance.getInstance().bigBubbleParticles.draw(gameBatch);

        einheiten.values.forEach {
            it.values.forEach { it.draw(gameBatch) }
        }

//        spiel.mensch.einheiten.forEach {

//            if (ship.alive) {
//                ship.draw(gameBatch);
//            } else {
//                GameInstance.getInstance().frigates.removeValue(ship, true);
//            }
//        }
        gameBatch!!.end()
    }

    override fun hide() {}
    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.BACK) {
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun pause() {}
    override fun resume() {}
    override fun dispose() {}

    init {
        Gdx.input.isCatchBackKey = true
        Gdx.input.inputProcessor = this
        cam = OrthographicCamera(width.toFloat(), height.toFloat())
        cam.position.x = 400f
        cam.position.y = 240f
        cam.update()
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
    }
}
