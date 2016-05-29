package ServerPack;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * this class monitor the request that sending to the server (query request) and
 * add the query to the SearchThreadPool
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
                for (int i= 0; i < streamList.size(); i++) {
                    InOutStreams currentStream = streamList.get(i);
                    executeRead = false;
//                    
                    try {

                        int query = currentStream.getInteger();
                        SearchRunable task = new SearchRunable(currentStream, query, cashReadersPool, dBreadersPool);
                        seachersThreadPool.execute(task);
                    } catch (IOException ex) {
                        try {
                            ex.printStackTrace();
                            currentStream.getOis().close();
                            currentStream.getOos().close();
                            currentStream.getSocket().close();
                        }
                        catch (IOException ex1) {
                            ex1.printStackTrace();
                        }
                        finally{
                            streamList.remove(i);
                            i--;
                        }

                    }

                }
            } finally {

                lock.unlock();

            }
        }
    }
}