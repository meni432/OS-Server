package ServerPack;

/**
 * XYZ Class, z value synchronize with read write lock
 */
class XYZ implements Comparable<XYZ>{

    public static final int LockError = -1;
    private final ReadWriteLock lock = new ReadWriteLock();
    private boolean overWriteZ;

    private int x;
    private int y;
    private int z;

    public XYZ() { }

    
    public XYZ(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

    }

    @Override
    public String toString() {
        return "XYZ{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
            return z;
    }

    /**
     * inc z synchronize function with write lock
     */
    public void incZ() {
        try {
            lock.lockWrite();
            this.z++;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            lock.unlockWrite();
        }
    }
    
    public void zeroZ(){
        try {
            lock.lockWrite();
            this.z = 0;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            lock.unlockWrite();
        }
    }
    public void setOverWriteZ(boolean overWriteZ) {
        this.overWriteZ = overWriteZ;
    }
        public boolean isOverWriteZ() {
        return overWriteZ;
    }

    @Override
    public int hashCode() {
        return new Integer(x).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
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
    public int compareTo(XYZ o) {
        return Integer.compare(this.getX(), o.getX());
    }
        
        

}
