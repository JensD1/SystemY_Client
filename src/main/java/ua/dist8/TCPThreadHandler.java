package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class TCPThreadHandler extends Thread {

    private Socket clientSocket;
    TCPThreadHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream clientInput = clientSocket.getInputStream();
            byte[] contents = new byte[10000]; // pas dit nog aan !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if( clientInput.read(contents) != -1) { // the message is not empty.
                String message = new String(contents);
                JSONObject json = new JSONObject(message);

                switch (json.getString("typeOfMsg")) {
                    case "shutdown": {
                        NodeClient nodeClient = new NodeClient();
                        nodeClient.shutdown();
                        break;
                    }
                    case "fileRequest":
                        //todo
                        break;
                    case "multicastReply": {
                        NodeClient nodeClient = new NodeClient();
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
