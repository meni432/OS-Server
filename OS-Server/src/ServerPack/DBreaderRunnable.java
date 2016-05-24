package ServerPack;

import java.util.concurrent.Semaphore;

/**
<<<<<<< HEAD
 * DataBase Reader Runnable class
 * this class is a Task to search in DataBase
=======
 * DB reader Runnable class
 *  this class is a Task to search in DB
>>>>>>> refs/remotes/origin/up-change
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
<<<<<<< HEAD
        semDoneReading.release(); //release get Ans method      
    }
    
    public int getAns() throws InterruptedException{
        semDoneReading.acquire();// wait for answer from the DataBase
=======
        semDoneReading.release(); // release getAns function
    }
    
    public int getAns() throws InterruptedException{
        semDoneReading.acquire(); // wait until finish reading
>>>>>>> refs/remotes/origin/up-change
        return ans;
    }

    
}
