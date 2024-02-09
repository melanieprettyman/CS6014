import java.util.ArrayList;
import java.util.HashMap;

public class NetworkTest {


        public static void main(String[] args) throws InterruptedException {
            // Define the range and step for network sizes you want to test
            int minSize = 10;
            int maxSize = 100;
            int step = 10;

            // A map to store the number of messages required for each network size
            HashMap<Integer, ArrayList<Integer>> convergenceData = new HashMap<>();

            for (int size = minSize; size <= maxSize; size += step) {
                ArrayList<Integer> messagesForSize = new ArrayList<>();
                for (int trial = 0; trial < 5; trial++) { // Adjust the number of trials per size if needed
                    Network.reset(); // Reset the network state before each trial
                    Network.makeProbablisticNetwork(size); // Or use makeSimpleNetwork() if you want to test that
                    Network.startup();
                    Network.runBellmanFord();
                    messagesForSize.add(Network.getMessageCount());
                }
                convergenceData.put(size, messagesForSize);
            }

            // Output the data for plotting
            for (Integer size : convergenceData.keySet()) {
                System.out.println("Network Size: " + size + ", Messages: " + convergenceData.get(size));
            }
        }
    }


