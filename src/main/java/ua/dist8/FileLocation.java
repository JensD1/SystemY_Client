package ua.dist8;

import java.net.InetAddress;

public class FileLocation {
    private String filename;
    private InetAddress inetAddress;

    /**
     * Creates a FileLocation with file name and IP address.
     * @param filename The name of the requested file.
     * @param ip The IP address of the node with the requested file.
     */
    public FileLocation(String filename,InetAddress ip){
        this.filename = filename;
        this.inetAddress = ip;
    }

    /***
     * This function will return an inetAddress.
     * @return IP-address
     */
    public InetAddress getInetAddress(){
        return inetAddress;
    }

    /***
     * This function will return a string.
     * @return filename
     */
    public String getFilename(){
        return  filename;
    }

}
