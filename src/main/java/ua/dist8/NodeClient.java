package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.net.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import java.util.concurrent.Semaphore;

public class NodeClient {
    private String nodeName;
    private Integer nextID;
    private Integer previousID;
    private InetAddress nsIP;
    private static Semaphore sendingSem = new Semaphore(1);
    private static Semaphore fileSem = new Semaphore(1);
    private static final Logger logger = LogManager.getLogger();
    private static NodeClient nodeClient = new NodeClient();
    private static Map<String, InetAddress> replicatedFilesMap;
    private static volatile InetAddress ownNodeAddress;
    private static ReplicationUpdateThread replicationUpdateThread;


    /**
     * Constructor for the NodeClient class
     */
    private NodeClient(){
        try {
            nodeName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.error(e);
        }
        replicatedFilesMap = new ConcurrentHashMap<>();
        previousID = Hashing.createHash(nodeName);
        nextID = previousID;
        ownNodeAddress = null;
    }

    public static NodeClient getInstance(){
        return nodeClient;
    }

    public InetAddress getNsIP(){
        return nsIP;
    }

    public static InetAddress getOwnNodeAddress(){
        return ownNodeAddress;
    }

    public Boolean nodeExists(Integer hash){
        try {
            if (nsIP == null) {
                logger.warn("Not connected to any NameServer, Please use !connect before requesting a file");
                return false;
            }
            String hostName = nsIP.getHostAddress();
            String url = "http://" + hostName + ":8080/nodeExists?nodeHash=" + hash;
            logger.debug("Trying to connect with " + url);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            logger.debug("Connecting to " + url + "Response code = " + responseCode);
            if (responseCode == 200) { //connection successful
                String response = "";
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNextLine()) {
                    response += scanner.nextLine();
                    response += "\n";
                }
                scanner.close();
                // returns a string
                JSONObject jsonResponse = new JSONObject(response);
                Boolean nodeExists = jsonResponse.getBoolean("nodeExists");
                logger.debug("The value of nodeExists is: " + nodeExists);
                return nodeExists;
            }
            logger.error("Request failed!");

        } catch(Exception e){
            logger.error(e);
        }

        // an error happened
        return false;
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
        logger.debug("Received a multicast from another Node (" + nodeIP.getHostName() + ") on the network, processing message ...");
        while (ownNodeAddress == null){}
        Integer hash = Hashing.createHash(receivedNodeName);
        if (nodeExists(hash) && !nodeIP.equals(ownNodeAddress)) {
            logger.info("This is a valid node.");
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
            json.put("typeOfMsg", "multicastReply");
            if (currentID < hash && hash < nextID) {
                logger.debug("In the first if of multicastReply.");
                nextID = hash;
                if (previousID.equals(currentID)) {
                    previousID = hash;
                }
                json.put("typeOfNode", "CL");
                json.put("setAs", "next");
                json.put("currentID", currentID);
                json.put("newNodeID", nextID);
                sendUnicastMessage(nodeIP, json);
                logger.debug("NextID changed to: " + nextID + " Sending unicast message.. He is not an end node.");
            }
            if (previousID < hash && hash < currentID) {
                logger.debug("In the second if of multicastReply.");
                previousID = hash;
                if (nextID.equals(currentID)) {
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
            if (currentID >= nextID) { // there is only one node, or multiple nodes but you have the highest ID number because next is lower.
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
                if (currentID > hash && hash < nextID) {
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
            if (currentID <= previousID) { // you have the lowest nodeID on the network.
                if (currentID > hash) {
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
                if (currentID < hash && previousID < hash) {
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
            if (nextID.equals(previousID)) {
                if (!nextID.equals(currentID)) {
                    logger.debug("STARTING REPLICATION FROM MULTICAST.");
                    replicationStart();
                }
            } else {
                logger.debug("STARTING CHECKIFOWNERCHANGED FROM MULTICAST.");
                checkIfOwnerChanged();
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
            sendingSem.acquire();
            Socket socket = new Socket(toSend, 5000);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(json.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            socket.close();
            sendingSem.release();
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
            logger.debug("Succesfully connected to " + nsIP.getHostName() + ". The number of other nodes in the network before I entered is " + amountOfNodes);
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            ownNodeAddress = nodeRequest(Hashing.createHash(InetAddress.getLocalHost().getHostName()));
            logger.debug("STARTING REPLICATION FROM RECEIVEMULTICASTREPLYNS.");
            nodeClient.replicationStart();
        }
        else if(amountOfNodes == 0){
            logger.debug("Succesfully connected to " + nsIP.getHostName() +". I am the only node in the network, setting next/previous ID to myself");
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            ownNodeAddress = nodeRequest(Hashing.createHash(InetAddress.getLocalHost().getHostName()));
            nextID = Hashing.createHash(nodeName);
            previousID = Hashing.createHash(nodeName);
        }
        else if(amountOfNodes == -1){
            this.nsIP = nsIP; //This will save the IP-address of the NS for later use
            ownNodeAddress = nodeRequest(Hashing.createHash(InetAddress.getLocalHost().getHostName()));
            logger.debug("I am already in the network! Fetching my next and previous neighbour from NS! ");
            //todo getNeighbours
        }
        else {
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

    public String getFileLocation(File file) throws IOException {
        String name = nsIP.getHostAddress();

        String filename = file.getName();
        logger.debug("Requesting file" + filename + " from NamingServer...");
        URL urlFile = new URL("http://" + name + ":8080/fileRequest?filename=" + filename);
        HttpURLConnection conFile = (HttpURLConnection) urlFile.openConnection();
        conFile.setRequestMethod("GET");
        int responseCodeFile = conFile.getResponseCode();
        logger.debug("Connecting to " + urlFile + "Response code = " + responseCodeFile);
        String responseFile = "";
        Scanner scannerFile = new Scanner(conFile.getInputStream());
        while (scannerFile.hasNextLine()) {
            responseFile += scannerFile.nextLine();
            responseFile += "\n";
        }
        scannerFile.close();
        conFile.disconnect();
        logger.debug("File location received from NamingServer!");
        JSONObject responseFileJSON = new JSONObject(responseFile);

        return responseFileJSON.getString("inetAddress");
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
            String name = nsIP.getHostAddress();
            Integer hash = Hashing.createHash(nodeName);
            logger.debug("Starting shutdown procedure...");

            //This part is for receiving neighboring nodes.
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
            if (responseCode == 200) { //connection successful, NS can only remove the node if he finds it
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

                // Send a message to the owners of each local file. At the owner, the removeReplicatedFiles method will be executed
                File folder = new File("/home/pi/localFiles/");
                File[] listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        String filename = file.getName();
                        String destAddress = getFileLocation(file);
                        JSONObject json = new JSONObject();
                        json.put("typeOfMsg", "replicationShutdown");
                        json.put("typeOfSource","local");
                        json.put("typeOfDest","owned");
                        json.put("typeOfNode", "CL");
                        json.put("fileName", filename);
                        logger.debug("Sending message to owner of local file " + filename);
                        sendUnicastMessage(InetAddress.getByName(destAddress), json);
                    }
                }

                // Send a message to the owners of each replicated file. At the owner, the removeReplicatedFiles method will be executed
                folder = new File("/home/pi/replicatedFiles/");
                listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        String filename = file.getName();
                        String destAddress = getFileLocation(file);
                        JSONObject json = new JSONObject();
                        json.put("typeOfMsg", "replicationShutdown");
                        json.put("typeOfSource","replicated");
                        json.put("typeOfDest","owned");
                        json.put("typeOfNode", "CL");
                        json.put("fileName", filename);
                        logger.debug("Sending message to owner of replicated file " + filename);
                        sendUnicastMessage(InetAddress.getByName(destAddress), json);
                    }
                }

                // Send the owned files to the previous neighbour.
                folder = new File("/home/pi/ownedFiles/");
                listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        String filename = file.getName();
                        String previousNeighbor = responseJSON.getString("previousNode");

                        //update log file
                        File logFile = new File("/home/pi/logFiles/" +filename+ "Log");
                        JSONObject jsonLog = new JSONObject(logFile);
                        jsonLog.put("owner", previousNeighbor);

                        //send msg (with fileTransfer) to previous neighbour:
                        InetAddress[] address = {InetAddress.getByName(previousNeighbor)};
                        int succes = FileTransfer.sendFile(address, file.getPath(), "replication");
                        if(succes!=0)
                            FileTransfer.sendFile(address, logFile.getPath(), "log");
                    }
                }

                logger.debug("Stopping replicationUpdate.");
                replicationUpdateThread.stop();

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
     * REST request to get InetAddress of the node containing the file location.
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
     * REST request to get the number of nodes in the network.
     * @return
     * @throws IOException
     */
    public int getNumberOfNodes() throws IOException {
        if (nsIP == null){
            logger.warn("Not connected to any NameServer, Please use !connect before requesting a file");
            return -1;
        }
        String hostName = nsIP.getHostAddress();
        String url ="http://"+hostName+":8080/numberOfNodes";
        logger.debug("Trying to connect with "+url);
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
            int numberOfNodes = jsonResponse.getInt("numberOfNodes");
            logger.debug("Number of nodes is: "+ (numberOfNodes + 1));
            return numberOfNodes + 1;
        }
        logger.error("Request failed!");

        // an error happened
        return -1;
    }


    /**
     * REST request to get InetAddress of node location.
     * @param nodeHash
     * @return
     * @throws IOException
     */
    public InetAddress nodeRequest(Integer nodeHash) throws IOException {
        if (nsIP == null){
            logger.warn("Not connected to any NameServer, Please use !connect before requesting a file");
            return null;
        }
        String hostName = nsIP.getHostAddress();
        String url ="http://"+hostName+":8080/nodeRequest?nodeHash=" + nodeHash;
        logger.debug("Trying to connect with "+url);
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
            logger.debug("Hostname of node is: "+ip);
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
    
    public static Map<String,InetAddress> getReplicatedFilesMap(){
        return replicatedFilesMap;
    }

    /***
     * Delete a replicated file on this node with its logfile if it is an owner if it is never been downloaded
     * If it's downloaded then only the log file will be updated
     * @param fileName The name of the file that must be removed
     * @param typeOfDest The type of the destination
     */
    public void removeReplicatedFile(String fileName, String typeOfDest, String typeOfSource, InetAddress sourceAddress) {

        try {
            //The owner will receive this msg and then checks if this file is already downloaded,
            //If yes then we will only update the log file
            //If no then we will remove the log and then the file from all the download locations
            if(typeOfDest.equals("owner")) {

                File file = new File("/home/pi/logFiles/" +fileName+ "Log");
                JSONObject jsonLog = new JSONObject(file);
                boolean isDownloaded = jsonLog.getBoolean("isDownloaded");
                ArrayList<String> downloadLocations = (ArrayList<String>) jsonLog.get("downloadLocations");

                if (!isDownloaded && typeOfSource.equals("local")){

                    logger.debug("File " + fileName + "has not been downloaded yet so it will be removed...");

                    JSONObject json = new JSONObject();
                    json.put("typeOfMsg","replicationShutdown");
                    json.put("typeOfDest","download");
                    json.put("fileName",fileName);

                    for (String hostName : downloadLocations){

                        sendUnicastMessage(InetAddress.getByName(hostName), json);
                        logger.debug("Sent unicast to notify download location " + hostName + " must delete " + fileName);

                    }

                    file = new File("/home/pi/ownedFiles/" + fileName);
                    boolean isDeleted = file.delete();
                    if (isDeleted){

                        logger.trace("Owned file: "+fileName+ " is successfully deleted");
                    }
                    else logger.error("Owned file: " +fileName+ " is not successfully deleted");

                    file = new File("/home/pi/logFiles/" +fileName+ "Log");
                    isDeleted = file.delete();
                    if (isDeleted){

                        logger.trace("Log file: "+fileName+ " is successfully deleted");
                    }
                    else logger.error("Log file: " +fileName+ " is not successfully deleted");

                }
                else{

                    logger.debug("File " + fileName + "has already been downloaded so the log file will be updated...");

                    String sourceName = sourceAddress.getHostName();
                    downloadLocations.remove(sourceName);
                    jsonLog.put("downloadLocations",downloadLocations);
                    byte[] contents = jsonLog.toString().getBytes();
                    int bytesLength = contents.length;
                    fileSem.acquire();
                    FileOutputStream fos = new FileOutputStream("/home/pi/logFiles/" + fileName + "Log");
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    bos.write(contents, 0, bytesLength); // content, offset, how many bytes are read.
                    bos.flush();
                    bos.close();
                    fos.close();
                    logger.debug("Log file " + fileName + "Log" + " is updated!");
                    fileSem.release();
                }
            }

            else if(typeOfDest.equals("download")) {

                File file = new File("/home/pi/replicatedFiles/" + fileName);
                boolean isDeleted = file.delete();
                if (isDeleted){

                    logger.trace("Replicated file: "+fileName+ " is successfully deleted");
                }
                else logger.error("Replicated file: " +fileName+ " is not successfully deleted");

            }

        } catch (Exception e) {
            logger.error(e);
        }

    }

//    /***
//     * This method will delete a specific file
//     * @param directory is the directory of the file
//     * @param fileName is the name of the file
//     */
//    public void removeFile(String directory, String fileName){
//
//        File file = new File(directory+fileName);
//        boolean isDeleted = file.delete();
//        if (isDeleted){
//
//            logger.trace("Shutdown file: "+fileName+ " is successfully deleted");
//        }
//        else logger.error("Shutdown file: " +fileName+ " is not successfully deleted");
//
//    }


    //public void fileRequest(Socket clientSocket){
        // todo finish this method.
    //}

    public void receiveFile(InputStream inputStream, JSONObject json, OutputStream outputStream, String type){ // todo if file already exists, don't save it!!
        try {
            int numberOfNodes = getNumberOfNodes();
            logger.debug("The number of nodes is: " + numberOfNodes);
            int fileStatus = 2; // We assume that standard everything is ok and we are the correct receiver.
            String fileName = json.getString("fileName");

            if(type.equals("replication")){
                logger.info("RECEIVING FILE " + fileName + " : The type of file is a replication file.");
                File file = new File("/home/pi/localFiles/" + fileName);
                if(file.exists() && numberOfNodes > 2){
                    fileStatus = 1; // we have the file locally.
                    logger.info("RECEIVING FILE " + fileName + " : File is already stored locally.. \nGiving a response back.\n");
                }
                file = new File("/home/pi/ownedFiles/" + fileName);
                if(file.exists() && numberOfNodes > 2){
                    fileStatus = 0; // We are the owners of the file.
                    logger.info("RECEIVING FILE " + fileName + " : We own the file.. \nGiving a response back.\n");
                }
            }

            logger.info("RECEIVING FILE " + fileName + " : Send JSON reply.");
            outputStream.write(fileStatus);
            logger.info("RECEIVING FILE " + fileName + " : JSON reply sent.");
            logger.debug("RECEIVING FILE " + fileName + " : sent fileStatus equaling " + fileStatus);

            if(fileStatus == 2) {
                byte[] contents = new byte[10000];
                logger.info("RECEIVING FILE " + fileName + " : We will receive a replicated file " + fileName);
                File folder;
                if (type.equals("replication")) {
                    folder = new File("/home/pi/ownedFiles/");
                } else if (type.equals("log")) {
                    folder = new File("/home/pi/logFiles/");
                } else throw new Exception("Wrong typeOfMsg!");
                if (!folder.exists()) {
                    boolean succes = folder.mkdir();
                    if (!succes) {
                        throw new Exception("Could not create directory " + folder.getName() + "!");
                    }
                }
                //Initialize the FileOutputStream to the output file's full path.
                fileSem.acquire();
                FileOutputStream fos;
                if (type.equals("replication")) {
                    fos = new FileOutputStream("/home/pi/ownedFiles/" + fileName);
                } else {
                    fos = new FileOutputStream("/home/pi/logFiles/" + fileName);
                }
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                fileSem.release();

                //Number of bytes read in one read() call
                int bytesRead = 0;
                if (type.equals("replication")) {
                    logger.info("RECEIVING FILE " + fileName + " : Starting to write the file to: /home/pi/ownedFiles/" + fileName);
                } else {
                    logger.info("RECEIVING FILE " + fileName + " : Starting to write the file to: /home/pi/logFiles/" + fileName);
                }
                while ((bytesRead = inputStream.read(contents)) != -1) { // -1 ==> no data left to read.
                    fileSem.acquire();
                    bos.write(contents, 0, bytesRead); // content, offset, how many bytes are read.
                    fileSem.release();
                }
                fileSem.acquire();
                bos.flush();
                bos.close();
                fos.close();
                fileSem.release();

                logger.info("RECEIVING FILE " + fileName + " : File saved successfully!");
            }
        } catch(Exception e){
            logger.error(e);
        }
    }

    public void checkIfOwnerChanged(){
        try {
            logger.info("Checking if the files have new owners.");
            File folder = new File("/home/pi/ownedFiles/");

            if(!folder.exists()){
                boolean success = folder.mkdir();
                if(!success){
                    throw new Exception("Could not create directory " + folder.getName() + "!");
                }
            }
            File[] listOfFiles = folder.listFiles();
            if(listOfFiles != null) {
                for (File file : listOfFiles) {
                    logger.info("SENDING FILE " + file.getName() + " : Check who is the owner of file "+ file.getName() + "according to the NamingServer.");
                    InetAddress[] address = {fileRequest(file.getName())};

                    if(!address[0].equals(ownNodeAddress)){
                        int fileStatus = FileTransfer.sendFile(address, file.getPath(), "replication");
                        moveFile(file, "/home/pi/replicatedFiles/");
                        if(fileStatus != 0){
                            logger.info("SENDING FILE " + file.getName() + " : File successfully replicated.");
                            fileSem.acquire();
                            File logfile = new File("/home/pi/logFiles/" + file.getName() + "Log");
                            FileInputStream fis = new FileInputStream(logfile); // Reads bytes from the file.
                            BufferedInputStream bis = new BufferedInputStream(fis); // Gives extra functionality to fileInputStream so it can buffer data.
                            byte[] contents;
                            StringBuilder logstring = new StringBuilder();
                            long fileLength = logfile.length();
                            logger.info("SENDING FILE " + file.getName() + " : The size of the logfile is: " + fileLength +" bytes");
                            long current = 0;

                            while(current!=fileLength){
                                int size = 10000;
                                if(fileLength - current >= size)
                                    current += size;
                                else{
                                    size = (int)(fileLength - current);
                                    current = fileLength;
                                }
                                contents = new byte[size];
                                bis.read(contents, 0, size);
                                String tempString = new String(contents);
                                logger.debug("SENDING FILE " + file.getName() + " : tempString is " + tempString);
                                logstring.append(tempString);
                                logger.debug("SENDING FILE " + file.getName() + " : logstring is " + logstring);
                            }
                            logger.debug("SENDING FILE " + file.getName() + " : The logstring contains: " + logstring);
                            JSONObject logjson = new JSONObject(logstring.toString());
                            logjson.put("owner", address[0].getHostName());
                            logjson.put("isDownloaded", true);
                            logjson.put("downloadLocations", logjson.getString("downloadLocations").concat("," + ownNodeAddress.getHostName()));
                            fis.close();
                            bis.close();

                            fileSem.release();

                            contents = logjson.toString().getBytes();
                            int bytesLength = contents.length;
                            fileSem.acquire();
                            FileOutputStream fos = new FileOutputStream("/home/pi/logFiles/" + file.getName() + "Log"); // todo make sure that this folder exists
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            bos.write(contents, 0, bytesLength); // content, offset, how many bytes are read.
                            bos.flush();
                            bos.close();
                            fos.close();
                            fileSem.release();
                            logger.info("SENDING FILE " + file.getName() + " : log file adjusted.");
                            logger.info("SENDING FILE " + file.getName() + " : Sending log file to " + address[0] + ".");
                            FileTransfer.sendFile(address, "/home/pi/logFiles/" + file.getName() + "Log", "log");
                            logger.info("SENDING FILE " + file.getName() + " : Log file successfully sent!");
                        }
                        File logfile = new File("/home/pi/logFiles/" + file.getName() + "Log");
                        logger.info("SENDING FILE " + file.getName() + " : Remove local log file.");
                        boolean success = logfile.delete();
                        if(!success){
                            throw new Exception("Could not delete logFile " + logfile.getName() + "!");
                        }
                    }
                }
            }
            else{
                logger.warn("No local files to replicate.");
            }
        } catch (Exception e){
            logger.error(e);
        }
    }

    public void createLogFile(InetAddress inet, String fileName){
        try {
            JSONObject json = new JSONObject();
            json.put("owner", inet.getHostName());
            json.put("isDownloaded", false);
            json.put("downloadLocations", ownNodeAddress.getHostName());

            File folder = new File("/home/pi/logFiles/");
            if(!folder.exists()){
                boolean success = folder.mkdir();
                if(!success){
                    throw new Exception("Could not create directory " + folder.getName() + "!");
                }
            }

            byte[] contents = json.toString().getBytes();
            int bytesLength = contents.length;
            fileSem.acquire();
            FileOutputStream fos = new FileOutputStream("/home/pi/logFiles/" + fileName); // todo make sure that this folder exists
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(contents, 0, bytesLength); // content, offset, how many bytes are read.
            bos.flush();
            bos.close();
            fos.close();
            fileSem.release();
        } catch (Exception e){
            logger.error(e);
        }
    }

    /**
     *
     * @param file
     * @param path to were to write
     */
    public void moveFile(File file, String path){
        // renaming the file and moving it to a new location
        try {
            fileSem.acquire();
            File folder = new File(path);
            if(!folder.exists()){
                boolean success = folder.mkdir();
                if(!success){
                    throw new Exception("Could not create directory " + folder.getName() + "!");
                }
            }
            logger.info("Moving file " + file.getName() + " to " + path + file.getName());
            if (file.renameTo(new File(path + file.getName()))) {
                // if file copied successfully then delete the original file
                file.delete();
                logger.info("File moved successfully");
            } else {
                logger.warn("Failed to move the file");
            }
            fileSem.release();
        } catch(Exception e){
            logger.error(e);
            fileSem.release();
        }
    }

    public void prepareFoldersStartup(){
        try {
            File folder = new File("/home/pi/ownedFiles/");
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                if(listOfFiles != null) {
                    for (File file : listOfFiles) {
                        file.delete();
                    }
                }
            }
            folder = new File("/home/pi/logFiles/");
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                if(listOfFiles != null) {
                    for (File file : listOfFiles) {
                        file.delete();
                    }
                }
            }
            folder = new File("/home/pi/replicatedFiles/");
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                if(listOfFiles != null) {
                    for (File file : listOfFiles) {
                        file.delete();
                    }
                }
            }
        }
        catch (Exception e){
            logger.error(e);
        }
    }

    public void sendFileAndCreatedLogFile(File file){
        try {
            logger.info("SENDING FILE " + file.getName() + " : replicating file " + file.getName());
            InetAddress[] address = {fileRequest(file.getName())};
            logger.debug("SENDING FILE " + file.getName() + " : Address to send to is: " + address[0]);
            logger.debug("SENDING FILE " + file.getName() + " : My own localHost address is: " + ownNodeAddress);
            if (address[0].equals(ownNodeAddress)) {
                logger.warn("SENDING FILE " + file.getName() + " : Address to send to is myself, changing this address.");
                while (address[0].equals(ownNodeAddress)) {
                    address[0] = nodeRequest(previousID);
                    logger.info("SENDING FILE " + file.getName() + " : Current address to send to is: " + address[0]);
                }
            }
            int proceed = FileTransfer.sendFile(address, file.getPath(), "replication");
            if (proceed != 0) {
                logger.info("SENDING FILE " + file.getName() + " : File successfully replicated.");
                logger.info("SENDING FILE " + file.getName() + " : Creating a log file for file " + file.getName() + "Log");
                createLogFile(address[0], file.getName() + "Log");
                logger.info("SENDING FILE " + file.getName() + " : Sending log file to " + address[0]);
                FileTransfer.sendFile(address, "/home/pi/logFiles/" + file.getName() + "Log", "log");
            }
            logger.info("SENDING FILE " + file.getName() + " : Removing local log file.");
            File logfile = new File("/home/pi/logFiles/" + file.getName() + "Log");
            boolean success = logfile.delete();
            if (!success) {
                throw new Exception("Could not delete logFile " + logfile.getName() + "!");
            }
            if (proceed != 0)
                logger.info("SENDING FILE " + file.getName() + " : Log file successfully sent!");
            else
                logger.info("SENDING FILE " + file.getName() + " : File transfer aborted.");
        } catch (Exception e){
            logger.error(e);
        }
    }

    public void replicationStart()
    {
        try {
            prepareFoldersStartup();
            logger.info("Starting replication process.");
            File folder = new File("/home/pi/localFiles/");
            if(!folder.exists()){
                boolean success = folder.mkdir();
                if(!success){
                    throw new Exception("Could not create directory " + folder.getName() + "!");
                }
            }
            File[] listOfFiles = folder.listFiles();
            if(listOfFiles != null) {
                for (File file : listOfFiles) {
                    sendFileAndCreatedLogFile(file);
                }
            }
            else{
                logger.warn("No local files to replicate.");
            }
            logger.debug("Starting ReplicationUpdateThread.");
            replicationUpdateThread = new ReplicationUpdateThread();
            replicationUpdateThread.start();
        } catch (Exception e){
            logger.error(e);
        }
    }

}

