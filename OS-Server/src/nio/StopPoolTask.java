package nio;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public class StopPoolTask implements Runnable{
private ThreadPool tp;

    public StopPoolTask(ThreadPool tp) {
        this.tp=tp;
    }


    
    @Override
    public void run() {
//    try {
//        Thread.sleep(3000);
//    } catch (InterruptedException ex) {
//        Logger.getLogger(StopPoolTask.class.getName()).log(Level.SEVERE, null, ex);
//    }
        tp.stop();
    }

    
    
}
