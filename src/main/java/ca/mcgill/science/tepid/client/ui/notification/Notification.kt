package ca.mcgill.science.tepid.client.ui.notification

import ca.mcgill.science.tepid.utils.WithLogging
import java.awt.*
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JWindow

class NotificationWindow(val w: Int = WIDTH,
                         val h: Int = HEIGHT,
                         val p: Int = PADDING) {

    private val queue: Deque<Notification> = ConcurrentLinkedDeque()
    private val window = JWindow()
    private var closeHover = false
    private var quotaMode = false
    private var closed = false

    private var icon: BufferedImage? = null
    private var oldIcon: BufferedImage? = null

    private var color = Color.BLACK
    private var oldColor = null

    private var title: String = ""
    private var body: String = ""
    private var oldTo = ""
    private var newFrom = ""


    private var y = h

    init {
        val screenBounds: Rectangle =
                window.graphicsConfiguration.device.defaultConfiguration.bounds
        window.apply {
            bounds = Rectangle(screenBounds.width - w - p,
                    screenBounds.height - h - p - 40,
                    w,
                    h)
        }

    }

    inner class NotificationBase : JPanel() {
        val quotaFont = font.deriveFont(35f).deriveFont(Font.BOLD)
        val titleFont = font.deriveFont(18f)
        val bodyFont = font.deriveFont(12f)

        val xButton = resourceImage("x.png")
        val xButtonHover = resourceImage("x_hover.png")

        override fun paint(g: Graphics) {
            super.paint(g)

            (g as? Graphics2D)?.apply {
                setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON)
                font = titleFont
                val titleX = height + p // todo check

            } ?: log.error("Graphics is not an instance of Graphics2D")
        }
    }

    companion object : WithLogging() {
        const val GREEN = 0x4D983E
        const val YELLOW = 0xFFB300
        const val RED = 0xFF4033

        private const val WIDTH = 360
        private const val HEIGHT = 90
        private const val PADDING = 10


        fun combineColors(a: Int, b: Int): Int {
            val aA = a shr 24 and 0xff
            val rA = a shr 16 and 0xff
            val gA = a shr 8 and 0xff
            val bA = a and 0xff
            val aB = b shr 24 and 0xff
            val rB = b shr 16 and 0xff
            val gB = b shr 8 and 0xff
            val bB = b and 0xff
            val rOut = rA * aA / 255 + rB * aB * (255 - aA) / (255 * 255)
            val gOut = gA * aA / 255 + gB * aB * (255 - aA) / (255 * 255)
            val bOut = bA * aA / 255 + bB * aB * (255 - aA) / (255 * 255)
            val aOut = aA + aB * (255 - aA) / 255
            return aOut and 0xff shl 24 or
                    (rOut and 0xff shl 16) or
                    (gOut and 0xff shl 8) or
                    (bOut and 0xff)
        }

        fun resourceImage(name: String): BufferedImage? = try {
            ImageIO.read(this::class.java.classLoader.getResourceAsStream(name))
        } catch (e: IOException) {
            log.error("Failed to load image", e); null
        }
    }

}