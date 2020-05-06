package ua.dist8;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;

public class replicationUpdateThread extends Thread{

    @Override
    /***
     * Constantly listens to TCP requests.
     * When there is an incoming request, it generates a new thread to handle it.
     */
    public void run() {

        long  startTime = System.currentTimeMillis();
        long  interval = 1000;

        if (System.currentTimeMillis()-startTime>=interval){

            Map<String, InetAddress> replicatedFilesMap = NodeClient.getReplicatedFilesMap();
            boolean remove;
            File folder = new File("/home/pi/localFiles/");
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                if (file.isFile()) {

                    //Check if there are new files
                    if (!replicatedFilesMap.containsKey(file.getName())){

                        //TODO replicate the new file


                    }

                }
            }

            for (String hashKey : replicatedFilesMap.keySet()) {
                remove = true;
                for (File file : listOfFiles) {
                    if (file.isFile()) {

                        //Check if there are removed files
                        if ( hashKey == file.getName()){

                            remove = false;

                        }

                    }
                }

                if (remove){

                    JSONObject json = new JSONObject();

                    json.put("typeOfMsg","removeReplicatedFile");
                    json.put("fileName",hashKey);
                    try {
                        NodeClient.getInstance().sendUnicastMessage(replicatedFilesMap.get(hashKey),json);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    replicatedFilesMap.remove(hashKey);


                }

            }

        }

    }

}