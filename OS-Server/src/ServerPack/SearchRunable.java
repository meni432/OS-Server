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
public class SearchRunable implements Runnable {

    private final InOutStreams ois;
    private final int query;
    private final ThreadPool dBreadersPool;
    private final ThreadPool cashReadersPool;
    private int cashAns = DatabaseManager.NOT_FOUND;
    private int databaseAnswer = DatabaseManager.NOT_FOUND;
    private final Semaphore semDoneReading;

    public SearchRunable(InOutStreams ois, int query, ThreadPool cashReaderPool, ThreadPool dBreadersPool) {
        this.ois = ois;
        this.query = query;
        this.dBreadersPool = dBreadersPool;
        this.cashReadersPool = cashReaderPool;
        this.semDoneReading = new Semaphore(0);
    }

    @Override
    public void run() {

        try {

            // create new cash serach reader and execute him to thread pool
            cashReadersPool.execute(new CashReaderRunnable(this, semDoneReading, query));
            // wating to cash reader to done reading.
            semDoneReading.acquire();
            // check if found on cash
            if (this.cashAns != DatabaseManager.NOT_FOUND) {
//                Thread.sleep(20);
                // send the answare to the client
                ois.getOos().writeObject(this.cashAns);
                System.err.println("cash answer x=" + query + " y=" + this.cashAns);
            } else { // if not found in cash, search in database
                // create new database search reader and execute him to thread pool
                dBreadersPool.execute(new DBreaderRunnable(this, semDoneReading, query));
                // wating for DBreaderThread to done reading
                semDoneReading.acquire();
//                Thread.sleep(20);

                // send the answare to the client
                ois.getOos().writeObject(this.databaseAnswer);
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

    /**
     * get answer from cash (insert by the cash)
     *
     * @param y the answer
     */
    public void setCashAnswer(int y) {
        this.cashAns = y;
    }

    /**
     * get answer from database (insert by the database)
     *
     * @param y
     */
    public void setDatabaseAnswer(int y) {
        this.databaseAnswer = y;
    }

}
