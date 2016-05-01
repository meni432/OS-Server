/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager implements Runnable {
// TODO change in the client to ObjectInputStream
    
    private final ReentrantLock lock;
    ServerSocket socketS;
    List<InOutStreams> streamList;
    int currentStream = 0;

    public ConnectionManager(ServerSocket socketS,ReentrantLock lock) {
        this.socketS = socketS;
        this.streamList=Collections.synchronizedList(new ArrayList<InOutStreams>());
        this.lock=lock;
    }

    public void run() {
        /// why do we need lock????
        while (true) {
            try {
                Socket s = socketS.accept();
                System.out.println("connection accept");
              lock.lock();
                try {
                    streamList.add(new InOutStreams(s));
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                System.out.println("problem in the seerver connection");
            }
        }
    }

    public List<InOutStreams> getStreamList() {
        return streamList;
    }

 

}
