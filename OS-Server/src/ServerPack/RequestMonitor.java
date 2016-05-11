package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public class RequestMonitor extends Thread {

    private final List<InOutStreams> streamList;
    private final ThreadPool seachersThreadPool;
    private final ThreadPool dBreadersPool;
    private final ThreadPool cashReadersPool;
    private final ReentrantLock lock;

    public RequestMonitor(List<InOutStreams> streamList, ThreadPool seachersThreadPool, ReentrantLock lock,ThreadPool cashReadersPool,ThreadPool dBreadersPool) {
        this.streamList = streamList;
        this.seachersThreadPool = seachersThreadPool;
        this.cashReadersPool=cashReadersPool;
        this.dBreadersPool=dBreadersPool;
        this.lock = lock;
    }

    @Override
    public void run()  {
        while (true) {
            lock.lock();
            try {
                
                for (InOutStreams currentStream : streamList){
                    try {
                        // TODO blocking read
                        int query = (int)currentStream.getOis().readObject();
                        SearchThread task = new SearchThread(currentStream, query,cashReadersPool,dBreadersPool);
                        seachersThreadPool.execute(task);
                        
                    } catch (IOException ex) {
                        try {
                            currentStream.getOis().close();
                            currentStream.getOos().close();
                            currentStream.getSocket().close();
//                            System.out.println("is removed:"+streamList.remove(currentStream));
                        } catch (IOException ex1) {
                           // streamList.remove(currentStream);
                            System.err.println("catch in request monitor");
                        }
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(RequestMonitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
