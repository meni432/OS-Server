package ServerPack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author ofir Arnon
 */
public class InOutStreams {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    
    private static int ordinalNumber = 1;

    public static final int NOT_AVIVABLE = -2;

    InOutStreams(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);  //open a PrintWriter on the socket
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));  //open a BufferedReader on the socket
        
        out.println(ordinalNumber++);
    }

    public BufferedReader getOis() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getOos() {
        return out;
    }

    public int getInteger() throws IOException {
        if (!socket.isInputShutdown()) {
            int read = Integer.parseInt(in.readLine());
            return read;
        }
        return NOT_AVIVABLE;
    }

    public void writeInteger(int num) throws IOException {
        if (!socket.isOutputShutdown()) {
            out.println(num);
        }
    }

}
