package ua.dist8;



import org.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class NodeApplication {

    private static final Logger logger = LogManager.getLogger();
    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        boolean running = true;
        NodeClient nodeClient = NodeClient.getInstance();

        TCPListener tcpListener = null;
        UDPListener udpListener = null;
        String fileName;
        InetAddress address;
        Scanner scanner = new Scanner(System.in);
        logger.info("This is version 3.3.3");
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