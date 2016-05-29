/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerPack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meni Samet
 * @param <K>
 * @param <V>
 */
public class SyncHashMap<K, V> {

    private final HashMap<K, V> hashMap = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReadWriteLock();
    private final ReentrantLock iterationLock = new ReentrantLock(true);


    public V put(K key, V value) {
        try {
            readWriteLock.lockWrite();
            return hashMap.put(key, value);
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockWrite();
        }
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        try {
            readWriteLock.lockWrite();
            hashMap.putAll(m);

        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    public V get(K key) {

        try {
            readWriteLock.lockRead();
            return hashMap.get(key);
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
        throw new RuntimeException();
    }

    public void clear() {

        try {
            readWriteLock.lockRead();
            hashMap.clear();
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
    }

    public int size() {

        try {
            readWriteLock.lockRead();
            return hashMap.size();
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockRead();
        }
        return -1;
    }

    public Set<Map.Entry<K, V>> entrySet() {

        try {
            readWriteLock.lockWrite();
            return hashMap.entrySet();
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncHashMap.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            readWriteLock.unlockWrite();
        }
        return null;
    }

    public Collection<V> values() {
        try {
            readWriteLock.lockRead();
            return hashMap.values();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            readWriteLock.unlockRead();
        }
        return null;
    }
    

    public ReentrantLock getIterationLock() {
        return iterationLock;
    }

}
