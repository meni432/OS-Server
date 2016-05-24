package ServerPack;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * implement synchronized blocking queue
 */
public class BlockingQueue {

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private List queue = new LinkedList();

    /**
     * insert element to the queue
     * @param item 
     */
    public void enqueue(Object item) {
        lock.lock();
        try {
            this.queue.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * get element from the queue,
     * if queue is empty, wait for a element.
     * @return
     * @throws InterruptedException 
     */
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

    /**
     * @return true if has more element in the queue, else false
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

}
