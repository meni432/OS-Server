
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ofir Arnon
 */
public class Cthread implements Runnable {
    private final static int SIZE=100;
    private HashMap<Integer,Database.YandZ> toUpdate;
    private  ReentrantLock lock;
    SortedSet<XYZ> cash ;
    
    public Cthread()
    {
        toUpdate = new HashMap<>();
        cash=new TreeSet();
    }
    public void execute(int x, Database.YandZ yANDz ) {
        lock.lock();
        try {//If the map previously contained a mapping for the key, the old value is replaced
            this.toUpdate.put(x,yANDz);
        } finally {
            lock.unlock();
        }

    }
    public void updateCash()
    {
       lock.lock();
         try {
                Set<Map.Entry<Integer, Database.YandZ>> set = toUpdate.entrySet();
                Iterator<Map.Entry<Integer, Database.YandZ>> itr = set.iterator();
                while (itr.hasNext()) {
                    Map.Entry<Integer, Database.YandZ> elm = (Map.Entry<Integer, Database.YandZ>) itr.next();
                    XYZ element=new XYZ(elm.getKey(),elm.getValue()); 
                    if(cash.remove(element)){// if x already in the cash, then update
                        cash.add(element);
                    }
                    else if(cash.size()<SIZE)// not  in the cash and the cash isnt full
                    {
                        cash.add(element);// adding or updating z                   
                    }
                    else{// not in the cash and the cash full
                        XYZ lowestZ=cash.first();
                        if(lowestZ.compareTo(element)==0)
                        {
                            cash.remove(lowestZ);
                            cash.add(element);
                        }
                    }
                }
            
        } finally {
            lock.unlock();
        }
    }
    @Override
    public void run() {
        
        
    }
    
    
    
    
    
    
    static class XYZ implements Comparable<XYZ>{
        int x;
        Database.YandZ yANDz;
        public XYZ(int x,Database.YandZ yANDz)
        {
            this.x=x;
            this.yANDz =yANDz;
            
        }

       

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final XYZ other = (XYZ) obj;
            if (this.x != other.x) {
                return false;
            }
            return true;
        }
        
        @Override
        public int compareTo(XYZ other) {
            if (this.yANDz.z>other.yANDz.z)
                return 1;
            else if(this.yANDz.z==yANDz.z)
                return 0;
            else
                return -1;
        }
    }
    
    
    
    
    
    
}
