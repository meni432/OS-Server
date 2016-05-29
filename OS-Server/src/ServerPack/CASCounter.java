package ServerPack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Compare And Swap Number
 */
class CASNumber {

    private int value;
    private ReentrantLock lock = new ReentrantLock(true);

    public int compareAndSwap(int expectedValue, int newValue) {
        lock.lock();
        try {
            int oldValue = value;
            if (value == expectedValue) {
                value = newValue;
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    public int getValue() {
        lock.lock();
        try {
            return value;
        } finally {
            lock.unlock();
        }
    }

}

public class CASCounter {

    private CASNumber value = new CASNumber();

    public int getValue() {
        return value.getValue();
    }

    public int increment() {
        int oldValue = value.getValue();
        while (value.compareAndSwap(oldValue, oldValue + 1) != oldValue) {
            oldValue = value.getValue();
        }
        return oldValue + 1;
    }

    public int decrement() {
        int oldValue = value.getValue();
        while (value.compareAndSwap(oldValue, oldValue - 1) != oldValue) {
            oldValue = value.getValue();
        }
        return oldValue - 1;
    }

}
