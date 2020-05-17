package ua.dist8;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
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
     *                      This should be either "replication", "fileRequest" or "log".
     */
    static public int sendFile(InetAddress toSend, String filePath, String typeOfMessage){
        int fileStatus = 1;
        try {
            JSONObject json = new JSONObject();
            InetAddress ownAddress = NodeClient.getOwnNodeAddress();
            //Specify the file
            readSem.acquire();
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file); // Reads bytes from the file.
            BufferedInputStream bis = new BufferedInputStream(fis); // Gives extra functionality to fileInputStream so it can buffer data.
            readSem.release();
            json.put("typeOfMsg", typeOfMessage);
            json.put("typeOfNode", "CL");
            json.put("fileName", file.getName());
            OutputStream outputStream = null;
            InputStream inputStream = null;
            Socket socket = null;
            if(toSend.equals(ownAddress))
                fileStatus = 0;
            while (fileStatus == 1) {
                // Send the json object first so the other node knows what type of message this is.
                logger.info("SENDING FILE " + file.getName() + " : Sending a file to: " + toSend);
                logger.info("SENDING FILE " + file.getName() + " : The file that will be sent is: " + filePath);
                sendingSem.acquire();
                socket = new Socket(toSend, 5000);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                outputStream.write(json.toString().getBytes());
                outputStream.flush();
                sendingSem.release();
                logger.info("SENDING FILE " + file.getName() + " : JSON is successfully sent.");

                logger.info("SENDING FILE " + file.getName() + " : Waiting for JSON acknowledge.");
                fileStatus = inputStream.read();
                logger.info("SENDING FILE " + file.getName() + " : JSON acknowledge received.");
                if(fileStatus == 1) {
                    logger.info("SENDING FILE " + file.getName() + " : sent to the wrong address, trying again with a new address...");
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                    NodeClient nodeClient = NodeClient.getInstance();
                    do {
                        toSend = nodeClient.nodeRequest(Hashing.createHash(toSend.getHostName()) - 1);
                        logger.info("SENDING FILE " + file.getName() + " : New address is: "+toSend);
                    } while(toSend.equals(ownAddress));
                }
            }

            if(fileStatus != 0) {
                byte[] contents;
                readSem.acquire();
                long fileLength = file.length();
                logger.info("SENDING FILE " + file.getName() + " : The size of the file is: " + fileLength + " bytes");
                readSem.release();
                long current = 0;

                while (current != fileLength) {
                    int size = 10000;
                    if (fileLength - current >= size)
                        current += size;
                    else {
                        size = (int) (fileLength - current);
                        current = fileLength;
                    }
                    contents = new byte[size];
                    readSem.acquire();
                    bis.read(contents, 0, size);
                    readSem.release();
                    sendingSem.acquire();
                    outputStream.write(contents);
                    sendingSem.release();
                    logger.info("SENDING FILE " + file.getName() + " : Sending file ... " + (current * 100) / fileLength + "% complete!");
                }
                logger.info("SENDING FILE " + file.getName() + " : File sent succesfully!");
            }
            else
                logger.info("SENDING FILE " + file.getName() + " : The other one already had the file.");
            sendingSem.acquire();
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            socket.close();
            sendingSem.release();
            readSem.acquire();
            fis.close();
            bis.close();
            readSem.release();
            // todo make sure this send is also received by the other one.
        } catch(Exception e){
            logger.error(e);
        }
        return fileStatus;
    }
}
