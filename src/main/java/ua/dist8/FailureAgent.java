package ua.dist8;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;


public class FailureAgent extends Agent
{
    protected void setup()
    {
        addBehaviour( new FailureAgentBehaviour( this ) );
    }
}

class FailureAgentBehaviour extends SimpleBehaviour
{
    public FailureAgentBehaviour(Agent agent) {
        super(agent);
    }

    public void action()
    {
        System.out.println( "Initializing agent on " + myAgent.getLocalName() );
        // load in directory of local files
        // while loop
        // listen for changes
    }

    private boolean finished = false;
    public  boolean done() {  return finished;  }

} //End class B1