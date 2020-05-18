package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPThreadHandler extends Thread {

    private Socket clientSocket;
    private static final Logger logger = LogManager.getLogger();

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
                        logger.debug("Another node is exiting the network.");
                        nodeClient.receivedShutdown(json);
                        break;
                    }
                    case "fileRequest": {
                        NodeClient nodeClient = NodeClient.getInstance();
                        //nodeClient.fileRequest(String fileName); // todo finish this method.
                        break;
                    }
                    case "replication": {
                        logger.info("Received a replication message.");
                        NodeClient nodeClient = NodeClient.getInstance();
                        OutputStream clientOutput = clientSocket.getOutputStream();
                        nodeClient.receiveFile(clientInput, json, clientOutput, json.getString("typeOfMsg"));
                        clientOutput.close();
                        break;
                    }
                    case "log": {
                        logger.info("Received a log message.");
                        NodeClient nodeClient = NodeClient.getInstance();
                        OutputStream clientOutput = clientSocket.getOutputStream();
                        nodeClient.receiveFile(clientInput, json, clientOutput, json.getString("typeOfMsg"));
                        clientOutput.close();
                        break;
                    }
                    case "multicastReply": {
                        NodeClient nodeClient = NodeClient.getInstance();
                        if (json.getString("typeOfNode").equals("NS")) {
                            nodeClient.receiveMulticastReplyNS(json, clientSocket.getInetAddress());
                        } else if (json.getString("typeOfNode").equals("CL")) {
                            nodeClient.receiveMulticastReplyNode(json);
                        }
                        break;
                    }

                    case "replicationShutdown" : {
                        NodeClient.getInstance().removeReplicatedFile(json.getString("fileName"), json.getString("typeOfDest")
                                , json.getString("typeOfSource"), clientSocket.getInetAddress());
                    }
                    default:
                        logger.error("Received a wrong typeOfMessage!");
                }
            }

            clientInput.close();
            clientSocket.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}