package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class TCPListener extends Thread {
    private static final Logger logger = LogManager.getLogger();
    private volatile boolean isRunning = false;
    ServerSocket  serverSocket;
    @Override
    /***
     * Constantly listens to TCP requests.
     * When there is an incoming request, it generates a new thread to handle it.
     */
    public void run() {
        isRunning = true;
        logger.info("Initializing TCP listener..." );
        try {
            //Initialize socket
            serverSocket = new ServerSocket(5000);
            logger.info("Listening to port 5000....");
            while (isRunning){
                Socket clientSocket = serverSocket.accept();

                TCPThreadHandler thread = new TCPThreadHandler(clientSocket);
                logger.info("TCP Packet received! Creating new thread(ID= "+thread.getId()+") to process the request.");
                thread.start();
            }
            logger.debug("The TCPListener thread has ended.");
        } catch (SocketException e){
            logger.trace(e);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void stopRunning(){
        try{
            serverSocket.close();
            isRunning = false;
        }
        catch (Exception e){
            logger.error(e);
        }
    }

    public boolean isRunning(){
        return isRunning;
    }
}
