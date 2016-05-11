package ServerPack;


import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

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

    private ArrayList<T> array;
    private final ReentrantLock lock = new ReentrantLock(true);
    
    public void add(T data){
        lock.lock();
        try{
            array.add(data);
        } finally {
            lock.unlock();
        }
    }
    
    public T get(int index){
        lock.lock();
        try{
            return array.get(index);
        }finally{
            lock.unlock();
        }
    }
    
    public T remove(int index) {
        lock.lock();
        try{
            return array.remove(index);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean remove(T data){
        lock.lock();
        try{
            return array.remove(data);
        } finally {
            lock.unlock();
        }
    }
    
    
}
