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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Meni Samet
 */
public final class Database {

    static class YandZ {

        int y;
        int z;

        public YandZ() {
            this.y = 0;
            this.z = 0;
        }

        public YandZ(int y, int z) {
            this.y = y;
            this.z = z;
        }

    }

    final static int MAX_CAPACITY = 100;
    final static int OBJECT_SIZE = Integer.BYTES * 3;
    final static int NOT_FOUND = -1;
    final static int INITIAL_VALUE = 1;
    final static int L = 100;
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
    private static CashManager cashM;

    private static HashMap<Integer, YandZ> toWrite = new HashMap<>();

    public Database(CashManager cashM) {
        Database.cashM = cashM;
    }

    /**
     * generate file name for DB
     *
     * @param x the request value
     * @return filename contain the result
     */
    private static String getFileName(int x) {
        return "" + (x / MAX_CAPACITY) * MAX_CAPACITY + ".db";
    }

    /**
     * @param x the request value
     * @return position of the first byte
     */
    private static int getPosition(int x) {
        return (x % (MAX_CAPACITY)) * OBJECT_SIZE;
    }

    /**
     * return y value for given x
     *
     * @param x the request value
     * @return y value
     * @throws IOException
     */
    private static YandZ readDBHelper(int x) {
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
                    return new YandZ(raf.readInt(), raf.readInt());
                } else {
                    return new YandZ(NOT_FOUND, NOT_FOUND);
                }
            } else {
                raf.seek(position + Integer.BYTES);
                return new YandZ(raf.readInt(), raf.readInt());
            }
        } catch (FileNotFoundException | EOFException ex) {
            return new YandZ(NOT_FOUND, NOT_FOUND);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new YandZ(NOT_FOUND, NOT_FOUND);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void updateFromCash(int x, int y, int incZ) {
        try {
            readWriteLock.lockRead();
            YandZ toPut = new YandZ();
            toPut.y = y;
            toPut.z = incZ;
            toWrite.put(x, toPut);
        } catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
    }

    public static int readY(int query) {

        try {
            readWriteLock.lockRead();
            YandZ yAndZ = readDBHelper(query);
            System.err.println("---------before execute----+ x=" + query + " y=" + yAndZ.y + " z=" + yAndZ.z);
            if (yAndZ.z > CashManager.minZ) {
                System.err.println("try execute in database");
                cashM.execute(query, yAndZ.y, yAndZ.z);
            }
            int ans = yAndZ.y;
            YandZ temp;
            if ((temp = toWrite.get(query)) != null) {
                temp.z++;
                ans = temp.y; // ofir added this line because we need the ans from the hashTable(ans=-1 if it's not in the DB) 
            } else if (ans != NOT_FOUND) {
                temp = new YandZ();
                temp.y = ans;
                temp.z = 1;
                toWrite.put(query, temp);
            } else {
                temp = new YandZ();
                ans = temp.y = (int) (Math.random() * L) + 1;
                temp.z = 1;
                toWrite.put(query, temp);
            }
            return ans;
        } catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }//        catch (InterruptedException ex) {
        //            ex.printStackTrace();    
        //        } 
        finally {
            readWriteLock.unlockRead();
        }

        return NOT_FOUND;
    }

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
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            try {
                if (raf != null) {
                    raf.writeInt(1);
                }
            } catch (IOException ex1) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    private static void writeXYZOverZ(int x, int y, int z) {
        RandomAccessFile raf = null;
        try {
            String fileName = getFileName(x);
            raf = new RandomAccessFile(fileName, "rw");

            int position = getPosition(x);
            raf.seek(position);
            raf.writeInt(x);
            raf.writeInt(y);
            raf.writeInt(z);
            raf.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            try {
                if (raf != null) {
                    raf.writeInt(1);
                }
            } catch (IOException ex1) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public static void writeAll() {
        try {
            readWriteLock.lockWrite();
            try {
                Set<Entry<Integer, YandZ>> set = toWrite.entrySet();
                Iterator<Entry<Integer, YandZ>> itr = set.iterator();
                while (itr.hasNext()) {
                    Entry<Integer, YandZ> elm = (Entry<Integer, YandZ>) itr.next();
                    writeXYZ(elm.getKey(), elm.getValue().y, elm.getValue().z);
                }
                toWrite.clear();
            } finally {
                readWriteLock.unlockWrite();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
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
            String fileName = "600.db";
            RandomAccessFile raf = new RandomAccessFile(fileName, "r");
            for (int i = 0; i < 100; i++) {
                System.out.println("x[" + i + "]= " + raf.readInt() + " y[" + i + "]= " + raf.readInt() + " z[" + i + "]= " + raf.readInt());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
