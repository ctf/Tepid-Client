package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.models.data.PrintJob;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LPDServer implements Closeable {
    private boolean closed;
    private ServerSocket serverSock = null;
    private Queue<LPDJobListener> listeners = new ConcurrentLinkedQueue<>();

    public LPDServer(int port) throws IOException {
        serverSock = new ServerSocket(port, 0, InetAddress.getByName(null));
    }

    public void start() {
        while (!closed) {
            try {
                new LPDClientHandler(serverSock.accept(), LPDServer.this).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        serverSock.close();
    }

    public void addJobListener(LPDJobListener l) {
        listeners.add(l);
    }

    protected void newJob(final PrintJob p, InputStream is, long size) {
        if (size < 1) size = Long.MAX_VALUE;
        Queue<PipedOutputStream> streams = new ConcurrentLinkedQueue<>();
        for (final LPDJobListener l : listeners) {
            try {
                PipedOutputStream pos = new PipedOutputStream();
                final PipedInputStream pis = new PipedInputStream(pos);
                streams.add(pos);
                new Thread("Print Job Event") {
                    @Override
                    public void run() {
                        l.printJob(p, pis);
                    }
                }.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            int bytesRead, totalBr = 0;
            byte[] buf = new byte[2048];
            while (totalBr < size && (bytesRead = is.read(buf)) > 0) {
                for (OutputStream os : streams) {
                    os.write(buf, 0, bytesRead);
                }
                totalBr += bytesRead;
            }
            for (OutputStream os : streams) os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
