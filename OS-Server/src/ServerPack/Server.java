package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ofir Arnon
 */
public final class Server {

    public static volatile Server _instance;
    private static final ReentrantLock instanceLock = new ReentrantLock(true);
    private static final int PORT = 4500; // defult port

    /* lock that synchronized between connectionManager and requestMonitor */
    private final ReentrantLock lock = new ReentrantLock(true);
    private ServerSocket serverSocket;
    public static final int TIME_TO_WAIT = 1000; // time to wait for read object until interrupt him

    /*-------- defult parameters to start without command line ------------------------ */
    public static int S_THREADS_NUM; //1. S - number of allowed S-threads.
    public static int CACHE_SIZE; //C - size of the cache.
    public static int LEAST_TO_CACHE;//3. M - the least number of times a query has to be requested in order to be allowed to enter the cache.
    public static int RANDOM_RANGE; //4. L - to specify the range [1, L] from which missing replies will be drawn uniformly at random.
    public static int NUMBER_OF_READER_THREADS; //5. Y - number of reader threads.

    /* Thread pools */
    private ThreadPool seachersPool; // s-Thread pool
    private ThreadPool cashReadersPool; // c-Thread pool
    private ThreadPool dBreadersPool; // r-Thread   pool

    /**
     * @param S S_THREADS_NUM
     * @param C CACHE_SIZE
     * @param M LEAST_TO_CACHE
     * @param L RANDOM_RANGE
     * @param Y NUMBER_OF_READER_THREADS
     */
    private Server(int S, int C, int M, int L, int Y) {
        S_THREADS_NUM = S;
        CACHE_SIZE = C;
        LEAST_TO_CACHE = M;
        RANDOM_RANGE = L;
        NUMBER_OF_READER_THREADS = Y;

        creatsThreadPools();
        startWriteThreads();
        startConnection();
        System.out.println("start server:\n"
                + "S_THREADS_NUM: " + S_THREADS_NUM + "\n"
                + "CACHE_SIZE: " + CACHE_SIZE + "\n"
                + "LEAST_TO_CACHE: " + LEAST_TO_CACHE + "\n"
                + "RANDOM_RANGE: " + RANDOM_RANGE + "\n"
                + "NUMBER_OF_READER_THREADS: " + NUMBER_OF_READER_THREADS + "\n");
    }



    public static Server getInstance(int S, int C, int M, int L, int Y) {
        instanceLock.lock();
        try {
            if (_instance == null) {
                _instance = new Server(S, C, M, L, Y);
            }
            return _instance;
        } finally {
            instanceLock.unlock();
        }
    }
    public void creatsThreadPools() {
        seachersPool = new ThreadPool(S_THREADS_NUM, "seachersPool");
        cashReadersPool = new ThreadPool(NUMBER_OF_READER_THREADS, "cashReadersPool");
        dBreadersPool = new ThreadPool(NUMBER_OF_READER_THREADS, "dBreadersPool");
    }

    public void startWriteThreads() {
        new Thread("DbWriterThread") {
            @Override
            public void run() {
                DatabaseManager databaseManager = DatabaseManager.getInstance();
                while (true) {
                    databaseManager.chackForUpdate();
//                    System.out.println("------------update DB completed---------------");
                    
                }
            }
        }.start();
        new Thread("CashWriteThread") {
            @Override
            public void run() {
                while (true) {
                    CacheManager.updateCache();
                    System.out.println("------------update cash completed---------------");
                }
            }
        }.start();
    }

    public void startConnection() {

        try {
            serverSocket = new ServerSocket(PORT,1000);
            SyncArrayList<InOutStreams> streamList = new SyncArrayList<>();
            ConnectionManager connectionManager = new ConnectionManager(serverSocket, streamList, lock);
            new Thread(connectionManager).start();
            new RequestMonitor(streamList, seachersPool, cashReadersPool, dBreadersPool, lock).start();
        } catch (IOException ex) {
            System.out.println("cannot creats server socket");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // custing parameter from command line to Integer;
        int S, C, M, L, Y;
        if (args.length == 5) {
            S = Integer.parseInt(args[0]);
            C = Integer.parseInt(args[1]);
            M = Integer.parseInt(args[2]);
            L = Integer.parseInt(args[3]);
            Y = Integer.parseInt(args[4]);
        } else {
            S = 5;
            C = 100;
            M = 25;
            L = 1000;
            Y = 5;
        }
        
        // call to Server Instance
        Server server = Server.getInstance(S, C, M, L, Y);
    }

}
