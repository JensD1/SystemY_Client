package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class UDPThreadHandler extends Thread{
    private DatagramPacket datagramPacket;

    UDPThreadHandler(DatagramPacket datagramPacket){
        this.datagramPacket = datagramPacket;
    }

    /**
     * In this method we will let the node scan for incoming UDP messages on the multicast address 224.0.0.200.
     * When there is such an incoming message, there is a node that wants to join the network an we will let
     * the receivedMulticast method handle this.
     * @throws IOException
     * @throws JSONException
     */
    @Override
    public void run() {
        String dataString = new String(this.datagramPacket.getData());
        try {
            JSONObject json = new JSONObject(dataString);
            if(json.getString("typeOfMsg").equals("Discovery")) {
                NodeClient nodeClient = new NodeClient();
                nodeClient.multicastHandler(json.getString("name"), datagramPacket.getAddress());
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}
