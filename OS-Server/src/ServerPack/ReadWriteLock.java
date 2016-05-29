package ServerPack;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author ofir Arnon
 */
public class ReadWriteLock {

    private int readers = 0, wrieters = 0;
    private Semaphore Rmutex = new Semaphore(1);
    private Semaphore Wmutex = new Semaphore(1);
    private Semaphore Mutex2 = new Semaphore(1);
    private Semaphore Rdb = new Semaphore(1);
    private Semaphore Wdb = new Semaphore(1);

    public void lockRead() throws InterruptedException {
        Mutex2.acquire();
        Rdb.acquire();
        Rmutex.acquire();
        readers++;
        if (readers == 1) {
            Wdb.acquire();
        }
        Rmutex.release();
        Rdb.release();
        Mutex2.release();
        // Enter TO <C.S>

    }

    public void unlockRead() {
        try {
            Rmutex.acquire();
            readers--;
            if (readers == 0) {
                Wdb.release();
            }
            Rmutex.release();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void lockWrite() throws InterruptedException {
        Wmutex.acquire();
        wrieters++;
        if (wrieters == 1) {
            Rdb.acquire();
        }
        Wmutex.release();
        Wdb.acquire();
        // Enter To <C.S>
    }

    public void unlockWrite() {
        try {
            Wdb.release();
            Wmutex.acquire();
            wrieters--;
            if (wrieters == 0) {
                Rdb.release();
            }
            Wmutex.release();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    
//    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
//
//    public void lockRead() throws InterruptedException{
//        readWriteLock.readLock().lock();
//    }
//    public void unlockRead() {
//        readWriteLock.readLock().unlock();
//    }
//
//    public void lockWrite() throws InterruptedException {
//        readWriteLock.writeLock().lock();
//    }
//
//    public void unlockWrite() {
//        readWriteLock.writeLock().unlock();
//    }

}
