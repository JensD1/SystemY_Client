package ua.dist8;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class TCPThreadHandler extends Thread {

    private Socket clientSocket;
    TCPThreadHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream clientInput = clientSocket.getInputStream();
            byte[] contents = new byte[10000]; // pas dit nog aan !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if( clientInput.read(contents) != -1) { // the message is not empty.
                String message = new String(contents);
                JSONObject json = new JSONObject(message);

                if(json.getString("typeOfMsg").equals("shutdown")){
                    //todo
                }
                else if(json.getString("typeOfMsg").equals("fileRequest")){
                    //todo
                }
                else if(json.getString("typeOfMsg").equals("multicastReply")){
                    //todo
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
