/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public class STask implements Runnable {

    InOutStreams ois;
    int query;
    
    public STask(InOutStreams ois, int query){
        this.ois = ois;
        this.query = query;
    }
    
    @Override
    public void run() {
        
        try {
            Thread.sleep(20);
            int ans = Database.readY(query);
            System.out.println(Thread.currentThread().getName()+" "+ans);
            ois.getOos().writeObject(ans);
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
            Logger.getLogger(STask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}


//    ConnectionManager cm;
//
//    public STask(ConnectionManager cm) {
//        this.cm = cm;
//    }
//    
//    
//
//    @Override
//    public void run() {
//        int query;
//        InOutStreams streams = cm.getStreams();
//        if (streams == null) {
//            // System.out.println("streams null");
//            return;
//        }
//        try {
//            //System.out.println("streams NOT null");
//            if (streams.getOis().available() == 0) {
//                System.out.println("available()==0");
//                return;
//            }
//            System.out.println("available != 0");
//            query = streams.getOis().readInt();
//            System.out.println("query:" + query);
//        } catch (IOException ex) {
//            System.out.println("end of stream");
//            return;
//        }
//        try {
//            //-------------------------------------//
//            // search in the cash
//            //-------------------------------------//
//            int ans = Database.readY(query);
//            streams.getOos().writeInt(ans);
//        } catch (IOException ex) {
//            System.out.println("problem in reading");
//            return;
//        }
//
//    }
    
