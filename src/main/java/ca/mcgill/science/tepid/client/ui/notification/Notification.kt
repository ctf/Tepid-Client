package ca.mcgill.science.tepid.client.ui.notification

import ca.mcgill.science.tepid.utils.WithLogging
import com.io.jimm.StringUtils
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingUtilities

/**
 * Singleton reference for [NotificationHandler]
 */
object Notifications : NotificationHandlerContract by NotificationHandler()

interface NotificationHandlerContract {
    /**
     * Send a new notification to display
     * Notifications are queued and animated into display, granted its bounded window is not disposed
     * Any notification sent after a window's disposal will be displayed in a new window
     */
    fun notify(model: NotificationModel)

    /**
     * Close the current window (if any) and dispose any of its associated notifications
     */
    fun close()

    /**
     * Get the current thread running the animations, if any
     */
    val thread: Thread?
}

/**
 * Parent class that will handle all notifications
 * Only one instance is needed
 */
class NotificationHandler : NotificationHandlerContract, WithLogging() {

    var window: NotificationWindow? = null

    private var eventReceiver = object : NotificationEventReceiver {
        override fun onDispose(id: Long) {
            synchronized(this@NotificationHandler) {
                if (window?.id == id)
                    window = null
            }
        }
    }

    @Synchronized
    override fun notify(model: NotificationModel) {
        if (window?.disposed != false) {
            log.info("Creating new notification window")
            window = NotificationWindow(eventReceiver = eventReceiver)
        }
        window?.notify(model)
    }

    override val thread: Thread?
        @Synchronized
        get() = window?.thread

    @Synchronized
    override fun close() {
        window?.dispose()
        window = null
    }


}

/**
 * Helper callback to relay events for better memory management
 */
interface NotificationEventReceiver {
    fun onDispose(id: Long)
}

/**
 * JWindow that handles notification displays
 * Note that as soon as no notifications are queued or the window is closed,
 * the window becomes invalidated
 * See [NotificationHandler] for a singleton wrapper implementation
 */
class NotificationWindow(val id: Long = System.currentTimeMillis(),
                         val w: Int = WIDTH,
                         val h: Int = HEIGHT,
                         val p: Int = PADDING,
                         val eventReceiver: NotificationEventReceiver? = null) {

    private val window: JWindow = JWindow()

    private val entries: BlockingQueue<NotificationModel> = LinkedBlockingQueue()
    private var closeHover = false
    // triggers safeRepaint on first switch
        set(value) {
            if (field == value) return
            field = value
            safeRepaint()
        }

    private var quotaMode = false
    private var _disposed = false

    /**
     * Checks if a window can still be used
     */
    val disposed: Boolean
        get() = _disposed

    private var icon: BufferedImage? = null
    private var oldIcon: BufferedImage? = null

    private var notifColor = Color.BLACK
    private var oldNotifColor = notifColor

    private var title: String = ""
    private var body: String = ""
    private var oldTo = ""
    private var newFrom = ""

    private var oldEntry: NotificationModel? = null
    private var entry: NotificationModel? = null

    val thread: Thread = getAnimationThread()

    private var notifY: Double = h.toDouble()

    init {
        window.apply {
            val screenBounds: Rectangle = graphicsConfiguration.device.defaultConfiguration.bounds
            bounds = Rectangle(screenBounds.width - w - p,
                    screenBounds.height - h - p - 40,
                    w,
                    h)
            log.info("Bounds $bounds")
            contentPane = NotificationPanel()
            opacity = 0.9f
            isVisible = true
        }
    }

    private fun safeRepaint() {
        if (SwingUtilities.isEventDispatchThread()) {
            window.repaint()
        } else {
            SwingUtilities.invokeLater(window::repaint)
        }
    }

    fun dispose() = dispose(false)

    /**
     * Removes any pending entries and stops the animation thread
     */
    @Synchronized
    private fun dispose(notify: Boolean) {
        if (_disposed) return
        _disposed = true
        entries.clear()
        thread.interrupt()
        window.dispose()
        if (notify)
            eventReceiver?.onDispose(id)
    }

    private fun animate(height: Int, fps: Int) {
        val frameMs: Int = 1000 / fps
        var carry: NotificationModel? = null
        loop@ while (!Thread.currentThread().isInterrupted && !_disposed) {
            var entry: NotificationModel = carry ?: entries.take()
            if (entry === carry)
                carry = null
            else if (entry is TransitionNotification && !quotaMode && icon != null) {
                carry = entry
                newFrom = entry.from.toString()
                entry = StateNotification(entry.title, entry.body,
                        quotaColor(entry.from.toFloat()), NotificationIcon.NONE)
            }
            if (entry !== carry)
                log.debug("Showing notification $entry")
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
                    icon = entry.icon.image()
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

    fun notify(model: NotificationModel) {
        if (_disposed) {
            log.error("Cannot show notification in disposed window")
            return
        }
        entries.add(model)
        if (!thread.isAlive)
            thread.start()
    }

    private fun getAnimationThread(): Thread {
        val height = window.height
        return Thread({
            log.info("Starting animation")
            try {
                window.isVisible = true
                animate(height, FPS)
            } catch (e: Exception) {
                if (e !is InterruptedException)
                    log.error("Animation error", e)
            }
            log.info("Finished animation")
            dispose(true)
        }, "Animation")
    }

    inner class NotificationPanel : JPanel() {
        val quotaFont = font.deriveFont(35f).deriveFont(Font.BOLD)
        val titleFont = font.deriveFont(18f)
        val bodyFont = font.deriveFont(12f)

        val xButton = resourceImage("x.png")
        val xButtonHover = resourceImage("x_hover.png")

        init {
            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    closeHover = e.x > width - 24 && e.y < 24
                }

            })
            addMouseListener(object : MouseAdapter() {

                override fun mouseExited(e: MouseEvent?) {
                    closeHover = false
                }

                override fun mouseReleased(e: MouseEvent?) {
                    if (closeHover) dispose()
                }
            })
        }

        private fun Graphics2D.draw(model: NotificationModel) {
            when (model) {
                is StateNotification -> {

                }
                is TransitionNotification -> {

                }
            }
        }

        private fun updateNotifColor(): Color = when {
            quotaMode -> {
                notifColor = Color(quotaColor(y.toFloat() / height), true)
                notifColor
            }
            notifY >= height -> {
                notifColor
            }
            else -> {
                val newColor = ((1 - y.toFloat() / height) * 0xff).toInt() shl 24 or (oldNotifColor.rgb and 0xffffff)
                Color(combineColors(newColor, notifColor.rgb), true)
            }
        }

        override fun paint(g: Graphics) {
            super.paint(g)

            (g as? Graphics2D)?.apply {
                setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON)
                font = titleFont

                // background
                this.color = updateNotifColor()
                // title
                val titleX = height + p // todo check
                drawString(title, titleX, 24)
                fillRect(0, 0, height, height)
                // body
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

                // per model
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

        private const val FPS = 60
        private const val ms = 2000.0
        private val easeInOut = CubicBezier.create(
                0.42, 0.0, 0.58, 1.0, 1000.0 / 60.0 / ms / 4.0)

        /**
         * Merges two colors based on their alpha values
         * Order does not matter
         */
        fun combineColors(c1: Int, c2: Int): Int {
            val alpha1 = c1 shr 24 and 0xff
            val alpha2 = c2 shr 24 and 0xff

            // bit shift color parts and combine
            val (r, g, b) = arrayOf(16, 8, 0).map { bit -> { color: Int -> color shr bit and 0xff } }.map {
                it(c1) * alpha1 / 255 + it(c2) * alpha2 * (255 - alpha1) / (255 * 255)
            }

            val a = alpha1 + alpha2 * (255 - alpha1) / 255

            // bit shift again and fold with 'or'
            return arrayOf(a, r, g, b).reversedArray().foldIndexed(0) { i, color, part ->
                color or (part and 0xff shl (i * 8))
            }
        }

        /**
         * Blend two colors together, where [ratio] is the weighting on [color2]
         * For example, a ratio of 0 would equal [color1], and a ratio of 1 would equal [color2]
         *
         * Everything, including the alpha, will be weighted and outputted as a new [Color]
         */
        fun blend(color1: Color, color2: Color, ratio: Float): Color {
            if (ratio !in 0..1)
                throw IllegalArgumentException("Blend ratio must be between 0.0f and 1.0f; currently $ratio")
            /**
             * Maps a component retrieving function and applies it to both colors with the ratio
             * Note that a * (1 - r) + b * r = a + (b - a) * r
             */
            val (r, g, b, a) = arrayOf<(Color) -> Int>({ it.red }, { it.green }, { it.blue }, { it.alpha }).map {
                (it(color1) + (it(color2) - it(color1)) * ratio).toInt()
            }
            return Color(r, g, b, a)
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