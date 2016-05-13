package ServerPack;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Meni Samet
 */
public class SyncArrayList<T> {

    private ArrayList<T> array = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock(true);

    public void add(T data) {
        lock.lock();
        try {
            array.add(data);
        } finally {
            lock.unlock();
        }
    }

    public T get(int index) {
        lock.lock();
        try {
            return array.get(index);
        } finally {
            lock.unlock();
        }
    }

    public T remove(int index) {
        lock.lock();
        try {
            return array.remove(index);
        } finally {
            lock.unlock();
        }
    }
    
    public int size(){
        lock.lock();
        try {
            return array.size();
        } finally {
            lock.unlock();
        }
    }

 

}
