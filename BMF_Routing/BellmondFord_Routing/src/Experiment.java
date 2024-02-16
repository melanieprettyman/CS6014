
    import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

    public class Experiment {

        public static void main(String[] args) throws InterruptedException {
            int startSize = 5;
            int maxSize = 100;
            int stepSize = 10;
            int trialsPerSize = 20;

            HashMap<Integer, Double> averageMessagesPerSize = new HashMap<>();

            for (int size = startSize; size <= maxSize; size += stepSize) {
                ArrayList<Integer> messagesForSize = new ArrayList<>();
                for (int trial = 0; trial < trialsPerSize; trial++) {
                    Network.reset(); // Ensure the network is reset before each trial
                    Network.makeProbablisticNetwork(size); // Create a network of the current size
                    Network.startup(); // Initialize routers
                    Network.runBellmanFord(); // Run the Bellman-Ford algorithm
                    messagesForSize.add(Network.getMessageCount()); // Record the message count for this trial
                }
                // Calculate the average number of messages for this network size
                double averageMessages = messagesForSize.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                averageMessagesPerSize.put(size, averageMessages);
            }

            // Print the results in a simple table format suitable for Excel
            System.out.println("Network Size\tAverage Messages");
            averageMessagesPerSize.forEach((size, avgMessages) -> System.out.println(size + "\t" + avgMessages));
        }
    }


