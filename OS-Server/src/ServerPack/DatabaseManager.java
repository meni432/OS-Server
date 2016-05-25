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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Meni Samet
 */
public final class DatabaseManager {
     /* a temporary structure to hold the elements who needs to be write to the DB */
    private static final HashMap<Integer, XYZ> elemToUpdateDB = new HashMap<>();
    final static int FILE_MAX_CAPACITY = 100; // FILE_MAX_CAPACITY (x,y,z) trio on each file
    final static int UPDATE_DB_REACHED = 100; // UPDATE_DB_REACHED 
    final static int OBJECT_SIZE = Integer.BYTES * 3; // trio (x,y,z) size in bytes
    final static int NOT_FOUND = -1; // indicate that the trio not found
    final static int INITIAL_VALUE = 1; // z initial value
    /* when last update time minus current time bigger than TIME_TO_UPDATE_MS writer DB thread start update */    
    private static final int TIME_TO_UPDATE_MS = 5000;
     /* readWriteLock to synchronize between DB read-write action */   
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
     /* semaphore that holds the writer DB thread until TIME_TO_UPDATE_MS or UPDATE_DB_REACHED reached */
    private static final Semaphore semUpdateDB = new Semaphore(0);
    /* holds the last update DB time */    
    private static long LastUpdate = System.currentTimeMillis();

    
    /**
     * singleton design pattern 
     */
    private DatabaseManager() {
    } 

    /**
     * generate file name for DB
     *
     * @param x the request value
     * @return filename contain the result
     */
    private static String getFileName(int x) {
        return "" + (x / FILE_MAX_CAPACITY) * FILE_MAX_CAPACITY + ".db";
    }

    /**
     * @param x the request value
     * @return position of the first byte
     */
    private static int getPosition(int x) {
        return (x % (FILE_MAX_CAPACITY)) * OBJECT_SIZE;
    }

    /**
     * return y value for given x
     *
     * @param x the request value
     * @return y value
     * @throws IOException
     */
    private static XYZ readDBHelper(int x) {
        RandomAccessFile raf = null;
        try {
            String fileName = getFileName(x);
            raf = new RandomAccessFile(fileName, "r");
            int position = getPosition(x);
            raf.seek(position);
            int read = raf.readInt();
            if (read == 0) {
                if (x == 0) {
                    raf.seek(Integer.BYTES); //TODO i this this is a bug!!
                    return new XYZ(x, raf.readInt(), raf.readInt());
                } else {
                    return new XYZ(x, NOT_FOUND, NOT_FOUND);
                }
            } else {
                raf.seek(position + Integer.BYTES);
                return new XYZ(x, raf.readInt(), raf.readInt());
            }
        } catch (FileNotFoundException | EOFException ex) {
            return new XYZ(x, NOT_FOUND, NOT_FOUND);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new XYZ(x, NOT_FOUND, NOT_FOUND);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    /**
     * 
     * @param toPut trio XYZ to update z from cache to DB
     */
    public static void updateFromCash(XYZ toPut) {
        try {
            readWriteLock.lockRead();
            elemToUpdateDB.put(toPut.getX(), toPut);
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
    }
/**
 * search query x in the DB 
 * @param query x 
 * @return y answer
 */
    public static int readY(int query) {

        try {
            readWriteLock.lockRead();

            int ans;
            XYZ trio;
            if ((trio = elemToUpdateDB.get(query)) != null) { // if the trio founds in the elemToUpdateDB temporay structure
                trio.incZ();
                ans = trio.getY(); 
            } else { // search in the DB files
                trio = readDBHelper(query);
                ans = trio.getY();

                XYZ toUpdate = new XYZ();
                if (ans != NOT_FOUND) { // if the trio in the DB
                    toUpdate.setY(ans);
                } else { // create random y for query x
                    ans = (int) (Math.random() * Server.RANDOM_RANGE) + 1;
                    toUpdate.setY(ans);
                }
                toUpdate.incZ();
                elemToUpdateDB.put(query, toUpdate); // put toUpdate in the temporary structure
            }
            
            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (elemToUpdateDB.size() >= UPDATE_DB_REACHED || diffTime > TIME_TO_UPDATE_MS) { // invoke DB writer Thread
                semUpdateDB.release();
            }
            if (trio.getZ() > CacheManager.minZ) { // add the trio to the cathe
                CacheManager.addXYZtoCash(query, trio.getY(), trio.getZ());
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
     *  write trio (x,y,z) to the DB files
     * @param x query
     * @param y answer
     * @param incZ increment z in the given incZ
     */
    private static void writeXYincZ(int x, int y, int incZ) {
        RandomAccessFile raf = null;
        try {
            String fileName = getFileName(x);
            raf = new RandomAccessFile(fileName, "rw");

            int position = getPosition(x);
            raf.seek(position);
            raf.writeInt(x);
            raf.writeInt(y);
            int read = raf.readInt();// go to catch IOE if it's the first time, than write z=1
            raf.seek(position + 2 * Integer.BYTES);
            raf.writeInt(read + incZ);
            raf.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            try {
                if (raf != null) {
                    raf.writeInt(1);
                }
            } catch (IOException ex1) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * write / update new (x, y, newZ)
     *
     * @param x the request value
     * @param y answer
     * @param newZ 
     */
    private static void writeXYOvverideZ(int x, int y, int newZ) {
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
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            try {
                if (raf != null) {
                    raf.writeInt(1);
                }
            } catch (IOException ex1) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }
    /**
     * write all the elements from elemToUpdateDB to the DB
 
     */
    public static void updateDB() {
        try {
            long diffTime = System.currentTimeMillis() - LastUpdate;
            // Check if this is the time to update, if not , wait
            while (elemToUpdateDB.size() < UPDATE_DB_REACHED && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateDB.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            readWriteLock.lockWrite();
            try {
                Set<Entry<Integer, XYZ>> set = elemToUpdateDB.entrySet();
                Iterator<Entry<Integer, XYZ>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<Integer, XYZ> elm = (Entry<Integer, XYZ>) itr.next();
                    if (elm.getValue().isOverWriteZ()) { // if the element came from the cache, overwrite z
                        writeXYOvverideZ(elm.getKey(), elm.getValue().getY(), elm.getValue().getZ());
                    } else { // inc z
                        writeXYincZ(elm.getKey(), elm.getValue().getY(), elm.getValue().getZ());
                    }
                }
                elemToUpdateDB.clear();
                LastUpdate = System.currentTimeMillis();
            } finally {
                readWriteLock.unlockWrite();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * only for test DB files
     */
    public static void main(String[] args) {
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