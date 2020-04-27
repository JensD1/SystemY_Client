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

        TCPListener tcpListener = new TCPListener();
        UDPListener udpListener = new UDPListener();
        tcpListener.start();
        udpListener.start();
        String fileName;
        InetAddress address;
        Scanner scanner = new Scanner(System.in);
        logger.info("This is V2.0");
        logger.info("Welcome to the client test application!");
        while(running){
            logger.info("Please enter a command. Type !help for a list of commands: ");
            String input = scanner.nextLine();
            switch(input){
                case "!help":
                    logger.info("The available commands are:\n!RequestFilePing\n!requestFile\n!printNeighbours\n!connect\n!disconnect\n!loadNeighboursFromNS \n!exit");
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
                    nodeClient.multicast();
                    break;
                case "!disconnect":
                    try {
                        nodeClient.shutdown();
                    } catch (Exception e) {
                        logger.error(e);
                    }
                    
                    break;
                case "!printNeighbours":
                    nodeClient.printNeighbours();
                    break;
                case "!loadNeighboursFromNS":
                    nodeClient.getNeighbours();
                    break;
                case "!exit":
                    try {
                        nodeClient.shutdown();
                    } catch (InterruptedException e) {
                        logger.error(e);
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