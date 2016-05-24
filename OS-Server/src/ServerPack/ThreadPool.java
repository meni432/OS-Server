package ServerPack;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {

    private BlockingQueue taskQueue;
    private final List<PoolThread> threads;
    private boolean isStopped = false;

    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * default constructor
     * @param noOfThreads  number of thread to create in this thread pool
     */
    public ThreadPool(int noOfThreads) {
        this.threads = new ArrayList<>();
        taskQueue = new BlockingQueue();

        for (int i = 0; i < noOfThreads; i++) {
            threads.add(new PoolThread(taskQueue));
        }
        for (PoolThread thread : threads) {
            thread.start();
        }
    }
    
    /**
     * constructor with namely threads 
     * @param noOfThreads
     * @param familyName 
     */
    public ThreadPool(int noOfThreads, String familyName) {
        this.threads = new ArrayList<>();
        taskQueue = new BlockingQueue();

        for (int i = 0; i < noOfThreads; i++) {
            threads.add(new PoolThread(taskQueue));
        }
        int familyId = 0;
        for (PoolThread thread : threads) {
            thread.setName(familyName + (familyId++));
            thread.start();
        }
    }

    /**
     * insert new task to thread pool
     * @param task Runnable task
     */
    public void execute(Runnable task) {
        lock.lock();
        try {
            if (this.isStopped) {
                throw new IllegalStateException("ThreadPool is stopped");
            }

            this.taskQueue.enqueue(task);
        } finally {
            lock.unlock();
        }

    }

    /**
     *  stop all thread in this thread pool
     * TODO not test / implement yet
     */
    public void stop() {
        lock.lock();
        try {
           
            this.isStopped = true;
            for (PoolThread thread : threads) {
                thread.doStop();
            }
            
        } finally {
            lock.unlock();
        }
    }

}
