/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverPack;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Meni Samet
 */
public final class Database {

    final static int MAX_CAPACITY = 100;
    final static int OBJECT_SIZE = Integer.BYTES * 3;
    final static int NOT_FOUNT = -1;
    final static int INITIAL_VALUE = 1;
    
    private Database() {}

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
    public static int readY(int x) throws IOException {
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
                return NOT_FOUNT;
            }
        } else {
            raf.seek(position + Integer.BYTES);
            int ans=raf.readInt();
            raf.close();
            return ans;
        }

    }

    /**
     * write new (x,y,1)
     *
     * @param x the request value
     * @param y
     * @throws java.io.IOException
     */
    public static void writeY(int x, int y) throws IOException {
        String fileName = getFileName(x);
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

        int position = getPosition(x);
        raf.seek(position);
        raf.writeInt(x);
        raf.writeInt(y);
        raf.writeInt(INITIAL_VALUE);

        raf.close();
    }

    /**
     * increase z value for given x and toInc (x,y,z+toInc)
     *
     * @param x the request value
     * @param toInc value to amount z ( z = z + toInc )
     * @throws IOException
     */
    public static void incZ(int x, int toInc) throws IOException {
        String fileName = getFileName(x);
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
        
        int position = getPosition(x);
        raf.seek(position + 2 * Integer.BYTES);
        int read = raf.readInt();
        raf.seek(position + 2 * Integer.BYTES);
        int newVal = read + toInc;
        System.out.println(newVal);
        raf.writeInt(newVal);
        
        raf.close();
    }

    /**
     *set new value for given x
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
    public static void main(String[] args) throws IOException {
        int start=65;
        for (int i = 0; i < 15; i++) {
            writeY(start++, i);
        }
       
    }
}