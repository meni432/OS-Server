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
    public static final int TIME_TO_WAIT = 1000; // time to wait for read object until interrupt him

    public static int S_THREADS_NUM = 5; //1. S - number of allowed S-threads.
    public static int CACHE_SIZE = 100; //C - size of the cache.
    public static int LEAST_TO_CACHE = 10;//3. M - the least number of times a query has to be requested in order to be allowed to enter the cache.
    public static int RANDOM_RANGE = 2; //4. L - to specify the range [1, L] from which missing replies will be drawn uniformly at random.
    public static int NUMBER_OF_READER_THREADS = 7; //5. Y - number of reader threads.

//    public final static int S_THREADS_NUM ; //1. S - number of allowed S-threads.
//    public final static int CACHE_SIZE; //C - size of the cache.
//    public final static int LEAST_TO_CACHE ;//3. M - the least number of times a query has to be requested in order to be allowed to enter the cache.
//    public final static int RANDOM_RANGE ; //4. L - to specify the range [1, L] from which missing replies will be drawn uniformly at random.
//    public final static int NUMBER_OF_READER_THREADS; //5. Y - number of reader threads.
    public static void creatsThreadPools() {
        seachersPool = new ThreadPool(S_THREADS_NUM, "seachersPool");
        cashReadersPool = new ThreadPool(NUMBER_OF_READER_THREADS, "cashReadersPool");
        dBreadersPool = new ThreadPool(NUMBER_OF_READER_THREADS, "dBreadersPool");
    }

    public static void startWriteThreads() {
        new Thread("DbWriterThread") {
            @Override
            public void run() {
                while (true) {
                    DatabaseManager.writeAll();
                    System.out.println("------------update DB completed---------------");
//                    System.out.println((char) 27 + "[47;35m" + "------------update DB completed---------------" + (char) 27 + "[0;0m");

                }
            }
        }.start();
        new Thread("CashWriteThread") {
            @Override
            public void run() {
                while (true) {
                    CacheManager.updateCache();

                    System.out.println("------------update cash completed---------------");
//                    System.out.println((char) 27 + "[47;36m" + "------------update cache completed---------------" + (char) 27 + "[0;0m");

                }
            }
        }.start();
    }

    public static void startConnection() {

        try {
            serverSocket = new ServerSocket(PORT);
            SyncArrayList<InOutStreams> streamList = new SyncArrayList<>();
            ConnectionManager connectionM = new ConnectionManager(serverSocket, streamList, lock);
            new Thread(connectionM).start();
            new RequestMonitor(streamList, seachersPool, cashReadersPool, dBreadersPool, lock).start();
        } catch (IOException ex) {
            System.out.println("cannot creats server socket");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 4) { // for test only
            S_THREADS_NUM = Integer.parseInt(args[0]); //1. S - numbert of allowed S-threads.
            CACHE_SIZE = Integer.parseInt(args[1]); //C - size of the cache.
            LEAST_TO_CACHE = Integer.parseInt(args[2]);//3. M - the least number of times a query has to be requested in order to be allowed to enter the cache.
            RANDOM_RANGE = Integer.parseInt(args[3]); //4. L - to specify the range [1, L] from which missing replies will be drawn uniformly at random.
            NUMBER_OF_READER_THREADS = Integer.parseInt(args[4]); //5. Y - number of reader threads.
        }
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

}
