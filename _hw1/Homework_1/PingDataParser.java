import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingDataParser {

    public static void main(String[] args) {
        // Specify the file path
        String filePath = "ping.txt";

        try {
            //read the file containing ping data
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            //initalize arraylist which will store list of round-trip delays
            List<Double> roundTripDelays = new ArrayList<>();

            //skip the first line
            reader.readLine();

            //extract round trip delays from each line
            String line;
            while ((line = reader.readLine()) != null) {
                //split the line into an array of strings using "time=" as the delimiter
                String[] parts = line.split("time=");
                //check if the array has more than one element
                if (parts.length > 1) {
                    // Split the 2nd elem of the array using space as the delimiter
                    //split "24.731 ms" into ["24.731", "ms"]
                    double delay = Double.parseDouble(parts[1].split("\\s+")[0]);
                    roundTripDelays.add(delay);
                }
            }

            double averageQueuingDelay = findAvgQueueingDelay(roundTripDelays);

            //print results
            System.out.println("Average round trip queuing delay: " + averageQueuingDelay + " ms");

            //close reader
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Helper function to find average queueing delay
    static double findAvgQueueingDelay(List<Double> roundTripDelays){
        //min delay is zero queuing, sort round-trip delays and grab first number (will be min)
        Collections.sort(roundTripDelays);
        double minDelay = roundTripDelays.get(0);

        //calculate queuing delay for each packet (delay - min delay)
        List<Double> queuingDelays = new ArrayList<>();
        for (double delay : roundTripDelays) {
            queuingDelays.add(delay - minDelay);
        }

        //calculate average queuing delay
        double sumQueuingDelays = 0;
        for (double queuingDelay : queuingDelays) {
            sumQueuingDelays += queuingDelay;
        }
        return sumQueuingDelays / queuingDelays.size();
    }
}
