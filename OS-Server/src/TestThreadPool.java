
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
class Task implements Runnable {

    String name;

    public Task(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        System.out.println("start run: " + name);
//        try {
//           Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("end run: " + name);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}


public class TestThreadPool {

    static int name = 0;
    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(5);

        for (int i = 0; i < 1000; i++){
            try {
                pool.execute(new Task("task "+(++name)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        try {
           
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }

}
