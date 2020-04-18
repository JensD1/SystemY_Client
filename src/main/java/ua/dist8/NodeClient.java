package ua.dist8;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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
    private InetAddress nsIP;

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
     * This function wil run when a node receives a multicast message from a new node that wants to join the network.
     * Here we will see if the new node is a neighbour of the existing (current) node. When yes, we will adjust the
     * next-/previous ID number of the current node and send a message back to the node to let it know we are
     * neighbours.
     * @param receivedNodeName is the name of the node that wants to join
     * @param nodeIP is the IP-address of the node that wants to join
     */
    public void multicastHandler(String receivedNodeName, InetAddress nodeIP) throws IOException, JSONException {
        Integer hash = hashing.createHash(receivedNodeName);

        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Integer currentID = hashing.createHash(nodeName);
        JSONObject json = new JSONObject();
        json.put("typeOfMsg","multicastReply");
        if(currentID<hash && hash<nextID){
            nextID = hash;
            json.put("typeOfNode", "CL");
            json.put("currentID", currentID);
            json.put("newNodeID", nextID);
            sendUnicastMessage(nodeIP, json);
        }
        if(previousID< hash && hash<currentID){
            previousID = hash;
            json.put("typeOfNode", "CL");
            json.put("currentID", currentID);
            json.put("newNodeID", previousID);
            sendUnicastMessage(nodeIP, json);
        }
        // here we will look if the currentID node is the node with the highest or lowes ID number
        if(currentID>=nextID){ // there is only one node, or multiple nodes but you have the highest ID number because next is lower.
            if(currentID < hash){ // the new node has a higher ID
                nextID = hash;
                json.put("typeOfNode", "CL");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
            }
        }
        if(currentID<=previousID){ // you have the lowest nodeID on the network.
            if(currentID > hash){ // The new node has a lower ID.
                previousID = hash;
                json.put("typeOfNode", "CL");
                json.put("currentID", currentID);
                json.put("newNodeID", previousID);
                sendUnicastMessage(nodeIP, json);
            }
        }
    }

    /**
     * This method will send a unicast message to a given InetAddress as a respons to a multicast message.
     * The JSON object that will be send contains
     * 1) the type of node ( so the new node will know if this message comes from the NamingServer or another node ).
     * 2) the boolean isEndNode ( so the new node will know if it becomes a new endnode of the network ).
     * 3) the currentID ( this is the ID of the existing node in the network ).
     * 4) the newNodeID ( this is the ID of the node that wants to enter the network ).
     * @param toSend the InetAddress to were to send.
     * @param currentID The ID of the node that is already in the network.
     * @param newNodeID The ID of the node that wants to join the network.
     * @param isEndNode This boolean will be true if the currentID corresponds to an "endnode",
     *                  which is the node with the highest/lowest ID number in the network
     * @throws IOException the exception to handle the outputStream exceptions
     * @throws JSONException the exception to handle the JSON exceptions
     */
    public void sendUnicastMessage(InetAddress toSend,JSONObject json) throws IOException, JSONException {
        Socket socket = new Socket(toSend, 5000);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(json.toString().getBytes());
        outputStream.flush();
        outputStream.close();
        socket.close();

    }

    /**
     *  Once a multicast message is sent, the node that wants to access the network needs to listen to all responses.
     *  In the case that this is the first node, there will be only one message from the NamingServer, otherwise
     *  there will be 3 messages. 1 from the NamingServer and 2 from the other clients.
     *  In this method the node that wants to access the network will listen en see where in the network it belongs.
     * @throws Exception
     * @throws JSONException
     */
    public void receiveMulticastRelply() throws Exception, JSONException{ // todo
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
                    nsIP = clientSocket.getInetAddress(); //This will save the IP-address of the NS for later use
                    if(json.getInt("amountOfNodes") <= 0) // The JSON object of a NamingServer needs to contain this field.
                        leaveWhile = Boolean.TRUE; // er is maar 1 bericht dat ontvangen moest worden en dit is ontvangen
                        nextID = hashing.createHash(nodeName);
                        previousID = hashing.createHash(nodeName);
                }
                if(json.getString("typeOfNode").equals("CL")){
                    Boolean isEndNode = json.getBoolean("isEndNode");
                    Integer currentID = json.getInt("currentID"); // The other ones ID
                    Integer newNodeID = json.getInt("newNodeID"); // Your own ID
                    if(!isEndNode) {
                        if (currentID > newNodeID) {
                            nextID = currentID;
                        } else {
                            previousID = currentID;
                        }
                    }
                    else{
                        if (currentID > newNodeID) {
                            previousID = currentID;
                        } else {
                            nextID = currentID;
                        }
                    }
                }
            }
            clientInput.close();
            clientSocket.close();
        }while(!leaveWhile && receivedNumberOfMessages<3);
    }

    public void receivedShutdown(JSONObject json) throws JSONException{

        Integer updateID = json.getInt("updateID");
        if(updateID>nextID){
            nextID=updateID;
        }
        else
            previousID=updateID;
    }

    /**
     * This method will send a multicast message over UDP to everyone listening to the multicastAddress 224.0.0.200.
     * The purpose of this message is to make it possible for a node to access the current network and to know what
     * nodes are already on the network.
     * In this message, two variables will be transmitted by using a JSON object. The first variable will be the name
     * of the node that wants to join the network, the second variable will be the InetAddress of this node.
     * @throws IOException
     * @throws JSONException
     */
    public void multicast() throws IOException, JSONException {
        InetAddress MCgroup = InetAddress.getByName("224.0.0.200");
        JSONObject obj = new JSONObject();
        obj.put("typeOfMsg","Discovery");
        obj.put("name", nodeName);
        obj.put("ip", InetAddress.getLocalHost());
        MulticastSocket ms = new MulticastSocket(6012);
        ms.getLocalSocketAddress();
        ms.joinGroup(MCgroup);
        byte[] contents = obj.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(contents,contents.length, MCgroup, 6012);
        ms.send(packet);
        //ms.leaveGroup(ms.getLocalSocketAddress(), NetworkInterface.getByInetAddress(group));

    }

    public void shutdown () throws IOException, JSONException {
    //That is the part of the NS:
    JSONObject json = new JSONObject();
    Integer h = hashing.createHash(nodeName);
    json.put("typeOfNode", "CL");
    json.put("typeOfMsg","shutdown");
    json.put("ID",h);
    sendUnicastMessage(nsIP,json);

    //This part is for the neighboring nodes:
    String name = nsIP.getHostName();
    JSONObject json2 = new JSONObject();
    json2.put("typeOfMsg","shutdown");
    json2.put("updateID",nextID);
    URL url = new URL ("http://" +name+ ":8080/neighbourRequest?nodeID="+h);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer content = new StringBuffer();
    while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
    }
    in.close();
    con.disconnect();
    JSONObject j = new JSONObject(inputLine);
    InetAddress previousNeighbor = (InetAddress) j.get("previousIP");
    sendUnicastMessage(previousNeighbor,json2);
    json2.put("updateID",previousID);
    InetAddress nextNeighbor = (InetAddress) j.get("nextIP");
    sendUnicastMessage(nextNeighbor,json2);
    }
}
