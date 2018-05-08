package ca.mcgill.science.tepid.client.lpd

import ca.mcgill.science.tepid.models.data.PrintJob
import ca.mcgill.science.tepid.utils.WithLogging
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.*

class LPDServer(port: Int) : WithLogging(), Closeable {
    private var closed: Boolean = false

    init {
        log.info("Creating LPDServer in port $port")
    }

    private val serverSock: ServerSocket = ServerSocket(port, 0, InetAddress.getByName(null))
    private val listeners = ConcurrentLinkedQueue<(PrintJob, InputStream) -> Unit>()
    private val executor: ExecutorService = ThreadPoolExecutor(1, 10, 1, TimeUnit.MINUTES,
            ArrayBlockingQueue<Runnable>(30, true))

    fun start() {
        while (!closed) {
            try {
                LPDClientHandler(serverSock.accept(), this@LPDServer).start()
                log.info("New lpd")
            } catch (e: IOException) {
                log.error(e)
            }
        }
    }

    @Synchronized
    override fun close() {
        this.closed = true
        log.info("Closing LPDServer")
        try {
            listeners.clear()
            serverSock.close()
        } catch (e: IOException) {
            log.error("Failed to dispose", e)
        }
    }

    fun addJobListener(listener: (job: PrintJob, input: InputStream) -> Unit) {
        listeners.add(listener)
    }

    fun newJob(p: PrintJob, input: InputStream, size: Int) {
        val streams = ConcurrentLinkedQueue<PipedOutputStream>()
        log.info("Starting job events")
        for (l in listeners) {
            try {
                val pos = PipedOutputStream()
                val pis = PipedInputStream(pos)
                streams.add(pos)
                executor.execute {
                    pis.use { l(p, it) }
                }
            } catch (e: IOException) {
                log.error("Failed to run job listener", e)
            }
        }
        log.info("Bound job listeners")
        var bytesRead: Int
        var totalBr = 0
        val maxSize = if (size >= 1) size else Int.MAX_VALUE
        val buf = ByteArray(2048)
        try {
            while (totalBr < maxSize) {
                bytesRead = input.read(buf)
                if (bytesRead <= 0) break
                streams.forEach { it.write(buf, 0, bytesRead) }
                totalBr += bytesRead
            }
        } catch (e: IOException) {
            log.info("Error in passing pipe streams", e)
        } finally {
            streams.forEach(PipedOutputStream::close)
        }
    }
}
