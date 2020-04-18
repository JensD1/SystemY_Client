package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPListener extends Thread {
    @Override
    public void run() {
        System.out.println("Initializing TCP listener..." );
        try {
            //Initialize socket
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Server Started ....");
            while (true){
                Socket clientSocket = serverSocket.accept();
                TCPThreadHandler tcpThreadHandler = new TCPThreadHandler(clientSocket);
                tcpThreadHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}