package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;


public class ReplicationUpdateThread extends Thread{

    private static final Logger logger = LogManager.getLogger();

    @Override
    /***
     * This method checks at regular interval if there are new/deleted local files.
     * If change has occurred, then the replication map will be updated. Other nodes will be also notified if a file is locally deleted.
     */
    public void run() {

        logger.info("ReplicationUpdateThread started.");
        long  startTime = System.currentTimeMillis();
        long  interval = 1000;
        while(true) {
            if (System.currentTimeMillis() - startTime >= interval) {
                logger.info("Checking if files are removed or added.");
                Map<String, InetAddress> replicatedFilesMap = NodeClient.getReplicatedFilesMap();
                boolean remove;
                File folder = new File("/home/pi/localFiles/");
                File[] listOfFiles = folder.listFiles();

                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        //Check if there are new files
                        if (!replicatedFilesMap.containsKey(file.getName())) {
                            logger.info("There is a file added! Sending it to the rightful owner.");
                            NodeClient.getInstance().sendFileAndCreatedLogFile(file);
                        }

                    }
                }

                for (String hashKey : replicatedFilesMap.keySet()) {
                    remove = true;
                    for (File file : listOfFiles) {
                        if (file.isFile()) {
                            //Check if there are removed files
                            if (hashKey.equals(file.getName())) {
                                remove = false;
                            }

                        }
                    }

                    if (remove) {
                        logger.info("There is a file removed! Deleting it on all other nodes.");
                        JSONObject json = new JSONObject();
                        json.put("typeOfMsg", "replicationShutdown");
                        json.put("typeOfSource", "local");
                        json.put("typeOfDest", "owner");
                        json.put("fileName", hashKey);
                        NodeClient.getInstance().sendUnicastMessage(replicatedFilesMap.get(hashKey), json);
                        replicatedFilesMap.remove(hashKey);
                    }
                }
            }
        }
    }
}