package ServerPack;

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
public class CashManager {

    private static final HashMap<Integer, XYZ> toUpdate = new HashMap<>();
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
    private static final Semaphore semUpdateCash = new Semaphore(0);
    private static final HashMap<Integer, XYZ> cash = new HashMap<>();
    private static final int QUEUE_SIZE_TO_UPDATE = Server.CASH_SIZE / 3;
    public static int minZ = Server.LEAST_TO_CACHE;

    private static final int TIME_TO_UPDATE_MS = 5000;
    private static long LastUpdate = System.currentTimeMillis();

    private CashManager() {
    } // singleTone desgin Pattern

    public static void addXYZtoCash(int x, int y, int z) {

        try {
            readWriteLock.lockRead();

            if (z >= minZ) {
                XYZ xYz = new XYZ(x, y, z);
                toUpdate.put(x, xYz);//If the map previously contained a mapping for the key, the old value is replaced

            }
        } catch (InterruptedException ex) {
            Logger.getLogger(CashManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
    }

    public static void updateCash() {
        try {
            long diffTime = System.currentTimeMillis() - LastUpdate;
            while (toUpdate.size() < QUEUE_SIZE_TO_UPDATE && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateCash.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            readWriteLock.lockWrite();

            List<XYZ> values = new ArrayList<>(cash.values());
            Collections.sort(values, new Comparator<XYZ>() {

                @Override
                public int compare(XYZ o1, XYZ o2) {
                    return Integer.compare(o1.getZ(), o2.getZ());
                }
            });
            Set<Map.Entry<Integer, XYZ>> set = toUpdate.entrySet();
            Iterator<Map.Entry<Integer, XYZ>> itr = set.iterator();

            if (cash.size() + toUpdate.size() > Server.CASH_SIZE) {
                for (int i = 0; i < toUpdate.size(); i++) {  // removing the amount of ToUpdate    
                    try {
                        DatabaseManager.YandZ UpdateCash = new DatabaseManager.YandZ(values.get(i).getY(), values.get(i).getZ());
                        UpdateCash.setOverWriteZ(true);
                        DatabaseManager.updateFromCash(values.get(i).getX(), UpdateCash);
                        // update values.get(i)
                        cash.remove(values.get(i).getX());
                    } catch (IndexOutOfBoundsException ex) {
                        break;
                    }
                }
                if (values.size() >= toUpdate.size()) {
                    minZ = values.get(toUpdate.size()).getZ();
                }
            }
            for (int i = 0; itr.hasNext() && cash.size() <= Server.CASH_SIZE; i++) {
                Map.Entry<Integer, XYZ> elem = itr.next();
                cash.put(elem.getKey(), elem.getValue());
            }
            toUpdate.clear();
            LastUpdate = System.currentTimeMillis();
        } catch (InterruptedException ex) {
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    public static int searchCash(int x) {
        try {
            readWriteLock.lockRead();
            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (toUpdate.size() >= QUEUE_SIZE_TO_UPDATE || diffTime >= TIME_TO_UPDATE_MS) {
                semUpdateCash.release();
            }
            //System.out.println("search cash fun");
            XYZ ans = cash.get(x);           
            if (ans == null) {
                return DatabaseManager.NOT_FOUND;
            } else {
                ans.incZ(); // also update th cash (refrance) , can cause syn problem?
                return ans.getY();
            }

        } catch (InterruptedException ex) {
            return DatabaseManager.NOT_FOUND;
        } finally {
            readWriteLock.unlockRead();
        }

    }

    public static void main(String[] args) throws InterruptedException {

        CashManager c = new CashManager();

        for (int i = 0; i < 10; i++) {
            CashManager.addXYZtoCash(i, 2 * i, Server.LEAST_TO_CACHE + i);
        }
        CashManager.updateCash();
        System.out.println(c.cash.toString());
        for (int i = 0; i < 10; i += 2) {
            CashManager.addXYZtoCash(i, 2 * i, Server.LEAST_TO_CACHE * 2);
        }
        CashManager.updateCash();
        System.out.println(c.cash.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 37; i++) {
            CashManager.addXYZtoCash(i, 2 * i, 100);
        }
        CashManager.updateCash();
        System.out.println(CashManager.cash.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 50; i++) {
            CashManager.addXYZtoCash(i, 2 * i, 100);
        }
        CashManager.updateCash();
        System.out.println(CashManager.cash.toString());
        System.out.println("minZ= " + minZ);

    }

}
