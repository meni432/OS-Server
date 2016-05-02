/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.locks.ReentrantLock;


/**
 *
 * @author ofir Arnon
 */
public class Server {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        final int PORT = 5000;
        final ReentrantLock lock=new ReentrantLock(true);
        ServerSocket serverSocket;
        
        new Thread(new WriterThread()).start();
        
        try {
             serverSocket = new ServerSocket(PORT);
             ConnectionManager cm=new ConnectionManager(serverSocket,lock);
             new Thread(cm).start();
             ThreadPool sThreads=new ThreadPool(5);
             RequestMonitor requestMonitor = new RequestMonitor(cm.getStreamList(), sThreads, lock);
             requestMonitor.start();
        } catch (IOException ex) {
            System.out.println("cannot creats server socket");
            System.exit(1);
        }
       
    }
    
}
