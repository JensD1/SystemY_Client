package ua.dist8;

import org.json.JSONException;

import java.io.IOException;

public class NodeApplication {
    public static void main(String[] args) throws IOException, JSONException {
        NodeClient nodeClient = new NodeClient();
        nodeClient.multicast();
        TCPListener tcpListener = new TCPListener();
        UDPListener udpListener = new UDPListener();
        tcpListener.start();
        udpListener.start();
    }
}
