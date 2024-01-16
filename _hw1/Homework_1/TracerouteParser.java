import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TracerouteParser {
    public static void main(String[] args) {
        String inputFile = "traceroute_output2.txt";
        String outputFile = "average_delay2.txt";

        //read all lines into a list
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a BufferedWriter
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            //process each line in the list
            for (String line : lines) {
                String ipAddress = extractIpAddress(line);
                double averageDelay = computeAverageDelay(line, lines);

                writer.write(ipAddress + " " + averageDelay + "\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Helper function to extract IP address
    private static String extractIpAddress(String line) {
        //initalize index of the ()
        int startIndex = line.indexOf('(');
        int endIndex = line.indexOf(')', startIndex);
        //if there is () in the line, extract the substring between ()
        if (startIndex != -1 && endIndex != -1) {
            return line.substring(startIndex + 1, endIndex);
        }
        //else if there is no ip address, return a blank line
        return "";
    }

    //Helper function to calculate average delay
    private static double computeAverageDelay(String line, List<String> lines) {
        //slit the line by white space and store in an array of strings
        String[] tokens = line.split("\\s+");
        double sum = 0;
        int count = 0;

        //iterate through all the strings in the line and find the marker 'ms', the value for
        //the delay is the element before 'ms'
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.contains("ms")) {
                try {
                    //extract value and update sum and count
                    double value = Double.parseDouble(tokens[i - 1]);
                    sum += value;
                    count++;
                } catch (NumberFormatException ignored) {
                }
            } else {
                //if the token does not contain 'ms', check the next line for continuation
                String nextLine = getNextLine(lines, i);
                if (nextLine != null) {
                    line += " " + nextLine; // concatenate the next line to the current line
                }
            }
        }

        double average = count > 0 ? sum / count : 0;
        return average;
    }

    //Helper function to get next line
    private static String getNextLine(List<String> lines, int currentIndex) {
        //check if there is a next line
        if (currentIndex + 1 < lines.size()) {
            return lines.get(currentIndex + 1);
        }
        return null;
    }
}
