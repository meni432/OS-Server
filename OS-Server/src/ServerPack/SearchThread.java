package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public class SearchThread implements Runnable {

    private final InOutStreams ois;
    private final int query;
    private final ThreadPool dBreadersPool;
    private final ThreadPool cashReadersPool;
    private int cashAns=DatabaseManager.NOT_FOUND;
    private int databaseAnswer=DatabaseManager.NOT_FOUND;
    private final Semaphore semDoneReading; 
    
    
    
    public SearchThread(InOutStreams ois, int query,ThreadPool cashReaderPool,ThreadPool dBreadersPool){
        this.ois = ois;
        this.query = query;  
        this.dBreadersPool=dBreadersPool;
        this.cashReadersPool=cashReaderPool;
        this.semDoneReading=new Semaphore(0);
    }
    
    @Override
    public void run() {
        
        try {
 
            cashReadersPool.execute(new CashReaderRunnable(this, semDoneReading, query));
            semDoneReading.acquire();// wating to cash reader to done reading.
            if(this.cashAns!=DatabaseManager.NOT_FOUND){
//                Thread.sleep(20);
                ois.getOos().writeObject(this.cashAns);
                System.err.println("cash answer x="+query+" y="+this.cashAns);
            }else{
            
                dBreadersPool.execute(new DBreaderRunnable(this, semDoneReading, query));
                semDoneReading.acquire();// wating for DBreaderThread to done reading
//                Thread.sleep(20);
                ois.getOos().writeObject(this.databaseAnswer); 
                System.out.println("Db answer x="+query+" y="+this.databaseAnswer);
            }

  
        } catch (IOException ex) {
            try {
                ois.getOis().close();
                ois.getOos().close();
                ois.getSocket().close();
                System.err.println("STask colse connection(on write)");
            } catch (IOException ex1) {
                System.err.println("catch in STask");
            }
        
        } catch (InterruptedException ex) {
            Logger.getLogger(SearchThread.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
    
    public void setCashAnswer(int y)
    { 
        this.cashAns=y;
    }
    public void setDatabaseAnswer(int y)
    {
        this.databaseAnswer=y;
        DatabaseManager.updateFromCash(query, y, 1);/// ----Todo update when removing the number from the cash. 
    }

}


