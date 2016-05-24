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


/**
 * This class manage the cash, the class provide static function to update, add
 * and search in cash
 *  
 */
public class CacheManager {
 /* a temporary structure to hold the elements who needs to be write to the cache */
    private static final HashMap<Integer, XYZ> elemToUpdateCache = new HashMap<>();
 /* readWriteLock to synchronize between cache read/write action */   
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
 /* semaphore that holds the writer cache thread until TIME_TO_UPDATE_MS or UPDATE_CACHE_REACHED reached */
    private static final Semaphore semUpdateCache = new Semaphore(0);
 /* cache that holds query as a key, and XYZ as the value */   
    private static final HashMap<Integer, XYZ> cache = new HashMap<>();
 /* when elemToUpdateCache structure reach to this size, writer thread start updating the cache*/   
    private static final int UPDATE_CACHE_REACHED = Server.CACHE_SIZE / 2;
/* global minZ that holds the min value in the cathe, only elements bigger then him will enter the cache*/    
    public static int minZ = Server.LEAST_TO_CACHE;
/* when last update time minus current time bigger than TIME_TO_UPDATE_MS writer cash thread start update */    
    private static final int TIME_TO_UPDATE_MS = 5000;
/* holds the last update cache time */    
    private static long LastUpdate = System.currentTimeMillis();

    /**
     * singleton design pattern 
     */
     private CacheManager() {}
     

    /**
     * add element to cash
     * if z bigger that global minZ
     *the method put the element in the temp structure elemToUpdateCache
     * @param x query
     * @param y answer
     * @param z counting
     */
    public static void addXYZtoCash(int x, int y, int z) {

        try {
            readWriteLock.lockRead();

            if (z >= minZ) {
                XYZ xYz = new XYZ(x, y, z);
                elemToUpdateCache.put(x, xYz);//If the map previously contained a mapping for the key, the old value is replaced

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
            // Check if this is the time to update, if not, wait
            long diffTime = System.currentTimeMillis() - LastUpdate;
            while (elemToUpdateCache.size() < UPDATE_CACHE_REACHED && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateCache.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            readWriteLock.lockWrite(); // invoke write lock, reading is forbidden 

            List<XYZ> values = new ArrayList<>(cache.values()); // create a list of all element currently in the cache
            Collections.sort(values, new Comparator<XYZ>() {    // sort the elements by Z value

                @Override
                public int compare(XYZ o1, XYZ o2) {
                    return Integer.compare(o1.getZ(), o2.getZ());
                }
            });
            Set<Map.Entry<Integer, XYZ>> set = elemToUpdateCache.entrySet();
            Iterator<Map.Entry<Integer, XYZ>> itr = set.iterator();

            if (cache.size() + elemToUpdateCache.size() > Server.CACHE_SIZE) { // check if the size of cash set and elemToUpdateCache set is overflowed
                for (int i = 0; i < elemToUpdateCache.size(); i++) {  // removing the amount of elemToUpdateCache    
                    try {
                        XYZ UpdateCash = new XYZ(values.get(i).getX(), values.get(i).getY(), values.get(i).getZ());
<<<<<<< HEAD
                        UpdateCash.setOverWriteZ(true); // when you update th DB dont inc Z , over write him
                        DatabaseManager.updateFromCash(UpdateCash);// when the elements removed from the cache update DB
=======
                        UpdateCash.setOverWriteZ(true);
                        DatabaseManager.updateFromCache(UpdateCash);
                        // update values.get(i)
>>>>>>> refs/remotes/origin/up-change
                        cache.remove(values.get(i).getX());
                    } catch (IndexOutOfBoundsException ex) {
                        break;
                    }
                }
                if (values.size() >= elemToUpdateCache.size()) {// update minZ to be the least number in the cathe that not removed
                    minZ = values.get(elemToUpdateCache.size()).getZ();
                }
            }
            for (int i = 0; itr.hasNext() && cache.size() <= Server.CACHE_SIZE; i++) {
                Map.Entry<Integer, XYZ> elem = itr.next();
                cache.put(elem.getKey(), elem.getValue()); // updating cash from elemToUpdateCache
            }
            elemToUpdateCache.clear(); // after updating is complete, clear the temporary structure
            LastUpdate = System.currentTimeMillis();
        } catch (InterruptedException ex) {
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    /**
     * search in cache, if it's found , increment z by one.
     * @param x value to search
     * @return if found return y value of x , else DatabaseManager.NOT_FOUND
     */
    public static int searchCash(int x) {
        try {
            readWriteLock.lockRead();
            long diffTime = System.currentTimeMillis() - LastUpdate;
            if (elemToUpdateCache.size() >= UPDATE_CACHE_REACHED || diffTime >= TIME_TO_UPDATE_MS) {
                semUpdateCache.release();
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
}
