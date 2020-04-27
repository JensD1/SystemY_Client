package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.net.*;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class NodeClient {
    private String nodeName;
    private Integer nextID;
    private Integer previousID;
    private InetAddress nsIP;
    private Semaphore sem = new Semaphore(1);

    private static NodeClient nodeClient = new NodeClient();

    /**
     * Constructor for the NodeClient class
     */
    private NodeClient(){
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        previousID = Hashing.createHash(nodeName);
        nextID = previousID;
    }

    public static NodeClient getInstance(){
        return nodeClient;
    }

    /**
     * This function wil run when a node receives a multicast message from a new node that wants to join the network.
     * Here we will see if the new node is a neighbour of the existing (current) node. When yes, we will adjust the
     * next-/previous ID number of the current node and send a message back to the node to let it know we are
     * neighbours.
     * @param receivedNodeName is the name of the node that wants to join
     * @param nodeIP is the IP-address of the node that wants to join
     */
    public void multicastHandler(String receivedNodeName, InetAddress nodeIP) throws IOException, JSONException, InterruptedException {
        System.out.println("Received a multicast from another Node on the network, processing message ...");
        Integer hash = Hashing.createHash(receivedNodeName);
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Integer currentID = Hashing.createHash(nodeName);
        System.out.println("my own hash is: " + currentID);
        System.out.println("my Previous is: " + previousID);
        System.out.println("my next is: " + nextID);
        System.out.println("The new one is: " + hash);

        JSONObject json = new JSONObject();
        json.put("typeOfMsg","multicastReply");
        if(currentID<hash && hash<nextID){
            System.out.println("In the first if of multicastReply.");
            nextID = hash;
            if (previousID.equals(currentID)){
                previousID = hash;
            }
            json.put("typeOfNode", "CL");
            json.put("setAs", "next");
            json.put("currentID", currentID);
            json.put("newNodeID", nextID);
            sendUnicastMessage(nodeIP, json);
            System.out.println("NextID changed to: " + nextID + " Sending unicast message..\nHe is not an end node.");
        }
        if(previousID< hash && hash<currentID){
            System.out.println("In the second if of multicastReply.");
            previousID = hash;
            if (nextID.equals(currentID)){
                nextID = hash;
            }
            json.put("typeOfNode", "CL");
            json.put("setAs", "previous");
            json.put("currentID", currentID);
            json.put("newNodeID", previousID);
            sendUnicastMessage(nodeIP, json);
            System.out.println("PreviousID changed to: " + previousID + " Sending unicast message..\nHe is not an end node.");
        }
        // here we will look if the currentID node is the node with the highest or lowes ID number
        if(currentID>=nextID ){ // there is only one node, or multiple nodes but you have the highest ID number because next is lower.
            if (currentID < hash) {
                // the new node has a higher ID
                System.out.println("In the third if part 1 of multicastReply.");
                nextID = hash;
                if (previousID.equals(currentID)) {
                    previousID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "next");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
                System.out.println("NextID changed to: " + nextID + " Sending unicast message..\nHe is an end node.");
            }
            if(currentID > hash && hash < nextID){
                // the new node has a higher ID
                System.out.println("In the third if part 2 of multicastReply.");
                nextID = hash;
                if (previousID.equals(currentID)) {
                    previousID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "next");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
                System.out.println("NextID changed to: " + nextID + " Sending unicast message..\nHe is an end node.");
            }
        }
        if(currentID<=previousID){ // you have the lowest nodeID on the network.
            if(currentID > hash) {
                // The new node has a lower ID.
                System.out.println("In the fourth if part 1 of multicastReply.");
                previousID = hash;
                if (nextID.equals(currentID)) {
                    nextID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "previous");
                json.put("currentID", currentID);
                json.put("newNodeID", previousID);
                System.out.println("PreviousID changed to: " + previousID + " Sending unicast message..\nHe is an end node.");
                sendUnicastMessage(nodeIP, json);
            }
            if (currentID < hash && previousID < hash){
                // The new node has a lower ID.
                System.out.println("In the fourth if part 2 of multicastReply.");
                previousID = hash;
                if (nextID.equals(currentID)) {
                    nextID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "previous");
                json.put("currentID", currentID);
                json.put("newNodeID", previousID);
                System.out.println("PreviousID changed to: " + previousID + " Sending unicast message..\nHe is an end node.");
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
     * @param json JSON object that will be transmitted.
     * @throws IOException the exception to handle the outputStream exceptions
     * @throws JSONException the exception to handle the JSON exceptions
     */
    public void sendUnicastMessage(InetAddress toSend,JSONObject json) throws IOException, JSONException, InterruptedException {
        sem.acquire();
        Socket socket = new Socket(toSend, 5000);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(json.toString().getBytes());
        outputStream.flush();
        outputStream.close();
        socket.close();
        sem.release();
    }

    /**
     *  This function will run after this node has sent a multicast to receive the responses of other nodes and the naming server in the network
     * @param json JSON object that will be transmitted, that contains:
     *         "isEndNode"
     *         "currentID"
     *         "newNodeID"
     * @throws Exception
     * @throws JSONException
     */
    public void receiveMulticastReplyNode(JSONObject json) throws JSONException{
        System.out.println("Received a reply of our discovery multicast message from another node.");
        Integer currentID = json.getInt("currentID"); // The other ones ID
        System.out.println("Received a message from ID: " + currentID);
        Integer newNodeID = json.getInt("newNodeID"); // Your own ID
        System.out.println("I have ID: " + newNodeID);

        if(previousID.equals(newNodeID) && nextID.equals(newNodeID)){
            System.out.println("third if of receiveMulticastReplyNode");
            previousID = currentID;
            nextID = currentID;
        }
        else{
            String setAs = json.getString("setAs");
            if(setAs.equals("next")){
                previousID = currentID;
            }
            if(setAs.equals("previous")){
                nextID = currentID;
            }
        }
    }

    /***
     * This function will run after this node has sent a multicast to receive the response of the naming server in the network
     * @param json JSON object that will be transmitted, that contains:
     *             "amountOfNodes"
     * @param nsIP is the IP-address of the naming server
     * @throws JSONException
     * @throws IOException
     * @throws InterruptedException
     */
    public void receiveMulticastReplyNS(JSONObject json, InetAddress nsIP) throws JSONException, IOException, InterruptedException {
        System.out.println("Received a reply of our discovery multicast message from the NamingServer.");
        int amountOfNodes = (json.getInt("amountOfNodes"));
        if(amountOfNodes >0){
            System.out.println("Succesfully connected to " + nsIP.getHostName() + "\nThe amount of other nodes in the network = " + amountOfNodes);
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
        }
        else if(amountOfNodes == 0){
            System.out.println("I am the only node in the network, setting next/previous ID to myself");
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            nextID = Hashing.createHash(nodeName);
            previousID = Hashing.createHash(nodeName);
        }
        else if(amountOfNodes == -1){
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            System.out.println("I am already in the network!\nFetching my next and previous neighbour from NS! ");
            //todo getNeighbours
        }
        else{
            System.out.println("Something went wrong, please try again...");
        }

    }

    /***
     * This function will be called when one of the node's neighbors is preparing to shutdown itself
     * @param json is a JSONObject containing the to be updated next/previous nodeID
     * @throws JSONException
     */
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
        MulticastSocket ms = new MulticastSocket(6012);
        ms.getLocalSocketAddress();
        ms.joinGroup(MCgroup);
        byte[] contents = obj.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(contents,contents.length, MCgroup, 6012);
        ms.send(packet);
        System.out.println("Multicast bootstrap message is sent.");
    }

    /***
     * This function will run when this node wants to shutdown itself, sending three messages in total to:
     * Naming server
     * Previous neighbor
     * Next Neighbor
     * @throws IOException
     * @throws JSONException
     */
    public void shutdown () throws IOException, JSONException, InterruptedException {
        //That is the part of the NS:
        System.out.println("Shutting down client...\nSending shutdown message to server and neighbours...");
        JSONObject json = new JSONObject();
        Integer h = Hashing.createHash(nodeName);
        json.put("typeOfNode", "CL");
        json.put("typeOfMsg","shutdown");
        json.put("ID",h);
        sendUnicastMessage(nsIP,json);
        System.out.println("Message sent to NamingServer...");
        //This part is for the neighboring nodes:
        String name = nsIP.getHostAddress();
        JSONObject json2 = new JSONObject();
        json2.put("typeOfMsg","shutdown");
        json2.put("updateID",nextID);
        System.out.println("Requesting neighbours from NamingServer...");
        URL url = new URL ("http://" +name+ ":8080/neighbourRequest?nodeID="+h);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        System.out.println("Message received from NamingServer!");
        JSONObject j = new JSONObject(content.toString());
        System.out.println("Sending Unicast message to neighbours..");
        InetAddress previousNeighbor = (InetAddress) j.get("previousNode");
        sendUnicastMessage(previousNeighbor,json2);
        json2.put("updateID",previousID);
        InetAddress nextNeighbor = (InetAddress) j.get("nextNode");
        sendUnicastMessage(nextNeighbor,json2);
        System.out.println("Succesfuly disconnected from NamingServer!");
    }

    /**
     * REST request to get InetAddress of file location.
     * @param filename
     * @return
     * @throws IOException
     */
    public InetAddress fileRequest(String filename) throws IOException {
        if (nsIP == null){
            System.out.println("Not connected to any NameServer, Please use !connect before requesting a file");
            return null;
        }
        String hostName = nsIP.getHostAddress();
        String url ="http://"+hostName+":8080/fileRequest?filename=" + filename;
        System.out.println("Trying to connect with "+url);
        //String url ="http://host2/fileRequest?filename=" + filename;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("Connecting to " + url +"Response code = "+ responseCode);
        if(responseCode == 200){ //connection successful // Niet zeker wat ik hier moet zetten.
            String response = "";
            Scanner scanner = new Scanner(connection.getInputStream());
            while(scanner.hasNextLine()){
                response += scanner.nextLine();
                response += "\n";
            }
            scanner.close();
            // returns a string
            JSONObject jsonResponse = new JSONObject(response);
            String ip = jsonResponse.getString("inetAddress");
            System.out.println("Hostname of file is: "+ip);
            return InetAddress.getByName(ip);
        }
        System.out.println("Request failed!");

        // an error happened
        return null;
    }


    public void getNeighbours() throws IOException, InterruptedException {
        Integer h = Hashing.createHash(nodeName);
        String name = nsIP.getHostAddress();
        JSONObject json2 = new JSONObject();
        json2.put("typeOfMsg","shutdown");
        json2.put("updateID",nextID);
        System.out.println("Requesting neighbours from NamingServer...");
        URL url = new URL ("http://" +name+ ":8080/neighbourRequest?nodeHash="+h);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        System.out.println("Message received from NamingServer!");
        JSONObject j = new JSONObject(content.toString());
        System.out.println("Sending Unicast message to neighbours..");
        InetAddress previousNeighbor = (InetAddress) j.get("previousNode");
        sendUnicastMessage(previousNeighbor,json2);
        json2.put("updateID",previousID);
        InetAddress nextNeighbor = (InetAddress) j.get("nextNode");
        System.out.println("Previous NodeID is: "+previousID+"\nNext NodeID is: "+nextID);
    }

    /**
     * Print the neighbours of this node.
     */

    public void printNeighbours(){
        Integer myHash = Hashing.createHash(nodeName);
        System.out.println("Hashing my own nodeName: "+nodeName+"\nMy own hash is: "+myHash+"\nPrevious NodeID is: "+previousID+"\n Next NodeID is: "+nextID);
    }
}