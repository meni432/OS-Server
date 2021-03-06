package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * s-Runnable-Task to search in the cache/DB  
 * @author ofir Arnon
 */
public class SearchRunable implements Runnable {

    private final InOutStreams ois;
    private final int query;
    private final ThreadPool dBreadersPool;
    private final ThreadPool cashReadersPool;
    private int cashAns = DatabaseManager.NOT_FOUND;
    private int databaseAnswer = DatabaseManager.NOT_FOUND;

    public SearchRunable(InOutStreams ois, int query, ThreadPool cashReaderPool, ThreadPool dBreadersPool) {
        this.ois = ois;
        this.query = query;
        this.dBreadersPool = dBreadersPool;
        this.cashReadersPool = cashReaderPool;
    }

    @Override
    public void run() {

        try {
            CacheReaderRunnable cacheReaderRunnable = new CacheReaderRunnable(query);
            cashReadersPool.execute(cacheReaderRunnable);
            this.cashAns = cacheReaderRunnable.getAns(); // wait until get ans
            if (this.cashAns != DatabaseManager.NOT_FOUND) { // check if found on cash

                   ois.writeInteger(this.cashAns); // send the answare to the client
                System.out.println("cash answer x=" + query + " y=" + this.cashAns);
            } else { // if not found in cash, search in database
                // create new database search reader and execute him to thread pool
                DBreaderRunnable dBreaderRunnable = new DBreaderRunnable(query);
                dBreadersPool.execute(dBreaderRunnable);
                this.databaseAnswer = dBreaderRunnable.getAns(); // wait until get ans
                ois.writeInteger(this.databaseAnswer);
                System.out.println("Db answer x=" + query + " y=" + this.databaseAnswer);

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
            Logger.getLogger(SearchRunable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



}
