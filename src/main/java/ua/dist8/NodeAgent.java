package ua.dist8;
import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.behaviours.*;


public class NodeAgent extends Agent
{
    protected void setup()
    {
        addBehaviour( new AgentBehaviour( this ) );
    }
}

class AgentBehaviour extends SimpleBehaviour
{
    public AgentBehaviour(Agent agent) {
        super(agent);
    }

    public void action()
    {
        System.out.println( "Initializing agent on " + myAgent.getLocalName() );
        // load in directory of local files
        // while loop
        // listen on
    }

    private boolean finished = false;
    public  boolean done() {  return finished;  }

} //End class B1