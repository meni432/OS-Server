package ServerPack;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


/**
 *
 * @author Meni Samet
 * @param <T>
 */
public class SyncArrayList<T> {

    private final ArrayList<T> array = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * add element to array
     *
     * @param data
     */
    public void add(T data) {
        lock.lock();
        try {
            array.add(data);
        } finally {
            lock.unlock();
        }
    }

    /**
     * get element from array
     *
     * @param index index of element in array
     * @return T object at index
     */
    public T get(int index) {
        lock.lock();
        try {
            return array.get(index);
        } finally {
            lock.unlock();
        }
    }

    /**
     * remove element in given index
     *
     * @param index index of element to remove
     * @return the object T that remove
     */
    public T remove(int index) {
        lock.lock();
        try {
            return array.remove(index);
        } finally {
            lock.unlock();
        }
    }

    /**
     * remove element that send
     * @param element element T
     * @return true if remove, else false
     */
    public boolean remove(T element) {
        lock.lock();
        try {
            return array.remove(element);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return size of array (number of elements)
     */
    public int size() {
        lock.lock();
        try {
            return array.size();
        } finally {
            lock.unlock();
        }
    }

}
