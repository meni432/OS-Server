package ServerPack;

import java.util.concurrent.Semaphore;

/**
 * Cache Reader Runnable class
 * this class is a Task to search in cache
 */
public class CacheReaderRunnable implements Runnable {

    private final Semaphore semDoneReading;
    private final int qurey;
    private int ans = DatabaseManager.NOT_FOUND;

    public CacheReaderRunnable(int qurey) {
        this.semDoneReading = new Semaphore(0);
        this.qurey = qurey;
    }

    @Override
    public void run() {
        ans = CacheManager.searchCash(qurey);
        semDoneReading.release(); //release get Ans method
    }
    
    public int getAns() throws InterruptedException{
        semDoneReading.acquire(); // wait for answer from the cash
        return ans;
    }

}
