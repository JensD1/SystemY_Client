package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;

public class UDPListener extends Thread {
    private static final Logger logger = LogManager.getLogger();
    private volatile boolean isRunning = true;
    MulticastSocket ms;
    @Override
    /**
     * Constantly listens to UDP requests.
     * When there is an incoming request, it generates a new thread to handle it.
     */
    public void run() {
        try{
            // UDP parameters
            logger.info("Initializing UDP listener..." );
            ms = new MulticastSocket(6012);
            InetAddress MCgroup = InetAddress.getByName("224.0.0.200");
            ms.joinGroup(MCgroup); // todo
            logger.debug("Listening on Multicast address 224.0.0.200");
            while(isRunning){
                byte[] buf = new byte[1000];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                ms.receive(datagramPacket);

                UDPThreadHandler thread = new UDPThreadHandler(datagramPacket); //send  the request to a separate thread
                logger.info("UDP Packet received! Creating new thread(ID= "+thread.getId()+") to process the request.");
                thread.start();
            }
            logger.debug("The UDPListener thread has ended.");
        }catch(Exception e){
            logger.error(e);
        }
    }

    public void stopRunning(){
        try{
            ms.close();
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
