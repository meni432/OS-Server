package ServerPack;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Management the writing and reading from file
 */
public class DatabaseManager {

    public static DatabaseManager _instance;
    private static final ReentrantLock instanceLock = new ReentrantLock(true);

    final static int FILE_MAX_CAPACITY = 100; // FILE_MAX_CAPACITY (x,y,z) trio on each file
    final static String PATCH_DB = "./DB-Files/";
    final static int UPDATE_DB_REACHED = 300; // UPDATE_DB_REACHED 
    final static int OBJECT_SIZE = Integer.BYTES * 3; // trio (x,y,z) size in bytes
    final static int NOT_FOUND = -1; // indicate that the trio not found
    final static int INITIAL_VALUE = 1; // z initial value

    private final Semaphore updateReached = new Semaphore(0, true);

    /* individual lock file */
    private final SyncHashMap<String, ReadWriteLock> lockByFileName = new SyncHashMap<>(); //<File Name, lock> HashMap

    /* a temporary structure to hold the elements who needs to be write to the DB */
    private final SyncHashMap<String, SyncHashMap<Integer, XYZ>> elemToUpdateDBFiles
            = new SyncHashMap<>();

    /* all element in elemToUpdateDBFiles, for counting number of element in all Hash */
    private final HashMap<Integer, XYZ> counterHash = new HashMap<>();

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

    /**
     * @return DatabaseManager Single Object
     */
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
                    raf.seek(position + Integer.BYTES);
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
        String filename;

        filename = getFileName(query);
        // Hash Map for file
        if (elemToUpdateDBFiles.get(filename) == null) {
            elemToUpdateDBFiles.put(filename, new SyncHashMap<>(), true); // with iteration lock
        }
        elemToUpdateDB = elemToUpdateDBFiles.get(filename);

        // File Lock
        if (lockByFileName.get(filename) == null) {
            lockByFileName.put(filename, new ReadWriteLock(), true); // with iteration lock
        }
        currentLock = lockByFileName.get(filename);

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
                    counterHash.put(xyzObject.getX(), xyzObject);
                } finally {
                    elemToUpdateDB.getIterationLock().unlock();
                }

            }

            if (xyzObject.getZ() > CacheManager.getMinZ()) { // add the trio to the cathe
                CacheManager.addXYZtoCash(xyzObject);
            }

            if (getElementToUpdateDBSize() >= UPDATE_DB_REACHED) {
                updateReached.release();
            }

            return ans;

        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return NOT_FOUND;
        } finally {
            currentLock.unlockRead();
        }
    }

    private void addAllXYZ(SyncHashMap<Integer, XYZ> hashValues, String fileName) {
        RandomAccessFile raf = null;
        ReentrantLock iterLock = hashValues.getIterationLock();
        ReadWriteLock fileRwl = lockByFileName.get(fileName);
        try {
            fileRwl.lockWrite();
            raf = new RandomAccessFile(fileName, "rw");
            iterLock.lock();
            ArrayList<XYZ> array = new ArrayList<>(hashValues.values());
            array.removeAll(Collections.singleton(null)); // remove all null values from array
            Collections.sort(array); // sort by x
            for (XYZ xyz : hashValues.values()) {
                int position = getPosition(xyz.getX());
                raf.seek(position);
                raf.writeInt(xyz.getX());
                raf.writeInt(xyz.getY());
                raf.writeInt(xyz.getZ());
                counterHash.remove(xyz.getX());
            }
            hashValues.clear();

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            iterLock.unlock();
            fileRwl.unlockWrite();
        }
    }

    /**
     * write all the elements from elemToUpdateDB to the DB
     *
     */
    public void updateDB() {
        elemToUpdateDBFiles.getIterationLock().lock();
        try {
            Iterator it = elemToUpdateDBFiles.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String fileName = (String) pair.getKey();
                SyncHashMap<Integer, XYZ> hashValues = (SyncHashMap<Integer, XYZ>) pair.getValue();
                if (hashValues.size() > 0) { // check if there is values to write in this file
                    addAllXYZ(hashValues, fileName);
                }
            }
        } finally {
            elemToUpdateDBFiles.getIterationLock().unlock();
        }
    }

    /**
     * @return element in elemToUpdateDBFiles HashMap (wait for writing)
     */
    public int getElementToUpdateDBSize() {
        return counterHash.size();
    }

    /**
     * check if this is the time to update the database
     */
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

    /**
     * @param toPut trio XYZ to update z from cache to DB
     */
    public void updateFromCash(XYZ toPut) {
        try {
            String fileName = getFileName(toPut.getX());

            if (elemToUpdateDBFiles.get(fileName) == null) {
                elemToUpdateDBFiles.getIterationLock().lock();
                try {
                    elemToUpdateDBFiles.put(fileName, new SyncHashMap<>());
                } finally {
                    elemToUpdateDBFiles.getIterationLock().unlock();
                }
            }

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
