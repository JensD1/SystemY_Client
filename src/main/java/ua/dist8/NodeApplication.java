package ua.dist8;



import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import org.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class NodeApplication {

    private static final Logger logger = LogManager.getLogger();
    public static void main(String[] args) throws IOException, JSONException {
        boolean running = true;
        NodeClient nodeClient = NodeClient.getInstance();
        TCPListener tcpListener = null;
        UDPListener udpListener = null;
        String fileName;
        InetAddress address;
        jade.core.Runtime runtime = jade.core.Runtime.instance();

        //initialize the local list of owned files
        nodeClient.initLocalList();
        //Create a Profile, where the launch arguments are stored
        Profile profile = new ProfileImpl();
        AgentContainer container = null;
        profile.setParameter(Profile.CONTAINER_NAME, "TestContainer");
        profile.setParameter(Profile.MAIN_HOST, "host2");//namingserver will have the main container and is located at host 2

        String name = nodeClient.getHostName();

        try {
            container = runtime.createAgentContainer(profile);
        }catch(Exception e){
            logger.error(e);
            logger.error("Unable to connect to the main container on the NamingServer..");
        }
        try {
            AgentController ac = container.createNewAgent( name, "NodeAgent", null );
            ac.start();
        }
        catch (Exception e){
            logger.error(e);
        }



        Scanner scanner = new Scanner(System.in);
        logger.info("This is version 2.5.6");
        logger.info("Welcome to the client test application!");
        while(running){
            logger.info("Please enter a command. Type !help for a list of commands: ");
            String input = scanner.nextLine();
            switch(input){
                case "!help":
                    logger.info("The available commands are:\n!RequestFilePing\n!requestFile\n!printNeighbours\n!connect\n!disconnect\n!exit");
                    break;
                case "!requestFilePing":
                    logger.info("Give the name of the requested file: ");
                    fileName = scanner.nextLine();
                    address = nodeClient.fileRequest(fileName);
                    if(address == null)
                        break;
                    //todo ping
                    break;
                case "!requestFile":
                    logger.info("Give the name of the requested file: ");
                    fileName = scanner.nextLine();
                    address = nodeClient.fileRequest(fileName);
                    if (address == null)
                        break;
                    logger.info("File is located at host" + address.getHostName());
                    break;
                case "!connect":
                    //todo check if NS exist, otherwise do nothing
                    tcpListener = new TCPListener();
                    udpListener = new UDPListener();
                    if(!tcpListener.isRunning()){
                        tcpListener.start();
                    }
                    if(!udpListener.isRunning()){
                        udpListener.start();
                    }
                    nodeClient.multicast();
                    break;
                case "!disconnect":
                    try {
                        nodeClient.shutdown();
                        if(tcpListener != null) {
                            if (tcpListener.isRunning()) {
                                logger.info("Stopped listening on TCP ports.");
                                tcpListener.stopRunning();
                            }
                        }
                        if(udpListener != null) {
                            if (udpListener.isRunning()) {
                                logger.info("Stopped listening on UDP ports.");
                                udpListener.stopRunning();
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }

                    break;
                case "!printNeighbours":
                    nodeClient.printNeighbours();
                    break;
                case "!exit":
                    nodeClient.shutdown();
                    if(tcpListener != null) {
                        if (tcpListener.isRunning()) {
                            logger.info("Stopped listening on TCP ports.");
                            tcpListener.stopRunning();
                        }
                    }
                    if(udpListener != null) {
                        if (udpListener.isRunning()) {
                            logger.info("Stopped listening on UDP ports.");
                            udpListener.stopRunning();
                        }
                    }
                    logger.debug("exiting program");
                    running = false;
                    break;

                default:
                    logger.error("Invalid command!");
                    break;
            }
        }
    }
}