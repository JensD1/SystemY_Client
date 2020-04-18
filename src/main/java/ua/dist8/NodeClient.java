package ua.dist8;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.net.*;

public class NodeClient {
    private Hashing hashing;
    private String nodeName;
    private Integer nextID;
    private Integer previousID;


    /**
     * Constructor for the NodeClient class
     */
    NodeClient(){
        hashing = new Hashing();
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        previousID = -1;
        nextID = -1;
    }

    /**
     * This function wil run when a node receives a multicast message from a new node that wants to join the network
     * @param receivedNodeName is the name of the node that wants to join
     * @param nodeIP is the IP-address of the node that wants to join
     */
    public void receivedMulticast(String receivedNodeName, InetAddress nodeIP) throws IOException {
        Integer hash = hashing.createHash(receivedNodeName);

        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Integer currentID = hashing.createHash(nodeName);

        if(currentID<hash && hash<nextID){
            nextID = hash;
            sendUnicastMessage(nodeIP, currentID, nextID);
        }
        if(previousID< hash && hash<currentID){
            previousID = hash;
            sendUnicastMessage(nodeIP, currentID, previousID);
        }
    }

    public void sendUnicastMessage(InetAddress toSend, Integer currentID, Integer newNodeID) throws IOException {
        Socket socket = new Socket(toSend, 5000);
        OutputStream outputStream = socket.getOutputStream();
        JSONObject json = new JSONObject();
        json.put("typeOfNode", "CL");
        json.put("currentID", currentID);
        json.put("newNodeID", newNodeID);
        outputStream.write(json.toString().getBytes());
        outputStream.flush();
        outputStream.close();
        socket.close();
    }
  
    public void receiveUnicastMessage() throws Exception{
        Integer receivedNumberOfMessages = 0;
        Boolean leaveWhile = Boolean.FALSE;

        //Initialize socket
        System.out.println("Waiting for response of a multicast bootstrap.");
        ServerSocket serverSocket = new ServerSocket(5000);
        do{// make threaded!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            Socket clientSocket = serverSocket.accept();
            InputStream clientInput = clientSocket.getInputStream();
            byte[] contents = new byte[10000]; // pas dit nog aan !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            if( clientInput.read(contents) != -1){ // the message is not empty.
                String message = new String(contents);
                JSONObject json = new JSONObject(message);
                receivedNumberOfMessages++;
                if(json.getString("typeOfNode").equals("NS")){ // Als het bericht komt van de NamingServer
                    if(json.getInt("amountOfNodes") <= 0) // The JSON object of a NamingServer needs to contain this field.
                        leaveWhile = Boolean.TRUE; // er is maar 1 bericht dat ontvangen moest worden en dit is ontvangen
                        nextID = hashing.createHash(nodeName);
                        previousID = hashing.createHash(nodeName);
                }
                if(json.getString("typeOfNode").equals("CL")){
                    Integer currentID = json.getInt("currentID"); // The other ones ID
                    Integer newNodeID = json.getInt("newNodeID"); // Your own ID
                    if(currentID > newNodeID){
                        nextID = currentID;
                    }
                    else{
                        previousID = currentID;
                    }
                }
            }
            clientInput.close();
            clientSocket.close();
        }while(!leaveWhile && receivedNumberOfMessages<3);
    }

    public void multicast() throws IOException {
        InetAddress MCgroup = InetAddress.getByName("224.0.0.200");
        JSONObject obj = new JSONObject();
        obj.put("name", nodeName);
        obj.put("ip", InetAddress.getLocalHost());
        MulticastSocket ms = new MulticastSocket(6012);
        ms.joinGroup(MCgroup);
        byte[] contents = obj.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(contents,contents.length, MCgroup, 6012);
        ms.send(packet);
        //ms.leaveGroup(ms.getLocalSocketAddress(), NetworkInterface.getByInetAddress(group));

    }

    public void receiveMC () throws IOException {
        MulticastSocket ms = new MulticastSocket(6012);
        InetAddress MCgroup = InetAddress.getByName("224.0.0.200");
        ms.joinGroup(MCgroup);
        while(true) { // Make thread!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            ms.receive(recv);
            if (recv.getLength() > 0) {
                String s = new String(String.valueOf(recv));
                JSONObject jsonObject = new JSONObject(s);
                receivedMulticast(jsonObject.getString("name"), (InetAddress)jsonObject.get("ip"));
            }
        }
    }
}
