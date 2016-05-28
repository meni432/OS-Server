package ServerPack;



import java.util.concurrent.locks.ReentrantLock;
/**
 * represent a thread in a threadPool 
 * @author ofir Arnon
 */
public class PoolThread extends Thread {

    private final BlockingQueue taskQueue;
    private boolean isStopped = false;

    private final ReentrantLock lock = new ReentrantLock(true);

    public PoolThread(BlockingQueue queue) {
        taskQueue = queue;
    }

    @Override
    public void run() {
        while (!isStopped()) {
            try {
                Runnable runnable = (Runnable) taskQueue.dequeue();// dequeue runnble task
                runnable.run();
            } catch (InterruptedException e) {}
            
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
