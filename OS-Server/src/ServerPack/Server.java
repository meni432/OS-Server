package ServerPack;

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

    private static ThreadPool seachersPool;
    private static ThreadPool cashReadersPool;
    private static ThreadPool dBreadersPool;
    private static final int PORT = 5000;
    private static final ReentrantLock lock = new ReentrantLock(true);
    private static ServerSocket serverSocket;
    public static final int S_THREADS_NUM = 5; //1. S - number of allowed S-threads.
    public static final int CASH_SIZE = 100; //C - size of the cache.
    public static final int LEAST_TO_CACHE = 10;//3. M - the least number of times a query has to be requested in order to be allowed to enter the cache.
    public static final int RANDOM_RANGE = 100; //4. L - to specify the range [1, L] from which missing replies will be drawn uniformly at random.
    public static final int NUMBER_OF_READER_THREADS = 7; //5. Y - number of reader threads.

    /**
     * @param args the command line arguments
     */
    public static void creatsThreadPools() {
        seachersPool = new ThreadPool(S_THREADS_NUM);
        cashReadersPool = new ThreadPool(NUMBER_OF_READER_THREADS);
        dBreadersPool = new ThreadPool(NUMBER_OF_READER_THREADS);
    }

    public static void startWriteThreads() {
        new Thread(new DbWriterThread()).start();
        new Thread(new CashWriteThread()).start();
    }

    public static void startConnection() {
        try {
            serverSocket = new ServerSocket(PORT);
            ConnectionManager connectionM = new ConnectionManager(serverSocket, lock);
            new Thread(connectionM).start();
            new RequestMonitor(connectionM.getStreamList(), seachersPool, lock, cashReadersPool, dBreadersPool).start();
        } catch (IOException ex) {
            System.out.println("cannot creats server socket");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        creatsThreadPools();
        startWriteThreads();
        startConnection();
    }

}
