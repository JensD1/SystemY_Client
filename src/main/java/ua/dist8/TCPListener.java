package ua.dist8;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPListener extends Thread {
    @Override
    public void run() {
        System.out.println("Initializing TCP listener..." );
        try {
            //Initialize socket
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Listening to port 5000....");
            while (true){
                Socket clientSocket = serverSocket.accept();

                TCPThreadHandler thread = new TCPThreadHandler(clientSocket);
                System.out.println("TCP Packet received! Creating new thread(ID= "+thread.getId()+") to process the request.");
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
