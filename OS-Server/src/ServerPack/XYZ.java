/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerPack;

/**
 *
 * @author Meni Samet
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
        public void setZ(int z) {
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
