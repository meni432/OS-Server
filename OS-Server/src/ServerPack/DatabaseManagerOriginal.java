package ServerPack;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Management the writing and reading from file
 */
public class DatabaseManagerOriginal {

    public static DatabaseManagerOriginal _instance = new DatabaseManagerOriginal();

    /* a temporary structure to hold the elements who needs to be write to the DB */
    private static final SyncHashMap<Integer, XYZ> elemToUpdateDB = new SyncHashMap<>();
    final static int FILE_MAX_CAPACITY = 100; // FILE_MAX_CAPACITY (x,y,z) trio on each file
    final static String PATCH_DB = "./DB-Files/";
    final static int UPDATE_DB_REACHED = 100; // UPDATE_DB_REACHED 
    final static int OBJECT_SIZE = Integer.BYTES * 3; // trio (x,y,z) size in bytes
    final static int NOT_FOUND = -1; // indicate that the trio not found
    final static int INITIAL_VALUE = 1; // z initial value
    /* when last update time minus current time bigger than TIME_TO_UPDATE_MS writer DB thread start update */
    private static final int TIME_TO_UPDATE_MS = 1000;
    /* readWriteLock to synchronize between DB read-write action */
    private final ReadWriteLock readWriteLock = new ReadWriteLock();
    /* semaphore that holds the writer DB thread until TIME_TO_UPDATE_MS or UPDATE_DB_REACHED reached */
    private final Semaphore semUpdateDB = new Semaphore(0);
    /* holds the last update DB time */
    private static long LastUpdate = System.currentTimeMillis();

    private boolean needUpdate = false;

    /**
     * singleton design pattern
     */
    private DatabaseManagerOriginal() {
    }

    public static DatabaseManagerOriginal getInstance() {
        return _instance;
    }

    /**
     * generate file name for DB
     *
     * @param x the request value
     * @return filename contain the result
     */
    private String getFileName(int x) {
        if (x >= 0) { // in case that x is positive integer
            return PATCH_DB + "" + (x / FILE_MAX_CAPACITY) * FILE_MAX_CAPACITY + ".db";
        } else { // in case that x is negative integer, put in diffrent file
            x = Math.abs(x);
            return PATCH_DB + "" + (x / FILE_MAX_CAPACITY) * FILE_MAX_CAPACITY + "-NEG.db";
        }
    }

    /**
     * @param x the request value
     * @return position of the first byte
     */
    private int getPosition(int x) {
        // in file, whenever x is negative or positive, mapping like is possitive
        x = Math.abs(x);
        return (x % (FILE_MAX_CAPACITY)) * OBJECT_SIZE;
    }

    /**
     * return y value for given x
     *
     * @param x the request value
     * @return y value
     * @throws IOException
     */
    private XYZ readDBHelper(int x) {
        RandomAccessFile raf = null;
        try {
            String fileName = getFileName(x);
            raf = new RandomAccessFile(fileName, "r");
            int position = getPosition(x);
            raf.seek(position);
            int read = raf.readInt();
            if (read == 0) {
                if (x == 0) {
                    raf.seek(position + Integer.BYTES); //TODO i this this is a bug!!
                    return new XYZ(x, raf.readInt(), raf.readInt());
                } else {
                    return new XYZ(x, NOT_FOUND, 0);
                }
            } else {
                raf.seek(position + Integer.BYTES);
                return new XYZ(x, raf.readInt(), raf.readInt());
            }
        } catch (FileNotFoundException | EOFException ex) {
            return new XYZ(x, NOT_FOUND, 0);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new XYZ(x, NOT_FOUND, 0);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    Logger.getLogger(DatabaseManagerOriginal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     *
     * @param toPut trio XYZ to update z from cache to DB
     */
    public void updateFromCash(XYZ toPut) {
        toPut.setOverWriteZ(true);
        elemToUpdateDB.getIterationLock().lock();
        elemToUpdateDB.put(toPut.getX(), toPut);
        elemToUpdateDB.getIterationLock().unlock();
    }

    /**
     * search query x in the DB
     *
     * @param query x
     * @return y answer
     */
    public int readY(int query) {

        try {
            readWriteLock.lockRead();

            int ans;
            XYZ xyzObject;
            if ((xyzObject = elemToUpdateDB.get(query)) != null) { // if the trio founds in the elemToUpdateDB temporay structure
                xyzObject.incZ();
                ans = xyzObject.getY();
            } else { // search in the DB files
                xyzObject = readDBHelper(query);
                ans = xyzObject.getY();

                if (ans != NOT_FOUND) { // if the trio in the DB
                    xyzObject.setY(ans);
                } else { // create random y for query x
                    ans = (int) (Math.random() * Server.RANDOM_RANGE) + 1;
                    xyzObject.setY(ans);
                    xyzObject.zeroZ();
                }
                xyzObject.incZ();
                elemToUpdateDB.getIterationLock().lock();
                try {
                    elemToUpdateDB.put(query, xyzObject); // put toUpdate in the temporary structure
                } finally {
                    elemToUpdateDB.getIterationLock().unlock();
                }
            }

            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (elemToUpdateDB.size() >= UPDATE_DB_REACHED) { // invoke DB writer Thread
                needUpdate = true;
                semUpdateDB.release();
            }
            if (xyzObject.getZ() > CacheManager.getMinZ()) { // add the trio to the cathe
                CacheManager.addXYZtoCash(xyzObject);
            }
            return ans;

        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return NOT_FOUND;
        } finally {
            readWriteLock.unlockRead();
        }
    }

    /**
     * write / update new (x, y, newZ)
     *
     * @param x the request value
     * @param y answer
     * @param newZ
     */
    private void writeXYOvverideZ(int x, int y, int newZ) {
        RandomAccessFile raf = null;
        try {
            String fileName = getFileName(x);
            raf = new RandomAccessFile(fileName, "rw");

            int position = getPosition(x);
            raf.seek(position);
            raf.writeInt(x);
            raf.writeInt(y);
            raf.writeInt(newZ);
            raf.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseManagerOriginal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
//            try {
//                if (raf != null) {
//                    raf.writeInt(1);
//                }
//            } catch (IOException ex1) {
//                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex1);
//            }

            ex.printStackTrace();
        }
    }

    /**
     * write all the elements from elemToUpdateDB to the DB
     *
     */
    public void updateDB() {
        try {
            long diffTime = System.currentTimeMillis() - LastUpdate;
            // Check if this is the time to update, if not , wait
//            while (elemToUpdateDB.size() < UPDATE_DB_REACHED) {
//                semUpdateDB.acquire();
//                diffTime = System.currentTimeMillis() - LastUpdate;
//            }

            while (needUpdate == false) {
                Thread.sleep(10);
            }
            
            readWriteLock.lockWrite();
            elemToUpdateDB.getIterationLock().lock();
            try {
                Set<Entry<Integer, XYZ>> set = elemToUpdateDB.entrySet();
                Iterator<Entry<Integer, XYZ>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<Integer, XYZ> elm = (Entry<Integer, XYZ>) itr.next();
                    writeXYOvverideZ(elm.getKey(), elm.getValue().getY(), elm.getValue().getZ());
                }
                elemToUpdateDB.clear();
                LastUpdate = System.currentTimeMillis();
            } finally {
                elemToUpdateDB.getIterationLock().unlock();
                readWriteLock.unlockWrite();
                needUpdate = false;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManagerOriginal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * only for test DB files
     */
    public void main(String[] args) {
        try {
            // debug DB file
            String fileName = "3300.db";
            RandomAccessFile raf = new RandomAccessFile(fileName, "r");
            for (int i = 0; i < 100; i++) {
                System.out.println("x[" + i + "]= " + raf.readInt() + " y[" + i + "]= " + raf.readInt() + " z[" + i + "]= " + raf.readInt());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseManagerOriginal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DatabaseManagerOriginal.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
