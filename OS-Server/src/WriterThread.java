
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

    final static int TTS = 1000;

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(TTS);
                Database.writeAll();
                System.out.println("Write all compleated!");
            }
        } catch (InterruptedException ex) {
        } catch (IOException ex) {
            Logger.getLogger(WriterThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
