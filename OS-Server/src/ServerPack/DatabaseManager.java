package ServerPack;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
    private static final SyncHashMap<Integer, XYZ> elemToUpdateDB = new SyncHashMap<>();
    final static int FILE_MAX_CAPACITY = 1000; // FILE_MAX_CAPACITY (x,y,z) trio on each file
    final static String PATCH_DB = "./DB-Files/";
    final static int UPDATE_DB_REACHED = 100; // UPDATE_DB_REACHED 
    final static int OBJECT_SIZE = Integer.BYTES * 3; // trio (x,y,z) size in bytes
    final static int NOT_FOUND = -1; // indicate that the trio not found
    final static int INITIAL_VALUE = 1; // z initial value

    /* readWriteLock to synchronize between DB read-write action */
    private final ReadWriteLock readWriteLock = new ReadWriteLock();

    private final Semaphore updateReached = new Semaphore(0, true);
    
    
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
     *
     * @param toPut trio XYZ to update z from cache to DB
     */
    public void updateFromCash(XYZ toPut) {
        toPut.setOverWriteZ(true);
        elemToUpdateDB.getIterationLock().lock();
        try {
            elemToUpdateDB.put(toPut.getX(), toPut);
        } finally {
            elemToUpdateDB.getIterationLock().unlock();
        }
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

                if (elemToUpdateDB.size() >= UPDATE_DB_REACHED) {
                    updateReached.release();
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
            readWriteLock.unlockRead();
        }
    }

    private void addAllXYZ(List<XYZ> list) {
        RandomAccessFile raf = null;
        try {
            Iterator<XYZ> listIterator = list.iterator();
            if (listIterator.hasNext() == false) {
                return;
            }
            XYZ element = listIterator.next();
            String fileName = getFileName(element.getX());
            raf = new RandomAccessFile(fileName, "rw");
            while (true) {
                int position = getPosition(element.getX());
                raf.seek(position);
                raf.writeInt(element.getX());
                raf.writeInt(element.getY());
                raf.writeInt(element.getZ());

                if (listIterator.hasNext() == false) {
                    raf.close();
                    return;
                }
                element = listIterator.next();
                String nextFileName = getFileName(element.getX());
                if (fileName.equals(nextFileName) == false) {
                    raf.close();
                    fileName = nextFileName;
                    raf = new RandomAccessFile(fileName, "rw");
                }
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * write all the elements from elemToUpdateDB to the DB
     *
     */
    public void updateDB() {
        try {

            readWriteLock.lockWrite();
            elemToUpdateDB.getIterationLock().lock();
            try {
                ArrayList<XYZ> elemToUpdateArray = new ArrayList<>(elemToUpdateDB.values());
                Collections.sort(elemToUpdateArray, new Comparator<XYZ>() {
                    @Override
                    public int compare(XYZ o1, XYZ o2) {
                        return Integer.compare(o1.getX(), o2.getX());
                    }
                });
                addAllXYZ(elemToUpdateArray);
                elemToUpdateDB.clear();
            } finally {
                elemToUpdateDB.getIterationLock().unlock();
                readWriteLock.unlockWrite();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getElementToUpdateDBSize() {
        return elemToUpdateDB.size();
    }

    public void chackForUpdate() {
        while (true) {
            while (elemToUpdateDB.size() < UPDATE_DB_REACHED){
                try {
                    updateReached.acquire();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            if (elemToUpdateDB.size() >= UPDATE_DB_REACHED) {
                updateDB();
            }
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
