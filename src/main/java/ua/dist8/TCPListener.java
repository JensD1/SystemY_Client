package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPListener extends Thread {
    private static final Logger logger = LogManager.getLogger();
    @Override
    /***
     * Constantly listens to TCP requests.
     * When there is an incoming request, it generates a new thread to handle it.
     */
    public void run() {
        logger.info("Initializing TCP listener..." );
        try {
            //Initialize socket
            ServerSocket serverSocket = new ServerSocket(5000);
            logger.info("Listening to port 5000....");
            while (true){
                Socket clientSocket = serverSocket.accept();

                TCPThreadHandler thread = new TCPThreadHandler(clientSocket);
                logger.info("TCP Packet received! Creating new thread(ID= "+thread.getId()+") to process the request.");
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
