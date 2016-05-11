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

    final static int S = 5; // number of allowed S-threads
    final static int C = 10; //  size of the cache
    final static int M = 1000; // the least number of times a query has to be requested in order to be allowed to enter the cache
    final static int L = 100; // to specify the range [1, L] from which missing replies will be drawn uniformly at random
    final static int Y = 10; // number of reader threads

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        final int PORT = 5000;
        final ReentrantLock lock = new ReentrantLock(true);
        ServerSocket serverSocket;
        CashManager cashM = new CashManager();
        new Thread(cashM).start();
        Database dataB = new Database(cashM);
        new Thread(new WriterThread()).start();

        try {
            serverSocket = new ServerSocket(PORT);
            ConnectionManager connectionM = new ConnectionManager(serverSocket, lock);
            new Thread(connectionM).start();
            ThreadPool sThreads = new ThreadPool(5);
            RequestMonitor requestMonitor = new RequestMonitor(connectionM.getStreamList(), sThreads, lock, cashM);
            requestMonitor.start();
        } catch (IOException ex) {
            System.out.println("cannot creats server socket");
            System.exit(1);
        }

    }

}
