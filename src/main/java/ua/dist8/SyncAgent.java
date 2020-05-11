package ua.dist8;
import jade.core.Agent;

import jade.core.behaviours.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

//runnable and serializable already implemented in class Agent

public class SyncAgent extends Agent {
    private static final Logger logger = LogManager.getLogger();
    //global list that agents will update. First arg is filename, second arg is the lock ("Open" or "Closed")

    //Explanation of how Agent works: When a behavior gets called, we will execute the action method in that behavior.
    //when we create a new agent, the setup will be executed where we will start the behaviour of our sync agent
    protected void setup()
    {
        addBehaviour( new SyncAgentBehaviour( this ) );

    }
}

class SyncAgentBehaviour extends SimpleBehaviour
{
    private HashMap<String,String> synchronizedMap; //all files of the network
    private ConcurrentHashMap<String,String> localListMap; //locally owned files
    private static final Logger logger = LogManager.getLogger();
    private boolean isDone =false;
    public SyncAgentBehaviour(Agent agent) {
        super(agent);
    }

    // we will need two local maps, one updated and one old
    public void action()
    {
        NodeClient nodeClient = null;
        try {
            nodeClient = NodeClient.getInstance();
        }catch(Exception e){
            logger.error(e);
            logger.error("Unable to get nodeClient Instance");
        }

        System.out.println( "Initializing agent on " + myAgent.getLocalName() );
        localListMap = nodeClient.getLocalMap();
        // load in directory of local files
        // while loop
        // listen for changes
        isDone = true;
    }
    public boolean done(){
        return isDone = true;
    }



} //End class B1