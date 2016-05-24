package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    final static int FILE_MAX_CAPACITY = 100;
    final static int UPDATE_DB_REACHED = 100;
    final static int OBJECT_SIZE = Integer.BYTES * 3;
    final static int NOT_FOUND = -1;
    final static int INITIAL_VALUE = 1;
    private static final int TIME_TO_UPDATE_MS = 5000;
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
    private static final Semaphore semUpdateDB = new Semaphore(0);

    private static long LastUpdate = System.currentTimeMillis();

    private static HashMap<Integer, XYZ> toWrite = new HashMap<>();

    private DatabaseManager() {
    } // SingleTone desgin Pattern

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
                    raf.seek(Integer.BYTES);
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

    public static void updateFromCash(XYZ toPut) {
        try {
            readWriteLock.lockRead();
            toWrite.put(toPut.getX(), toPut);
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
    }

    public static int readY(int query) {

        try {
            readWriteLock.lockRead();

            int ans;
            XYZ yAndZAns;
            if ((yAndZAns = toWrite.get(query)) != null) {
                yAndZAns.incZ();
                ans = yAndZAns.getY(); // ofir added this line because we need the ans from the hashTable(ans=-1 if it's not in the DB) 
            } else {
                yAndZAns = readDBHelper(query);
                ans = yAndZAns.getY();

                XYZ toUpdate = new XYZ();
                if (ans != NOT_FOUND) {
                    toUpdate.setY(ans);
                } else {
                    ans = (int) (Math.random() * Server.RANDOM_RANGE) + 1;
                    toUpdate.setY(ans);
                }
                toUpdate.incZ();
                toWrite.put(query, toUpdate);
            }
            // cehck for cash update
            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (toWrite.size() >= UPDATE_DB_REACHED || diffTime > TIME_TO_UPDATE_MS) {
                semUpdateDB.release();
            }
            if (yAndZAns.getZ() > CacheManager.minZ) {
                CacheManager.addXYZtoCash(query, yAndZAns.getY(), yAndZAns.getZ());
            }
            return ans;

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            readWriteLock.unlockRead();
        }

        return NOT_FOUND;
    }
//    public static int readY(int query) {
//
//        try {
//            readWriteLock.lockRead();
//            XYZ yAndZ = readDBHelper(query);
//            if (yAndZ.getZ() > CashManager.minZ) {
//                CashManager.addXYZtoCash(query, yAndZ.getY(), yAndZ.getZ());
//            }
//            int ans = yAndZ.getY();
//            XYZ temp;
//            if ((temp = toWrite.get(query)) != null) {
//                temp.incZ();
//                ans = temp.getY(); // ofir added this line because we need the ans from the hashTable(ans=-1 if it's not in the DB) 
//            } else if (ans != NOT_FOUND) {
//                temp = new XYZ();
//                temp.setY(ans); 
//                temp.incZ();
//                toWrite.put(query, temp);
//            } else {
//                temp = new XYZ();
//                ans  = (int) (Math.random() * Server.RANDOM_RANGE) + 1;
//                temp.setY(ans);
//                temp.incZ();
//                toWrite.put(query, temp);
//            }
//            long diffTime = System.currentTimeMillis() - LastUpdate;
//            if (toWrite.size() >= UPDATE_DB_REACHED || diffTime > TIME_TO_UPDATE_MS) {
//                System.out.println("release update");
//                semUpdateDB.release();
//            }
//            return ans;
//        } catch (InterruptedException ex) {
//            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            readWriteLock.unlockRead();
//        }
//
//        return NOT_FOUND;
//    }

    /**
     * write new (x,y,1)
     *
     * @param x the request value
     * @param y
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    // TODO check change in raf
    private static void writeXYZ(int x, int y, int incZ) {
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
     * @param y
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

    public static void writeAll() {
        try {
            long diffTime = System.currentTimeMillis() - LastUpdate;
            while (toWrite.size() < UPDATE_DB_REACHED && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateDB.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            System.out.println("after acquire");
            readWriteLock.lockWrite();
            try {
                Set<Entry<Integer, XYZ>> set = toWrite.entrySet();
                Iterator<Entry<Integer, XYZ>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<Integer, XYZ> elm = (Entry<Integer, XYZ>) itr.next();
                    writeXYZ(elm.getKey(), elm.getValue().getY(), elm.getValue().getZ());
                }
                toWrite.clear();
                LastUpdate = System.currentTimeMillis();
            } finally {
                readWriteLock.unlockWrite();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * increase z value for given x and toInc (x,y,z+toInc)
     *
     * @param x the request value
     * @param toInc value to amount z ( z = z + toInc )
     * @throws IOException
     */
    /**
     * set new value for given x
     *
     * @param x the request value
     * @param z new value for z in (x,y,z)
     */
    public static void writeZ(int x, int z) throws IOException {
        String fileName = getFileName(x);
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

        int position = getPosition(x);
        raf.seek(position + Integer.BYTES);
        raf.writeInt(z);

        raf.close();
    }

    /**
     * only for test
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
