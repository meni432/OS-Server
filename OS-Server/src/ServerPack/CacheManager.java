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

/**
 * This class manage the cash, the class provide static function to update, add
 * and search in cash
 */
public class CacheManager {

    private static final HashMap<Integer, XYZ> toUpdate = new HashMap<>();
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
    private static final Semaphore semUpdateCash = new Semaphore(0);
    private static final HashMap<Integer, XYZ> cache = new HashMap<>();
    private static final int QUEUE_SIZE_TO_UPDATE = Server.CACHE_SIZE / 3;
    public static int minZ = Server.LEAST_TO_CACHE;

    private static final int TIME_TO_UPDATE_MS = 5000;
    private static long LastUpdate = System.currentTimeMillis();

    private CacheManager() {
    } // singleTone desgin Pattern

    /**
     * add element to cash
     *
     * @param x
     * @param y
     * @param z
     */
    public static void addXYZtoCash(int x, int y, int z) {

        try {
            readWriteLock.lockRead();

            if (z >= minZ) {
                XYZ xYz = new XYZ(x, y, z);
                toUpdate.put(x, xYz);//If the map previously contained a mapping for the key, the old value is replaced

            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            readWriteLock.unlockRead();
        }
    }

    /**
     * start update operation, Deletes old elements, to create a place for a new
     * ones
     */
    public static void updateCache() {
        try {
            // Check if this is the time to update, if not time, wait
            long diffTime = System.currentTimeMillis() - LastUpdate;
            while (toUpdate.size() < QUEUE_SIZE_TO_UPDATE && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateCash.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            readWriteLock.lockWrite();

            List<XYZ> values = new ArrayList<>(cache.values()); // create a list of all element currently in the cache
            Collections.sort(values, new Comparator<XYZ>() {

                @Override
                public int compare(XYZ o1, XYZ o2) {
                    return Integer.compare(o1.getZ(), o2.getZ());
                }
            });
            Set<Map.Entry<Integer, XYZ>> set = toUpdate.entrySet();
            Iterator<Map.Entry<Integer, XYZ>> itr = set.iterator();

            if (cache.size() + toUpdate.size() > Server.CACHE_SIZE) { // check if the size of cash set and toUpdate set is overflowed
                for (int i = 0; i < toUpdate.size(); i++) {  // removing the amount of ToUpdate    
                    try {
                        XYZ UpdateCash = new XYZ(values.get(i).getX(), values.get(i).getY(), values.get(i).getZ());
                        UpdateCash.setOverWriteZ(true);
                        DatabaseManager.updateFromCash(UpdateCash);
                        // update values.get(i)
                        cache.remove(values.get(i).getX());
                    } catch (IndexOutOfBoundsException ex) {
                        break;
                    }
                }
                if (values.size() >= toUpdate.size()) {
                    minZ = values.get(toUpdate.size()).getZ();
                }
            }
            for (int i = 0; itr.hasNext() && cache.size() <= Server.CACHE_SIZE; i++) {
                Map.Entry<Integer, XYZ> elem = itr.next();
                cache.put(elem.getKey(), elem.getValue());
            }
            toUpdate.clear();
            LastUpdate = System.currentTimeMillis();
        } catch (InterruptedException ex) {
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    /**
     * search in cache
     * @param x value to search
     * @return y value of x
     */
    public static int searchCash(int x) {
        try {
            readWriteLock.lockRead();
            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (toUpdate.size() >= QUEUE_SIZE_TO_UPDATE || diffTime >= TIME_TO_UPDATE_MS) {
                semUpdateCash.release();
            }
            XYZ ans = cache.get(x);
            if (ans == null) {
                return DatabaseManager.NOT_FOUND; 
            } else {
                ans.incZ();
                return ans.getY();
            }

        } catch (InterruptedException ex) {
            return DatabaseManager.NOT_FOUND;
        } finally {
            readWriteLock.unlockRead();
        }

    }

    public static void main(String[] args) throws InterruptedException {

        CacheManager c = new CacheManager();

        for (int i = 0; i < 10; i++) {
            CacheManager.addXYZtoCash(i, 2 * i, Server.LEAST_TO_CACHE + i);
        }
        CacheManager.updateCache();
        System.out.println(c.cache.toString());
        for (int i = 0; i < 10; i += 2) {
            CacheManager.addXYZtoCash(i, 2 * i, Server.LEAST_TO_CACHE * 2);
        }
        CacheManager.updateCache();
        System.out.println(c.cache.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 37; i++) {
            CacheManager.addXYZtoCash(i, 2 * i, 100);
        }
        CacheManager.updateCache();
        System.out.println(CacheManager.cache.toString());
        System.out.println("minZ= " + minZ);
        for (int i = 30; i < 50; i++) {
            CacheManager.addXYZtoCash(i, 2 * i, 100);
        }
        CacheManager.updateCache();
        System.out.println(CacheManager.cache.toString());
        System.out.println("minZ= " + minZ);

    }

}
