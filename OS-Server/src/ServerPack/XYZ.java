package ServerPack;

/**
 * XYZ Class,
 * z value synchronize with read write lock
 */
class XYZ {

        public static final int LockError = -1;
        private ReadWriteLock lock = new ReadWriteLock();

        private int x;
        private int y;
        private int z;

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

        /**
         * get z synchronize function
         * with read lock (more then one threads can read z)
         * @return z value
         */
        public int getZ() {
            try {
                lock.lockRead();
                return z;
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            finally {
                lock.unlockRead();
            }
            
            return LockError;
        }
        


        /**
         * not for use currently!!!!!
         * @param z 
         */
        private void setZ(int z) {
            try {
                lock.lockWrite();
                this.z = z;
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            finally {
                lock.unlockWrite();
            }
        }
        
        /**
         * inc z synchronize function
         * with write lock
         */
        public void incZ() {
            try {
                lock.lockWrite();
                this.z++;
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            finally {
                lock.unlockWrite();
            }
        }
        
       

    }
