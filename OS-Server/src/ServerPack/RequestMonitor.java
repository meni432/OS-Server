package ServerPack;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private ScheduleThread scheduleThread = new ScheduleThread();
    private ThreadPool readerThreadPool = new ThreadPool(1);

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
                int[] i = {0};
                for (i[0] = 0; i[0] < streamList.size(); i[0]++) {
                    InOutStreams currentStream = streamList.get(i[0]);
                    executeRead = false;
//                    ScheduleableRunnable readObjectRunnable = new ScheduleableRunnable() {
//
//                        Semaphore isFinishRead = new Semaphore(0);
//                        Semaphore mutex = new Semaphore(1);
//
//                        @Override
//                        public void run() {
//                            try {
//                                int query = (int) currentStream.getOis().readObject();
//                                executeRead = true;
//                                isFinishRead.release();
//                                SearchRunable task = new SearchRunable(currentStream, query, cashReadersPool, dBreadersPool);
//                                seachersThreadPool.execute(task);
//                            } catch (IOException ex) {
//                                ex.printStackTrace();
//                                if (ex instanceof SocketTimeoutException) {
//                                    System.out.println("timeout get");
//                                } else {
//
//                                    try {
//                                        currentStream.getOis().close();
//                                        currentStream.getOos().close();
//                                        currentStream.getSocket().close();
//
//                                    } catch (IOException ex1) {
////                            streamList.remove(currentStream);
//                                        System.err.println("catch in request monitor");
//                                    } finally {
//                                        System.out.println("is removed:" + streamList.remove(i[0]));
//                                        i[0]--;
//                                    }
//
//                                }
//
//                            } catch (ClassNotFoundException ex) {
//                                ex.printStackTrace();
//                            }
////                            catch (InterruptedException ex) {
////                                Logger.getLogger(RequestMonitor.class.getName()).log(Level.SEVERE, null, ex);
////                            }
//
//                        }
//
//                        @Override
//                        public void timeOver() {
//                            System.out.println("sc time out");
//                            isFinishRead.release();
//                        }
//
//                        @Override
//                        public void isFinish() {
//                            try {
//                                while (executeRead == false) {
//                                    isFinishRead.acquire();
//                                }
//                            } catch (InterruptedException ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//                    };
//
//                    Thread readThread = new Thread(readObjectRunnable);
//                    readThread.start();
//                    scheduleThread.addTask(Server.TIME_TO_WAIT*1000, readObjectRunnable);
//                    readObjectRunnable.isFinish();
//                    
                    try {

                        int query = currentStream.getInteger();
                        SearchRunable task = new SearchRunable(currentStream, query, cashReadersPool, dBreadersPool);
                        seachersThreadPool.execute(task);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

//                    if (executeRead == false) {
//                        readThread.interrupt();
//                    }
                }
            } finally {

                lock.unlock();

            }
        }
    }
}

class ScheduleThread extends Thread {

    class ScheduleTask {

        private long time;
        private Scheduleable scheduleable;

        public ScheduleTask(long time, Scheduleable scheduleable) {
            this.time = time;
            this.scheduleable = scheduleable;
        }
    }

    BlockingQueue tasks = new BlockingQueue();
    ReadWriteLock lock = new ReadWriteLock();

    public void addTask(long time, Scheduleable scheduleable) throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        tasks.enqueue(new ScheduleTask(currentTime + time, scheduleable));
    }

    public ScheduleThread() {
        super("ScheduleThread");
    }

    @Override
    public void run() {
        while (true) {
            try {
                ScheduleTask task = (ScheduleTask) tasks.dequeue();
                long currentTime = System.currentTimeMillis();
                if (task.time - currentTime < 0) {
                    task.scheduleable.timeOver();
                } else {
                    tasks.enqueue(task);
                }
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}

interface Scheduleable {

    public void timeOver();

    public void isFinish();
}

abstract class ScheduleableRunnable implements Scheduleable, Runnable {

}
