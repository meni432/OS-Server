package nio;



import java.util.concurrent.locks.ReentrantLock;

public class PoolThread extends Thread {

    private BlockingQueue taskQueue = null;
    private boolean isStopped = false;

    private final ReentrantLock lock = new ReentrantLock(true);

    public PoolThread(BlockingQueue queue) {
        taskQueue = queue;
    }

    public void run() {
        while (!isStopped()) {
            try {
                Runnable runnable = (Runnable) taskQueue.dequeue();
                runnable.run();
            } catch (InterruptedException e) {
                //log or otherwise report exception,
                //but keep pool thread alive.
              //  e.printStackTrace();
            }
            
        }
    }

    public void doStop() {
        
        lock.lock();
        try {
           
            isStopped = true;
            this.interrupt(); //break pool thread out of dequeue() call!! (awaits in dequeue)
        } finally {
            lock.unlock();
        }
    }

    public boolean isStopped() {
        lock.lock();
        try {
            return isStopped;
        } finally {
            lock.unlock();
        }
    }
}
