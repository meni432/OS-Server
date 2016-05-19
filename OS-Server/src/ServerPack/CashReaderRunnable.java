/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerPack;

import java.util.concurrent.Semaphore;

/**
 *
 * @author ofir Arnon
 */
public class CashReaderRunnable implements Runnable{
    private final SearchRunable searchThread;
    private final Semaphore semDoneReading;
    private final int qurey;
    
   
    public CashReaderRunnable(SearchRunable searchThread, Semaphore semDoneReading,int qurey) {
        this.searchThread = searchThread;
        this.semDoneReading = semDoneReading;
        this.qurey=qurey;
    }
    
     @Override
    public void run() {
        searchThread.setCashAnswer(CashManager.searchCash(qurey));
        semDoneReading.release();       
    }

}
