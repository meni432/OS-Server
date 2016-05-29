package ServerPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * This class manage the cash, the class provide static function to update, add
 * and search in cash
 *
 */
public class CacheManager {

    /* a temporary structure to hold the elements who needs to be write to the cache */
    private static final SyncHashMap<Integer, XYZ> elemToUpdateCache = new SyncHashMap<>();
    /* readWriteLock to synchronize between cache read/write action */
    private static final ReadWriteLock readWriteLock = new ReadWriteLock();
    /* semaphore that holds the writer cache thread until TIME_TO_UPDATE_MS or UPDATE_CACHE_REACHED reached */
    private static final Semaphore semUpdateCache = new Semaphore(0);
    /* cache that holds query as a key, and XYZ as the value */
    private static final HashMap<Integer, XYZ> cache = new HashMap<>();
    /* when elemToUpdateCache structure reach to this size, writer thread start updating the cache*/
    private static final int UPDATE_CACHE_REACHED = Server.CACHE_SIZE / 3;
    /* global minZ that holds the min value in the cathe, only elements bigger then him will enter the cache*/
    private static int minZ = Server.LEAST_TO_CACHE;
    /* when last update time minus current time bigger than TIME_TO_UPDATE_MS writer cash thread start update */
    private static final int TIME_TO_UPDATE_MS = 5000;
    /* holds the last update cache time */
    private static long LastUpdate = System.currentTimeMillis();


    /**
     * singleton design pattern
     */
    private CacheManager() {
    }
        
    /**
     * add element to cash if z bigger that global minZ the method put the
     * element in the temp structure elemToUpdateCache
     *
     * @param xyz (x,y,z) 
     */
    public static void addXYZtoCash(XYZ xyz) {
        try {
            readWriteLock.lockRead();
            if (xyz.getZ() >= getMinZ()) {
                elemToUpdateCache.put(xyz.getX(), xyz);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            readWriteLock.unlockRead();
        }
    }
    public static void updateCache() {
        List<XYZ> union = new ArrayList<>();
        try {
            // Check if this is the time to update, if not, wait
            long diffTime = System.currentTimeMillis() - LastUpdate;
            while (elemToUpdateCache.size() < UPDATE_CACHE_REACHED && diffTime < TIME_TO_UPDATE_MS) {
                semUpdateCache.acquire();
                diffTime = System.currentTimeMillis() - LastUpdate;
            }
            readWriteLock.lockWrite(); // invoke write lock, reading is forbidden 
            union.addAll(cache.values());
            union.addAll(elemToUpdateCache.values());         
            union.removeAll(Collections.singleton(null)); // remove all null values from union         
            cache.clear();
            elemToUpdateCache.clear();
            Collections.sort(union, new Comparator<XYZ>() {    // sort the elements by Z value but offside
                @Override
                public int compare(XYZ o1, XYZ o2) {
                    return Integer.compare(o2.getZ(), o1.getZ());
                }
            });
            for (int i = 0; i < Server.CACHE_SIZE && i < union.size(); i++) {
                XYZ current = union.get(i);
                cache.put(current.getX(), current);
            }
            if (union.size() > Server.CACHE_SIZE) {
                minZ = union.get(union.size() - 1).getZ();
            }
            for (int i = Server.CACHE_SIZE; i < union.size(); i++) {
                DatabaseManager.getInstance().updateFromCash(union.get(i));

            }
            union.clear();
            LastUpdate = System.currentTimeMillis();
        } catch (InterruptedException ex) {
        } finally {
            readWriteLock.unlockWrite();
        }
    }
    public static int getMinZ() {
        return minZ;
    }
    /**
     * search in cache, if it's found , increment z by one.
     *
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