package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class NodeClient {
    private String nodeName;
    private Integer nextID;
    private Integer previousID;
    private InetAddress nsIP;
    private Semaphore sem = new Semaphore(1);
    private static final Logger logger = LogManager.getLogger();
    private static NodeClient nodeClient = new NodeClient();
    private ConcurrentHashMap<String,String> localMap;


    /**
     * Constructor for the NodeClient class
     */
    private NodeClient(){
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.error(e);
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
        logger.debug("Received a multicast from another Node on the network, processing message ...");
        Integer hash = Hashing.createHash(receivedNodeName);
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.error(e);
        }

        Integer currentID = Hashing.createHash(nodeName);
        logger.info("my own hash is: " + currentID);
        logger.info("my Previous is: " + previousID);
        logger.info("my next is: " + nextID);
        logger.info("The new one is: " + hash);

        JSONObject json = new JSONObject();
        json.put("typeOfMsg","multicastReply");
        if(currentID<hash && hash<nextID){
            logger.debug("In the first if of multicastReply.");
            nextID = hash;
            if (previousID.equals(currentID)){
                previousID = hash;
            }
            json.put("typeOfNode", "CL");
            json.put("setAs", "next");
            json.put("currentID", currentID);
            json.put("newNodeID", nextID);
            sendUnicastMessage(nodeIP, json);
            logger.debug("NextID changed to: " + nextID + " Sending unicast message.. He is not an end node.");
        }
        if(previousID< hash && hash<currentID){
            logger.debug("In the second if of multicastReply.");
            previousID = hash;
            if (nextID.equals(currentID)){
                nextID = hash;
            }
            json.put("typeOfNode", "CL");
            json.put("setAs", "previous");
            json.put("currentID", currentID);
            json.put("newNodeID", previousID);
            sendUnicastMessage(nodeIP, json);
            logger.debug("PreviousID changed to: " + previousID + " Sending unicast message.. He is not an end node.");
        }
        // here we will look if the currentID node is the node with the highest or lowes ID number
        if(currentID>=nextID ){ // there is only one node, or multiple nodes but you have the highest ID number because next is lower.
            if (currentID < hash) {
                // the new node has a higher ID
                logger.debug("In the third if part 1 of multicastReply.");
                nextID = hash;
                if (previousID.equals(currentID)) {
                    previousID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "next");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
                logger.debug("NextID changed to: " + nextID + " Sending unicast message.. He is an end node.");
            }
            if(currentID > hash && hash < nextID){
                // the new node has a higher ID
                logger.debug("In the third if part 2 of multicastReply.");
                nextID = hash;
                if (previousID.equals(currentID)) {
                    previousID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "next");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
                logger.debug("NextID changed to: " + nextID + " Sending unicast message.. He is an end node.");
            }
        }
        if(currentID<=previousID){ // you have the lowest nodeID on the network.
            if(currentID > hash) {
                // The new node has a lower ID.
                logger.debug("In the fourth if part 1 of multicastReply.");
                previousID = hash;
                if (nextID.equals(currentID)) {
                    nextID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "previous");
                json.put("currentID", currentID);
                json.put("newNodeID", previousID);
                logger.debug("PreviousID changed to: " + previousID + " Sending unicast message.. He is an end node.");
                sendUnicastMessage(nodeIP, json);
            }
            if (currentID < hash && previousID < hash){
                // The new node has a lower ID.
                logger.debug("In the fourth if part 2 of multicastReply.");
                previousID = hash;
                if (nextID.equals(currentID)) {
                    nextID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "previous");
                json.put("currentID", currentID);
                json.put("newNodeID", previousID);
                logger.debug("PreviousID changed to: " + previousID + " Sending unicast message.. He is an end node.");
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
    public void sendUnicastMessage(InetAddress toSend,JSONObject json){
        try {
            sem.acquire();
            Socket socket = new Socket(toSend, 5000);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(json.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            socket.close();
            sem.release();
        } catch(Exception e){
            logger.error(e);
        }
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
        logger.info("Received a reply of our discovery multicast message from another node.");
        Integer currentID = json.getInt("currentID"); // The other ones ID
        logger.info("Received a message from ID: " + currentID);
        Integer newNodeID = json.getInt("newNodeID"); // Your own ID
        logger.info("I have ID: " + newNodeID);

        if(previousID.equals(newNodeID) && nextID.equals(newNodeID)){
            logger.debug("third if of receiveMulticastReplyNode");
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
        logger.info("Received a reply of our discovery multicast message from the NamingServer.");
        int amountOfNodes = (json.getInt("amountOfNodes"));
        if(amountOfNodes > 0){
            logger.debug("Succesfully connected to " + nsIP.getHostName() + ". The amount of other nodes in the network = " + amountOfNodes);
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
        }
        else if(amountOfNodes == 0){
            logger.debug("Succesfully connected to " + nsIP.getHostName() +". I am the only node in the network, setting next/previous ID to myself");
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            nextID = Hashing.createHash(nodeName);
            previousID = Hashing.createHash(nodeName);
        }
        else if(amountOfNodes == -1){
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            logger.debug("I am already in the network! Fetching my next and previous neighbour from NS! ");
            //todo getNeighbours
        }
        else{
            logger.error("Something went wrong, please try again...");
        }

    }

    /***
     * This function will be called when one of the node's neighbors is preparing to shutdown itself
     * @param json is a JSONObject containing the to be updated next/previous nodeID
     * @throws JSONException
     */
    public void receivedShutdown(JSONObject json) throws JSONException{
        String target = json.getString("target");
        Integer updateID = json.getInt("updateID");
        if(target.equals("next")){
            nextID = updateID;
            logger.info("NextID has changed to " + nextID);
        }
        else if(target.equals("previous")){
            previousID = updateID;
            logger.info("PreviousID has changed to " + previousID);
        }
        else{
            logger.error("Invalled target in receivedShutdown().");
        }
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
        logger.info("Multicast bootstrap message is sent.");
    }

    /***
     * This function will run when this node wants to shutdown itself, sending three messages in total to:
     * Naming server
     * Previous neighbor
     * Next Neighbor
     * @throws IOException
     * @throws JSONException
     */
    public void shutdown (){
        try{
            Integer hash = Hashing.createHash(nodeName);
            logger.debug("Starting shutdown procedure...");
            //This part is for the neighboring nodes:
            String name = nsIP.getHostAddress();
            logger.info("The hostaddress of the namingserver is " + name);
            JSONObject neighbourJSON = new JSONObject();
            neighbourJSON.put("typeOfMsg", "shutdown");
            neighbourJSON.put("target", "next");
            neighbourJSON.put("updateID", nextID);
            logger.debug("Requesting neighbours from NamingServer...");
            URL url = new URL("http://" + name + ":8080/neighbourRequest?nodeHash=" + hash);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            logger.debug("Connecting to " + url + "Response code = " + responseCode);
            if (responseCode == 200) { //connection successful, NS can only remove the mode if he finds the node
                String response = "";
                Scanner scanner = new Scanner(con.getInputStream());
                while (scanner.hasNextLine()) {
                    response += scanner.nextLine();
                    response += "\n";
                }
                scanner.close();
                con.disconnect();

                logger.debug("Neighbours received from NamingServer!");
                JSONObject responseJSON = new JSONObject(response);
                logger.debug("Sending Unicast message to neighbours..");
                String previousNeighbor = responseJSON.getString("previousNode");
                logger.debug("Previous host is " + previousNeighbor);
                sendUnicastMessage(InetAddress.getByName(previousNeighbor), neighbourJSON);
                neighbourJSON.put("updateID", previousID);
                neighbourJSON.put("target", "previous");
                String nextNeighbor = responseJSON.getString("nextNode");
                sendUnicastMessage(InetAddress.getByName(nextNeighbor), neighbourJSON);

                //Sending shutdown msg to NS
                logger.debug("Shutting down client... Asking the NamingServer to remove us from the network..");
                JSONObject shutdownJSON = new JSONObject();

                shutdownJSON.put("typeOfNode", "CL");
                shutdownJSON.put("typeOfMsg", "shutdown");
                shutdownJSON.put("ID", hash);
                logger.debug("Shutdown message for ID: " + hash);
                sendUnicastMessage(nsIP, shutdownJSON);
                //resetting neighbourIDs
                nextID = Hashing.createHash(nodeName);
                previousID = nextID;
                logger.info("Succesfuly disconnected from NamingServer!");
            }
        }
        catch (Exception e){
            logger.error(e);
        }
    }

    /**
     * REST request to get InetAddress of file location.
     * @param filename
     * @return
     * @throws IOException
     */
    public InetAddress fileRequest(String filename) throws IOException {
        if (nsIP == null){
            logger.warn("Not connected to any NameServer, Please use !connect before requesting a file");
            return null;
        }
        String hostName = nsIP.getHostAddress();
        String url ="http://"+hostName+":8080/fileRequest?filename=" + filename;
        logger.debug("Trying to connect with "+url);
        //String url ="http://host2/fileRequest?filename=" + filename;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        logger.debug("Connecting to " + url +"Response code = "+ responseCode);
        if(responseCode == 200){ //connection successful
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
            logger.debug("Hostname of file is: "+ip);
            return InetAddress.getByName(ip);
        }
        logger.error("Request failed!");

        // an error happened
        return null;
    }

    /**
     * Print the neighbours of this node.
     */

    public void printNeighbours(){
        Integer myHash = Hashing.createHash(nodeName);
        logger.debug("Hashing my own nodeName: "+nodeName+"\nMy own hash is: "+myHash+"\nPrevious NodeID is: "+previousID+"\n Next NodeID is: "+nextID);
    }

    public String getHostName(){
        return nodeName;
    }

    /**
     * Init the local list with owned files and set locks to open
     */
    public void initLocalList(){
        File folder = new File("/home/pi/localFiles/");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                localMap.put(fileName,"Open");

            }
        }
    }

    public ConcurrentHashMap<String, String> getLocalMap() {
        return localMap;
    }
}
