package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class FileTransfer {
    private static Semaphore sendingSem = new Semaphore(1);
    private static Semaphore readSem = new Semaphore(1);
    private static final Logger logger = LogManager.getLogger();

    public FileTransfer(){
        // nothing to do
    }

    /**
     * This method will send a file to another node.
     * @param toSend contains the ip address to were to send the file.
     * @param filePath contains the path to the file.
     * @param typeOfMessage contains which type of message this is.
     *                      This should be either "replication" of "fileRequest".
     */
    public void sendFile(InetAddress toSend, String filePath, String typeOfMessage){
        try {
            // Send the json object first so the other node knows what type of message this is.
            logger.info("Sending a file to: " + toSend);
            logger.info("The file that will be send is: "+ filePath);
            JSONObject json = new JSONObject();
            //Specify the file
            readSem.acquire();
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file); // Reads bytes from the file.
            BufferedInputStream bis = new BufferedInputStream(fis); // Gives extra functionality to fileInputStream so it can buffer data.
            readSem.release();
            json.put("typeOfMsg", typeOfMessage);
            json.put("typeOfNode", "CL");
            json.put("fileName", file.getName());

            sendingSem.acquire();
            Socket socket = new Socket(toSend, 5000);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(json.toString().getBytes());
            outputStream.flush();
            sendingSem.release();
            logger.info("JSON is successfully sent.");

            byte[] contents;
            readSem.acquire();
            long fileLength = file.length();
            readSem.release();
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
                readSem.acquire();
                bis.read(contents, 0, size);
                readSem.release();
                sendingSem.acquire();
                outputStream.write(contents);
                sendingSem.release();
                logger.info("Sending file ... "+(current*100)/fileLength+"% complete!");
            }
            sendingSem.acquire();
            outputStream.flush();
            outputStream.close();
            socket.close();
            sendingSem.release();
            readSem.acquire();
            fis.close();
            bis.close();
            readSem.release();
            logger.info("File sent succesfully!");
            // todo make sure this send is also received by the other one.
        } catch(Exception e){
            logger.error(e);
        }
    }
}
