package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
/**
 * manage the client-server connection, 
 * @author ofir Arnon
 */
public class ConnectionManager implements Runnable {

    ServerSocket serverSocket;
    SyncArrayList<InOutStreams> streamList; //structure that holds inOutStream for each client connection
    int currentStream = 0;
    ReentrantLock lock; // synchronizing between iterate the streamList(ReaquestMonitor class), and adding inOutStream to the streamList
    /**
     * 
     * @param serverSocket socket server
     * @param streamList structure that holds inOutStream for each client connection
     * @param lock  synchronizing between iterate the streamList(ReaquestMonitor class), and adding inOutStream to the streamList
     */
    public ConnectionManager(ServerSocket serverSocket, SyncArrayList<InOutStreams> streamList, ReentrantLock lock) {
        this.serverSocket = serverSocket;
        this.streamList = streamList;
        this.lock = lock;
    }

   /**
    * creates socket for each client connection, and add it to the streamList
    */
    @Override
    public void run() {
        Thread.currentThread().setName("ConnectionManager");
        while (true) {
            try {
                Socket s = serverSocket.accept(); 
                lock.lock();
                try {
                    streamList.add(new InOutStreams(s));
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                System.out.println("problem in the seerver connection");
                e.printStackTrace();
            }
        }
    }
}
