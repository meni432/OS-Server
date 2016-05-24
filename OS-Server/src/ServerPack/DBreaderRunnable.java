package ServerPack;

import java.util.concurrent.Semaphore;

/**
 * DB reader Runnable class
 *  this class is a Task to search in DB
 */
public class DBreaderRunnable implements Runnable{

    private final Semaphore semDoneReading;
    private final int qurey;
    private int ans = DatabaseManager.NOT_FOUND;
    
   
    public DBreaderRunnable(int qurey) {
        this.semDoneReading = new Semaphore(0);
        this.qurey=qurey;
    }
    
     @Override
    public void run() {
        ans = DatabaseManager.readY(qurey);
        semDoneReading.release(); // release getAns function
    }
    
    public int getAns() throws InterruptedException{
        semDoneReading.acquire(); // wait until finish reading
        return ans;
    }

    
}
