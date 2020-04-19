package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;

public class UDPListener extends Thread {
    @Override
    /**
     * Constantly listens to UDP requests.
     * When there is an incoming request, it generates a new thread to handle it.
     */
    public void run() {
        try{
            // UDP parameters
            System.out.println("Initializing UDP listener..." );
            MulticastSocket ms = new MulticastSocket(6012);
            InetAddress MCgroup = InetAddress.getByName("224.0.0.200");
            ms.joinGroup(MCgroup); // todo
            System.out.println("Listening on Multicast address 224.0.0.200");
            while(true){
                byte[] buf = new byte[1000];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                ms.receive(datagramPacket);
                System.out.println("Packet received! Creating new thread to process the request.");
                UDPThreadHandler thread = new UDPThreadHandler(datagramPacket); //send  the request to a separate thread
                thread.start();
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }
}
