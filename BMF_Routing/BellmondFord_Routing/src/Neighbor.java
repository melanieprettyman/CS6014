
public class Neighbor {
    /*
    * Represents a neighboring router and the cost (distance) to that router.
    * It includes a toString method that returns a string representation of the neighbor and its cost.
    */
    Router router;
    int cost;

    public Neighbor(Router router, int cost) {
        this.router = router;
        this.cost = cost;
    }

    @Override
    public String toString(){
        return "to " + router + " cost: " + cost;
    }
}
