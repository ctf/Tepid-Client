package ca.mcgill.science.tepid.client.ui.notification

import ca.mcgill.science.tepid.utils.WithLogging
import com.io.jimm.StringUtils
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.LinkedBlockingQueue
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingUtilities

class NotificationWindow(val w: Int = WIDTH,
                         val h: Int = HEIGHT,
                         val p: Int = PADDING) : JWindow() {

    private val entries: BlockingQueue<NotificationModel> = LinkedBlockingQueue()
    private var closeHover = false
    // triggers safeRepaint on first switch
        set(value) {
            if (field == value) return
            field = value
            safeRepaint()
        }

    private var quotaMode = false
    private var _closed = false
    val closed: Boolean
        get() = _closed

    private var icon: BufferedImage? = null
    private var oldIcon: BufferedImage? = null

    private var notifColor = Color.BLACK
    private var oldNotifColor = notifColor

    private var title: String = ""
    private var body: String = ""
    private var oldTo = ""
    private var newFrom = ""

    private var animationThread: Thread? = null

    private var notifY: Double = h.toDouble()

    init {
        val screenBounds: Rectangle = graphicsConfiguration.device.defaultConfiguration.bounds
        bounds = Rectangle(screenBounds.width - w - p,
                screenBounds.height - h - p - 40,
                w,
                h)
        contentPane = NotificationPanel()
        opacity = 0.9f
    }

    private fun safeRepaint() {
        if (SwingUtilities.isEventDispatchThread()) {
            repaint()
        } else {
            SwingUtilities.invokeLater(this::repaint)
        }
    }

    private fun close() {
        _closed = true
        dispose()
    }

    private fun addNotification(notification: NotificationWindow) {
        if (queue.add(notification))
            reposition()
    }

    private fun removeNotification(notification: NotificationWindow) {
        if (queue.remove(notification))
            reposition()
    }

    private fun reposition() {
        if (queue.isEmpty()) return
        val bounds = queue.first.graphicsConfiguration.device.defaultConfiguration.bounds
        val p = 20
        var nY = bounds.height - 40 - p
        queue.forEach {
            val b = it.bounds
            nY -= b.height + p
            it.setBounds(b.x, nY, b.width, b.height)
        }
    }

    override fun setVisible(b: Boolean) {
        if (b) addNotification(this) else removeNotification(this)
        super.setVisible(b)
        if (b) startAnimationThread()
    }

    private fun animate(height: Int, fps: Int) {
        val frameMs: Int = 1000 / fps
        var carry: NotificationModel? = null
        loop@ while (!Thread.interrupted()) {
            var entry: NotificationModel = carry ?: entries.take()
            if (entry === carry)
                carry = null
            else if (entry is TransitionNotification && !quotaMode && icon != null) {
                carry = entry
                newFrom = entry.from.toString()
                entry = StateNotification(entry.title, entry.body,
                        quotaColor(entry.from.toFloat()), "")
            }
            if (title.isEmpty()) title = entry.title
            if (body.isEmpty()) body = entry.body
            when (entry) {
                is TransitionNotification -> {
                    quotaMode = true
                    oldIcon = null
                    icon = null
                    oldTo = entry.to.toString()
                }
                is StateNotification -> {
                    quotaMode = false
                    oldNotifColor = notifColor
                    notifColor = Color(entry.color)
                    oldIcon = icon
                    setIcon(entry.icon)
                    if (oldIcon == null && oldTo.isBlank()) {
                        oldIcon = icon
                        safeRepaint()
                        Thread.sleep(2000) // todo move elsewhere
                    }
                    continue@loop
                }
            }
            val fromY = (entry.from * height).toDouble()
            val toY = (entry.to * height).toDouble()
            val distY = Math.abs(toY - fromY)
            val start = System.currentTimeMillis()
            var t = 0.0
            while (t <= 1) {
                val frameStart = System.currentTimeMillis()
                t = (frameStart - start).toDouble() / ms
                val pos = easeInOut.calc(t)
                if (pos >= 0.5 && title != entry.title) title = entry.title
                if (pos >= 0.5 && body != entry.body) body = entry.body
                notifY = if (toY > fromY) pos * distY + fromY
                else distY - pos * distY + toY
                safeRepaint()
                val sleepTime = frameMs - (System.currentTimeMillis() - frameStart)
                if (sleepTime > 0) Thread.sleep(sleepTime)
            }
            oldIcon = icon
            newFrom = ""
            safeRepaint()
            Thread.sleep((frameMs * 2).toLong())
        }
    }

    private fun setIcon(icon: String) {
        this.icon = if (icon.isNotBlank()) resourceImage("icons/$icon.png")
        else null
    }

    fun setStatus(color: Int, icon: String, title: String, body: String) {
        entries.add(StateNotification(title, body, color, icon))
    }

    fun setQuota(from: Int, to: Int, title: String, body: String) {
        entries.add(TransitionNotification(title, body, from, to))
    }

    private fun startAnimationThread() {
        val height = height
        val fps = 60
        if (animationThread?.isAlive == true) return
        animationThread = Thread({
            try {
                animate(height, fps)
            } catch (e: Exception) {
                if (e !is InterruptedException)
                    log.error("Animation error", e)
            }
        }, "Animation")
        animationThread?.start()
    }

    inner class NotificationPanel : JPanel() {
        val quotaFont = font.deriveFont(35f).deriveFont(Font.BOLD)
        val titleFont = font.deriveFont(18f)
        val bodyFont = font.deriveFont(12f)

        val xButton = resourceImage("x.png")
        val xButtonHover = resourceImage("x_hover.png")

        init {
            addMouseMotionListener(object :MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    closeHover = e.x > width - 24 && e.y < 24
                }

            })
            addMouseListener(object : MouseAdapter() {

                override fun mouseExited(e: MouseEvent?) {
                    closeHover = false
                }

                override fun mouseReleased(e: MouseEvent?) {
                    if (closeHover) close()
                }
            })
        }

        override fun paint(g: Graphics) {
            super.paint(g)

            (g as? Graphics2D)?.apply {
                setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON)
                font = titleFont
                val titleX = height + p // todo check
                when {
                    quotaMode -> {
                        notifColor = Color(quotaColor(y.toFloat() / height), true)
                        this.color = notifColor
                    }
                    notifY >= height -> {
                        this.color = notifColor
                    }
                    else -> {
                        val newColor = ((1 - y.toFloat() / height) * 0xff).toInt() shl 24 or (oldNotifColor.rgb and 0xffffff)
                        this.color = Color(combineColors(newColor, notifColor.rgb), true)
                    }
                }
                drawString(title, titleX, 24)
                fillRect(0, 0, height, height)
                font = bodyFont
                this.color = Color.BLACK
                val lines = StringUtils.wrap(body, fontMetrics, width - height - p * 2)
                var bodyY = 45
                val lineHeight = fontMetrics.height
                lines.forEach {
                    drawString(it, titleX, bodyY)
                    bodyY += lineHeight
                }
                val highY: Int = (-notifY % height).toInt()
                val lowY: Int = highY + height
                if (quotaMode) {
                    this.color = Color.WHITE
                    font = quotaFont
                    val highT = Math.floor(notifY / height).toInt().toString()
                    draw(highT, highY)
                    val lowT = Math.ceil(notifY / height).toInt().toString()
                    draw(lowT, lowY)
                } else {
                    draw(oldIcon, oldTo, highY, Color.WHITE, quotaFont)
                    draw(icon, newFrom, lowY, Color.WHITE, quotaFont)
                }
                val closeIcon = if (closeHover) xButtonHover else xButton
                if (closeIcon != null)
                    drawImage(closeIcon, w - closeIcon.width - p, p, null)
            } ?: log.error("Graphics is not an instance of Graphics2D")
        }

        private fun Graphics2D.preserverTransform(action: Graphics2D.() -> Unit) {
            val currentTransform = transform
            action()
            transform = currentTransform
        }

        /**
         * Draws first of [icon] and [text] that is nonnull
         */
        private fun Graphics2D.draw(icon: BufferedImage?,
                                    text: String?,
                                    yOffset: Int,
                                    color: Color,
                                    font: Font) {
            if (icon != null) {
                draw(icon, yOffset)
            } else {
                preserverTransform {
                    this.color = color
                    this.font = font
                    val shape = font.createGlyphVector(fontRenderContext, text).outline
                    translate(shape.bounds, yOffset)
                    fill(shape)
                }
            }
        }

        private fun Graphics2D.draw(icon: BufferedImage, yOffset: Int) {
            preserverTransform {
                val bounds = Rectangle(0, 0, icon.width, icon.height)
                translate(bounds, yOffset)
                drawImage(icon, 0, 0, null)
            }
        }

        private fun Graphics2D.translate(bounds: Rectangle, yOffset: Int) {
            translate((height - bounds.width) / 2 - bounds.x,
                    -yOffset + (height - bounds.height) / 2 - bounds.y)
        }

        private fun Graphics2D.draw(text: String, yOffset: Int) {
            preserverTransform {
                val shape = font.createGlyphVector(fontRenderContext, text).outline
                translate(shape.bounds, yOffset)
                fill(shape)
            }
        }
    }

    companion object : WithLogging() {
        const val GREEN = 0x4D983E
        const val YELLOW = 0xFFB300
        const val RED = 0xFF4033

        private const val WIDTH = 360
        private const val HEIGHT = 90
        private const val PADDING = 10

        private const val ms = 2000.0
        private val easeInOut = CubicBezier.create(
                0.42, 0.0, 0.58, 1.0, 1000.0 / 60.0 / ms / 4.0)

        private val queue: Deque<NotificationWindow> = ConcurrentLinkedDeque()

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

        fun quotaColor(q: Float): Int {
            val distTo0 = ((Math.max(100 - q, 50.0f) - 50) / 50)
            val distTo50 = Math.min(((Math.max(150 - q, 50.0f) - 50) / 50), 1f)
            val green = -0x1000000 or GREEN
            val yellow = (distTo50 * 0xff).toInt() shl 24 or YELLOW
            val red = (distTo0 * 0xff).toInt() shl 24 or RED
            return combineColors(red, combineColors(yellow, green))
        }
    }

}