
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ofir Arnon
 */
public class CashManager implements Runnable {

    static class XYZ {

        int x;
        int y;
        int z;

        public XYZ(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;

        }

        @Override
        public String toString() {
            return "XYZ{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
        }

    }

    // lock Mechanizem ????
    private final static int SIZE = 100;
    public final static int M = 10;
    public static int minZ = M;
    private final HashMap<Integer, XYZ> toUpdate;
    private final ReadWriteLock readWriteLock = new ReadWriteLock();
    private final Semaphore semUpdateCash = new Semaphore(0);
    HashMap<Integer, XYZ> cash;
    

    /**
     * Constructor
     */
    public CashManager() {
        toUpdate = new HashMap<>();
        cash = new HashMap();
    }

    public void execute(int x, int y, int z) throws InterruptedException {

        readWriteLock.lockRead();
        try {//If the map previously contained a mapping for the key, the old value is replaced
            System.out.println("execute fun, before put");
            if (z >= minZ) {
                XYZ xYz = new XYZ(x, y, z);
                this.toUpdate.put(x, xYz);
                System.out.println("execute fun, after put");
                if (toUpdate.size() >= SIZE / 3) {
                    semUpdateCash.release();
                }
            }
        } finally {
            readWriteLock.unlockRead();
        }

    }

    public void updateCash() throws InterruptedException {
        while (toUpdate.size() < SIZE / 3) {
            semUpdateCash.acquire();
        }
        readWriteLock.lockWrite();
        try {
            System.err.println("stat update in update fun");
            List<XYZ> values = new ArrayList<>(cash.values());
            Collections.sort(values, new Comparator<XYZ>() {

                @Override
                public int compare(XYZ o1, XYZ o2) {
                    if (o1.z > o2.z) {
                        return 1;
                    } else if (o1.z < o2.z) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            Set<Map.Entry<Integer, XYZ>> set = toUpdate.entrySet();
            Iterator<Map.Entry<Integer, XYZ>> itr = set.iterator();

            if (cash.size() + toUpdate.size() > SIZE) {
                for (int i = 0; i < toUpdate.size(); i++) {  // removing the amount of ToUpdate    
                    try {
                        Database.updateFromCash(values.get(i).x, values.get(i).y, 1);
                        cash.remove(values.get(i).x);
                    } catch (IndexOutOfBoundsException ex) {
                        break;
                    }
                }
                if (values.size() >= toUpdate.size()) {
                    minZ = values.get(toUpdate.size()).z;
                }
            }
            for (int i = 0; itr.hasNext() && cash.size() <= SIZE; i++) {
                Map.Entry<Integer, XYZ> elem = itr.next();
                cash.put(elem.getKey(), elem.getValue());
            }
            System.err.println("done update in update fun");
            toUpdate.clear();
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    public int searchCash(int x) throws InterruptedException {
        readWriteLock.lockRead();
        try {
            //System.out.println("search cash fun");
            XYZ ans = cash.get(x);
            if (ans == null) {
                return Database.NOT_FOUND;
            } else {
                ans.z++; // also update th cash (refrance) , can cause syn problem?
                return ans.y;
            }
        } finally {
            readWriteLock.unlockRead();
        }

    }

    @Override
    public void run() {
        while (true) {
            try {
                updateCash();
            } catch (InterruptedException ex) {
                Logger.getLogger(CashManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        CashManager c = new CashManager();

        for (int i = 0; i < 10; i++) {
            c.execute(i, 2 * i, c.M + i);
        }
        c.updateCash();
        System.out.println(c.cash.toString());
        for (int i = 0; i < 10; i += 2) {
            c.execute(i, 2 * i, c.M * 2);
        }
        c.updateCash();
        System.out.println(c.cash.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 37; i++) {
            c.execute(i, 2 * i, 100);
        }
        c.updateCash();
        System.out.println(c.cash.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 50; i++) {
            c.execute(i, 2 * i, 100);
        }
        c.updateCash();
        System.out.println(c.cash.toString());
        System.out.println("minZ= " + minZ);

    }

}
