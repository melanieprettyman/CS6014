
import java.util.HashMap;
/*This class represents a message sent between routers containing the sender, receiver,
* and a map of distances from the sender to other routers. The dump method prints the message
* details, including the sender, receiver, and the distances to other routers.*/
public class Message {
    Router sender, receiver;
    HashMap<Router, Integer> distances;

    public Message(Router sender, Router receiver, HashMap<Router, Integer> distances) {
        this.sender = sender;
        this.receiver = receiver;
        this.distances = distances;
    }

    public void dump() {
        //System.out.println("sender: " + sender + " receiver " + receiver);
        for(Router r : distances.keySet()){
            System.out.println("\t" + r + "\t" + distances.get(r));
        }
    }
}
