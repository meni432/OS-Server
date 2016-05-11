package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author ofir Arnon
 */
public class InOutStreams {
   private final ObjectInputStream ois;
   private final ObjectOutputStream oos;
   private final Socket socket;
    InOutStreams(Socket socket) throws IOException
    {
        this.ois=new ObjectInputStream(socket.getInputStream());
        this.oos=new ObjectOutputStream(socket.getOutputStream());
        this.socket=socket;              
    }

    public ObjectInputStream getOis() {
        return ois;
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }
    
}
