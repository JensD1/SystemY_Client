package ua.dist8;

import java.net.*;

public class UDPListener extends Thread {
    @Override
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

                UDPThreadHandler thread = new UDPThreadHandler(datagramPacket); //send  the request to a separate thread
                System.out.println("UDP Packet received! Creating new thread(ID= "+thread.getId()+") to process the request.");
                thread.start();
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }
}
