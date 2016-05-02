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
import java.util.HashSet;
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
    }

    final static int MAX_CAPACITY = 100;
    final static int OBJECT_SIZE = Integer.BYTES * 3;
    final static int NOT_FOUND = -1;
    final static int INITIAL_VALUE = 1;
    final static int L = 100;

    private static ReadWriteLock readWriteLock = new ReadWriteLock();
    private static HashMap<Integer, YandZ> toWrite = new HashMap<>();

    private Database() {
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
    private static int readDBHelper(int x) {

        try {
            String fileName = getFileName(x);

            RandomAccessFile raf = new RandomAccessFile(fileName, "r");

            int position = getPosition(x);
            raf.seek(position);
            int read = raf.readInt();
            if (read == 0) {
                if (x == 0) {
                    raf.seek(x + Integer.BYTES);
                    return raf.readInt();
                } else {
                    raf.close();
                    return NOT_FOUND;
                }
            } else {
                raf.seek(position + Integer.BYTES);
                int ans = raf.readInt();
                raf.close();
                return ans;
            }
        } catch (FileNotFoundException|EOFException ex) {
            return NOT_FOUND;
        } catch (IOException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return NOT_FOUND;
        }
    }

    public static int readY(int query) {

        try {
            readWriteLock.lockRead();
            int ans = readDBHelper(query);
            YandZ temp;
            if ((temp = toWrite.get(query)) != null) {
                temp.z++;
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
            ex.printStackTrace();
        } finally {
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
    private static void writeXYZ(int x, int y, int incZ) throws IOException, InterruptedException {
        String fileName = getFileName(x);
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

        int position = getPosition(x);
        raf.seek(position);
        raf.writeInt(x);
        raf.writeInt(y);
        int read = raf.readInt();
        raf.seek(position + 2 * Integer.BYTES);
        raf.writeInt(read + incZ);

        raf.close();
    }

    public static void writeAll() throws IOException, InterruptedException {
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
    public static void main(String[] args) throws IOException, InterruptedException {

    }
}
