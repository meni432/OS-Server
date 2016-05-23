package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public class RequestMonitor extends Thread {

    private final SyncArrayList<InOutStreams> streamList;
    private final ThreadPool seachersThreadPool;
    private final ThreadPool dBreadersPool;
    private final ThreadPool cashReadersPool;
    private final ReentrantLock lock;
    private boolean executeRead = false;

    public RequestMonitor(SyncArrayList<InOutStreams> streamList, ThreadPool seachersThreadPool, ThreadPool cashReadersPool, ThreadPool dBreadersPool, ReentrantLock lock) {
        this.streamList = streamList;
        this.seachersThreadPool = seachersThreadPool;
        this.cashReadersPool = cashReadersPool;
        this.dBreadersPool = dBreadersPool;
        this.setName("RequestMonitor");
        this.lock = lock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                lock.lock();
                for (int i = 0; i < streamList.size(); i++) {
                    InOutStreams currentStream = streamList.get(i);
                    executeRead = false;
                    Thread readObjectThread = new Thread("Read Socket Thread") {
                        @Override
                        public void run() {
                            try {
                                int query = (int) currentStream.getOis().readObject();
                                executeRead = true;
                                SearchRunable task = new SearchRunable(currentStream, query, cashReadersPool, dBreadersPool);
                                seachersThreadPool.execute(task);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                if (ex instanceof SocketTimeoutException) {
                                    System.out.println("timeout get");
                                } else {

                                    try {
                                        currentStream.getOis().close();
                                        currentStream.getOos().close();
                                        currentStream.getSocket().close();
//                            streamList.remove(currentStream);
//                            System.out.println("is removed:" + streamList.remove(currentStream));
                                    } catch (IOException ex1) {
//                            streamList.remove(currentStream);
                                        System.err.println("catch in request monitor");
                                    }

                                }

                            } catch (ClassNotFoundException ex) {
                                Logger.getLogger(RequestMonitor.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
                    };

                    readObjectThread.start();

                    try {
                        readObjectThread.join(Server.TIME_TO_WAIT);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    if (executeRead == false) {
                        readObjectThread.interrupt();
                        System.err.println("interrupt operation on");
                    }

                }
            } finally {

                lock.unlock();

            }
        }
    }
}
