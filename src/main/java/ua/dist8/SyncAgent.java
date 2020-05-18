package ua.dist8;
import jade.core.Agent;

import jade.core.behaviours.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.UUID;

//runnable and serializable already implemented in class Agent

public class SyncAgent extends Agent {
    private static final Logger logger = LogManager.getLogger();
    private static HashMap<String,String> synchronizedMap; //all files of the network
    private static ConcurrentHashMap<String,String> localListMap;
    //locally owned files
    private boolean isDone = false;
    //global list that agents will update. First arg is filename, second arg is the lock ("Open" or "Closed")

    //Explanation of how Agent works: When a behavior gets called, we will execute the action method in that behavior.
    //when we create a new agent, the setup will be executed where we will start the behaviour of our sync agent
    // we use different cyclic behaviours
    // one to listen for changes in the folder that sends update to next node
    //one to listen for messages from the next node. If the received list is the same as local list, do nothing
    // else
    protected void setup()
    {


        NodeClient nodeClient = null;
        nodeClient = NodeClient.getInstance();
        localListMap =  nodeClient.getLocalMap();
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();


        //listen for changes in the folder
        //if a file got added, set lock to open
        //send the changes to the previous node agent
        parallelBehaviour.addSubBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                Path path = Paths.get("location here");
                try {
                    WatchService watcher = path.getFileSystem().newWatchService();
                    path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                    WatchKey watchKey = watcher.take();
                    List<WatchEvent<?>> events = watchKey.pollEvents();
                    for (WatchEvent event : events) {
                        //check if the event refers to a new file created
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            //print file name which is newly created
                            String fileName =event.context().toString();
                            System.out.println("Created: " + fileName);
                        }
                    }
                }catch(Exception e){
                    logger.error(e);
                }

            }
        });

        //listen for ACLmessages from the next node.
        //take their list and compare with our list
        // if the lists are different, take their list and make sure that your local files are in the list
        //send the new list to next node
        // this message will have the origin Agent and the list
        // when we receive a message with the origin ourself, we wont send it to the next node
        parallelBehaviour.addSubBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {

            }
        });

        //lock stuff??
        parallelBehaviour.addSubBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {

            }
        });
        this.addBehaviour(parallelBehaviour);


    }
}



