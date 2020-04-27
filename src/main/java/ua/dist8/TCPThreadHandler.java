package ua.dist8;


import org.json.JSONObject;

import java.io.InputStream;
import java.net.Socket;

public class TCPThreadHandler extends Thread {

    private Socket clientSocket;
    TCPThreadHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    /***
     * Checks if packet concerns shutdown.
     * If so, it calls the method to remove this node from the hashmap.
     */
    public void run() {
        try {
            InputStream clientInput = clientSocket.getInputStream();
            byte[] contents = new byte[10000]; // todo pas mogelijks aan
            if( clientInput.read(contents) != -1) { // the message is not empty.
                String message = new String(contents);
                JSONObject json = new JSONObject(message);

                switch (json.getString("typeOfMsg")) {
                    case "shutdown": {
                        NodeClient nodeClient = NodeClient.getInstance();
                        //nodeClient.shutdown();
                        break;
                    }
                    case "fileRequest":
                        //todo
                        break;
                    case "multicastReply": {
                        NodeClient nodeClient = NodeClient.getInstance();
                        if (json.getString("typeOfNode").equals("NS")) {
                            nodeClient.receiveMulticastReplyNS(json, clientSocket.getInetAddress());
                        } else if (json.getString("typeOfNode").equals("CL")) {
                            nodeClient.receiveMulticastReplyNode(json);
                        }
                        break;
                    }
                }
            }
            clientInput.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
