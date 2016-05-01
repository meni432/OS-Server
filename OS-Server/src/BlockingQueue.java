

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockingQueue {

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();

    private List queue = new LinkedList();

    public void enqueue(Object item) {
        lock.lock();
        try {
            this.queue.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Object dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (this.queue.isEmpty()) {               
                notEmpty.await();
            }
            return this.queue.remove(0);
        } finally {
            lock.unlock();
        }
        
    }

}
