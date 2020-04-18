package ua.dist8;

import java.net.InetAddress;

public class FileLocation {
    private String filename;
    private InetAddress inetAddress;

    public FileLocation(String filename,InetAddress ip){
        this.filename = filename;
        this.inetAddress = ip;
    }

    public InetAddress getInetAddress(){
        return inetAddress;
    }

    public String getFilename(){
        return  filename;
    }

}
