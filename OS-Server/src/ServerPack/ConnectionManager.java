package ServerPack;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Connection Manager Class.
 * This is a Runnable class that alway listen for new client connection,
 * and after connection add him to ArryaList
 */
public class ConnectionManager implements Runnable {
// TODO change in the client to ObjectInputStream

    ServerSocket socketS;
    SyncArrayList<InOutStreams> streamList;
    int currentStream = 0;
    ReentrantLock lock;

    public ConnectionManager(ServerSocket socketS, SyncArrayList<InOutStreams> streamList, ReentrantLock lock) {
        this.socketS = socketS;
        this.streamList = streamList;
        this.lock = lock;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ConnectionManager");
        /// why do we need lock????
        while (true) {
            try {
                Socket s = socketS.accept();
                lock.lock();
                System.out.println("connection accept");
                try {
                    streamList.add(new InOutStreams(s));
                    System.out.println("connection add");
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                System.out.println("problem in the seerver connection");
                e.printStackTrace();
            }
        }
    }

    public SyncArrayList<InOutStreams> getStreamList() {
        return streamList;
    }

}
