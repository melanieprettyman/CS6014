import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        //1. Initializes the network structure
        //Network.makeSimpleNetwork(); //use this for testing/debugging
        Network.makeProbablisticNetwork(25); //use this for the plotting part

        //2. Prints the current network configuration
        Network.dump();

        //3. Initializes all routers in the network
        Network.startup();

        //4. Processes all queued messages to compute routing tables
        Network.runBellmanFord();

        //5. Prints the distance table of each router.
        System.out.println("done building tables!");
        for (Router r : Network.getRouters()) {
            r.dumpDistanceTable();
        }
        //6. Retrieves the total count of messages processed.
        System.out.println("total messages: " + Network.getMessageCount());
    }


}
