/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerPack;

/**
 *
 * @author ofir Arnon
 */
public class CashWriteThread implements Runnable{

    @Override
    
    public void run() {
        Thread.currentThread().setName("CashWriteThread");
        while(true){
            CashManager.updateCash();
            System.err.println("------------update cash completed---------------");
        }
    }
    
}
