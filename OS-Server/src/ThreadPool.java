

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {

    private BlockingQueue taskQueue = null;
    private List<PoolThread> threads = new ArrayList<PoolThread>();
    private boolean isStopped = false;

    private final ReentrantLock lock = new ReentrantLock(true);

    public ThreadPool(int noOfThreads) {
        taskQueue = new BlockingQueue();

        for (int i = 0; i < noOfThreads; i++) {
            threads.add(new PoolThread(taskQueue));
        }
        for (PoolThread thread : threads) {
            thread.start();
        }
    }

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
