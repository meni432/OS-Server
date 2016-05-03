
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Meni Samet
 */
public class WriterThread implements Runnable {

    final static int TTS = 3000;

    @Override
    public void run() {
        
            while (true) {
                try {
                    Thread.sleep(TTS);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WriterThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                Database.writeAll();
                System.err.println("-----------------------Write all compleated!------------------------");
            }
        
    }

}
