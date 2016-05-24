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
                for (int i= 0; i < streamList.size(); i++) {
                    InOutStreams currentStream = streamList.get(i);
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