package ua.dist8;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class NodeApplication {
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
        System.out.println("This is V1.0");
        System.out.println("Welcome to the client test application!\n");
        while(running){
            System.out.println("\nPlease enter a command.\nType !help for a list of commands: ");
            String input = scanner.nextLine();
            switch(input){
                case "!help":
                    System.out.println("The available commands are:\n!RequestFilePing\n!requestFile\n!printNeighbours\n!connect\n!disconnect\n!loadNeighboursFromNS \n!exit");
                    break;
                case "!requestFilePing":
                    System.out.println("Give the name of the requested file: ");
                    fileName = scanner.nextLine();
                    address = nodeClient.fileRequest(fileName);
                    if(address == null)
                        break;
                    //todo ping
                    break;
                case "!requestFile":
                    System.out.println("Give the name of the requested file: ");
                    fileName = scanner.nextLine();
                    address = nodeClient.fileRequest(fileName);
                    if (address == null)
                        break;
                    System.out.println("File is located at host" + address.getHostName());
                    break;
                case "!connect":
                    nodeClient.multicast();
                    break;
                case "!disconnect":
                    try {
                        nodeClient.shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("try catch print");
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
                        e.printStackTrace();
                    }
                    System.out.println("try catch print");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid command!\n");
                    break;
            }
        }
    }
}
