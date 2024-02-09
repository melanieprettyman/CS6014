
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Router {

    private HashMap<Router, Integer> distances;
    private String name;
    public Router(String name) {
        this.distances = new HashMap<>();
        this.name = name;
    }

    //This method initializes the router's distance table and broadcasts its initial state to its neighbors.
    //As soon as the network is online,
    //fill in your initial distance table and broadcast it to your neighbors
    public void onInit() throws InterruptedException {
        // Retrieve the set of neighbors for this router
        HashSet<Neighbor> neighbors = Network.getNeighbors(this);
        //For each neighbor of this router:
        for(Neighbor neighbor: neighbors){
            //Add the neighbor to the distances map with the cost to reach that neighbor.
            distances.put(neighbor.router, neighbor.cost);
        }
        //Create and send new message containing this router as the sender, the distance map, and
        //loop through neighbors and send individually (broadcast)
        for(Neighbor neighbor : neighbors) {
            HashMap<Router, Integer> copyOfDistances = new HashMap<>(this.distances);
            Message routerMsg = new Message(this, neighbor.router, copyOfDistances);
            Network.sendDistanceMessage(routerMsg);
        }

    }
    /*
    This method processes incoming distance messages and updates the router's distance table
    if new information provides a shorter path to any router. It then broadcasts the updated
    table to its neighbors if any changes were made.
    */
    public void onDistanceMessage(Message message) throws InterruptedException {
        boolean updated = false; // Flag to track if the distance table was updated
        HashSet<Neighbor> myNeighbors = Network.getNeighbors(this);


        //Iterate over each entry in the received message's distances map
        for (Map.Entry<Router, Integer> entry : message.distances.entrySet()) {
            //Calculate the potential new cost to the router in the entry as the sum of the message sender's
            // cost to this router and the entry's cost.
            Router entryRouter = entry.getKey();
            Integer messageSendersCost = get_Senders_Cost_To_Router(message, myNeighbors);
            Integer costToEntryRouterThroughSender = entry.getValue() + messageSendersCost;


            if (!this.distances.containsKey(entryRouter) // If the router in the entry is not in this router's distances map
                    || this.distances.get(entryRouter) < costToEntryRouterThroughSender) //or the new cost is lower than the existing cost
            {
                //Update this router's distances map with the new cost
                this.distances.put(entryRouter, costToEntryRouterThroughSender);
                updated = true;
            }
            //If the distances map has changed
            if (updated) {
                // Send updated distances to each neighbor directly
                for (Neighbor neighbor : myNeighbors) {
                    Message updatedMessage = new Message(this, neighbor.router, new HashMap<>(this.distances));
                    Network.sendDistanceMessage(updatedMessage);
                }
            }

        }
    }
    /*
    onDistanceMessage HELPER METHOD
    links map in Network class maps Router objects to sets of Neighbor objects
    (where each Neighbor encapsulates a directly connected router and the cost to reach it),
    */
    private Integer get_Senders_Cost_To_Router(Message message, HashSet<Neighbor> myNeighbors) {
        Integer costToSender = Integer.MAX_VALUE; // Default to MAX_VALUE if not found

        for (Neighbor neighbor : myNeighbors) {
            if (neighbor.router.equals(message.sender)) {
                costToSender = neighbor.cost;
            }
        }
        return costToSender;
    }
    public void dumpDistanceTable() {
        System.out.println("router: " + this);
        for(Router r : distances.keySet()){
            System.out.println("\t" + r + "\t" + distances.get(r));
        }
    }

    @Override
    public String toString(){
        return "Router: " + name;
    }


}
