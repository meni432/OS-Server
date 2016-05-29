package ServerPack;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Management the writing and reading from file
 */
public class DatabaseManager {

    public static DatabaseManager _instance;
    private static final ReentrantLock instanceLock = new ReentrantLock(true);

    /* a temporary structure to hold the elements who needs to be write to the DB */
//    private static final SyncHashMap<Integer, XYZ> elemToUpdateDB = new SyncHashMap<>();
    final static int FILE_MAX_CAPACITY = 100; // FILE_MAX_CAPACITY (x,y,z) trio on each file
    final static String PATCH_DB = "./DB-Files/";
    final static int UPDATE_DB_REACHED = 100; // UPDATE_DB_REACHED 
    final static int OBJECT_SIZE = Integer.BYTES * 3; // trio (x,y,z) size in bytes
    final static int NOT_FOUND = -1; // indicate that the trio not found
    final static int INITIAL_VALUE = 1; // z initial value

    /* readWriteLock to synchronize between DB read-write action */
//    private final ReadWriteLock readWriteLock = new ReadWriteLock();
    private final Semaphore updateReached = new Semaphore(0, true);

    private final HashMap<String, ReadWriteLock> lockFiles = new HashMap<>();

    private final SyncHashMap<String, SyncHashMap<Integer, XYZ>> elemToUpdateDBFiles
            = new SyncHashMap<>();

    /**
     * singleton design pattern
     */
    private DatabaseManager() {
        //clean PATCH_DB folder
        File dir = new File(PATCH_DB);
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }

    public static DatabaseManager getInstance() {
        instanceLock.lock();
        try {
            if (_instance == null) {
                _instance = new DatabaseManager();
            }
            return _instance;
        } finally {
            instanceLock.unlock();
        }
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
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * search query x in the DB
     *
     * @param query x
     * @return y answer
     */
    public int readY(int query) {
        ReadWriteLock currentLock;
        SyncHashMap<Integer, XYZ> elemToUpdateDB;
        elemToUpdateDBFiles.getIterationLock().lock();
        try {
            String filename = getFileName(query);
            if (elemToUpdateDBFiles.get(filename) == null) {
                elemToUpdateDBFiles.put(filename, new SyncHashMap<>());
            }
            elemToUpdateDB = elemToUpdateDBFiles.get(filename);

            if (lockFiles.get(filename) == null) {
                lockFiles.put(filename, new ReadWriteLock());
            }
            currentLock = lockFiles.get(filename);
        } finally {
            elemToUpdateDBFiles.getIterationLock().unlock();
        }
        try {
            currentLock.lockRead();
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

            if (xyzObject.getZ() > CacheManager.getMinZ()) { // add the trio to the cathe
                CacheManager.addXYZtoCash(xyzObject);
            }
            return ans;

        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return NOT_FOUND;
        } finally {
            currentLock.unlockRead();
            if (getElementToUpdateDBSize() >= UPDATE_DB_REACHED) {
                updateReached.release();
            }
        }
    }

    private void addAllXYZ(SyncHashMap<Integer, XYZ> hashValues, String fileName) {
        ReentrantLock iterLock = hashValues.getIterationLock();
        ReadWriteLock fileRwl = lockFiles.get(fileName);
        try {
            System.out.println("before iter lock");
            iterLock.lock();
            System.out.println("after iter lock before write lock");
            fileRwl.lockWrite();
            System.out.println("after write lock");
            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
            for (XYZ xyz : hashValues.values()) {
                int position = getPosition(xyz.getX());
                raf.seek(position);
                raf.writeInt(xyz.getX());
                raf.writeInt(xyz.getY());
                raf.writeInt(xyz.getZ());
            }
            raf.close();
            hashValues.clear();

            System.out.println("current count :" + getElementToUpdateDBSize());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            iterLock.unlock();
            fileRwl.unlockWrite();
            System.err.println("after add db");
        }
    }

    /**
     * write all the elements from elemToUpdateDB to the DB
     *
     */
    public void updateDB() {
        System.err.println("call update db");
        elemToUpdateDBFiles.getIterationLock().lock();
        try {
            Iterator it = elemToUpdateDBFiles.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
//                System.out.println(pair.getKey() + " = " + pair.getValue());
                String fileName = (String) pair.getKey();
                SyncHashMap<Integer, XYZ> hashValues = (SyncHashMap<Integer, XYZ>) pair.getValue();
                System.out.println("before add all xyz");
                if (hashValues.size() > 0) {
                    addAllXYZ(hashValues, fileName);
                }
                System.out.println("after add all xyz");
//                it.remove(); // avoids a ConcurrentModificationException
            }
        } finally {
            elemToUpdateDBFiles.getIterationLock().unlock();
        }
    }

    public int getElementToUpdateDBSize() {
        return countAllInToUpdate();
    }

    public void chackForUpdate() {
        while (getElementToUpdateDBSize() < UPDATE_DB_REACHED) {
            try {
                updateReached.acquire();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        if (getElementToUpdateDBSize() >= UPDATE_DB_REACHED) {
            updateDB();
        }
    }

    private int countAllInToUpdate() {
        int totalCount = 0;
        for (SyncHashMap<Integer, XYZ> elem : elemToUpdateDBFiles.values()) {
            totalCount += elem.size();
        }
        return totalCount;
    }

    /**
     *
     * @param toPut trio XYZ to update z from cache to DB
     */
    public void updateFromCash(XYZ toPut) {
        try {
            String fileName = getFileName(toPut.getX());
            elemToUpdateDBFiles.getIterationLock().lock();
            if (elemToUpdateDBFiles.get(fileName) == null) {
                elemToUpdateDBFiles.put(fileName, new SyncHashMap<>());
            }
            elemToUpdateDBFiles.getIterationLock().unlock();
            SyncHashMap<Integer, XYZ> hashMap = elemToUpdateDBFiles.get(fileName);
            hashMap.getIterationLock().lock();
            try {
                hashMap.put(toPut.getX(), toPut);
            } finally {
                hashMap.getIterationLock().unlock();
            }
        } finally {
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
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
