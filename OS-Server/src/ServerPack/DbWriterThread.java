package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Meni Samet
 */
public class DbWriterThread implements Runnable {
    
    @Override
    public void run() {
        Thread.currentThread().setName("DbWriterThread");
        while (true) {
            DatabaseManager.writeAll();
            System.err.println("------------update DB completed---------------");
        }
        
    }
    
}
