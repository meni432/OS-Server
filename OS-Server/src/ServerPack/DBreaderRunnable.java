package ServerPack;

import java.util.concurrent.Semaphore;

/**
 * DataBase Reader Runnable class
 * this class is a Task to search in DataBase
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
        ans = DatabaseManager.getInstance().readY(qurey);
        semDoneReading.release(); //release get Ans method      
    }
    
    public int getAns() throws InterruptedException{
        semDoneReading.acquire();// wait for answer from the DataBase
        return ans;
    }
    

    
}
