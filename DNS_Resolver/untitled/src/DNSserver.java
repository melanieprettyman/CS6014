import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

public class DNSserver {
    static DNSCache cache = new DNSCache();
    static ArrayList<DNSRecord> answer;


    public static void main(String[] args) {
        try {

            // Create a UDP socket to listen for requests on port 53
            DatagramSocket socket = new DatagramSocket(8053);

            while (true) {
                //System.out.println("Running server ");
                // Receive incoming request
                byte[] requestData = new byte[512];
                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length);
                socket.receive(requestPacket);

                // Decode the DNS message
                DNSMessage requestMessage = DNSMessage.decodeMessage(requestPacket.getData());
                //System.out.println("MSG contents: " + requestMessage);
                //System.out.println("Received DNS request from: " + requestPacket.getAddress());


                //Check cache for question
                ArrayList<DNSQuestion> question =  requestMessage.getQuestions();
                DNSRecord cachedRecord = cache.query(question.get(0));
                System.out.println(cachedRecord);

                //If question is cache, set answer to cache answer
                if(cachedRecord != null) {
                    ArrayList<DNSRecord> temPAnswer = new ArrayList<>();
                    temPAnswer.add(cachedRecord);
                    answer = temPAnswer;

                    System.out.println("Found cached record for query: " + question);

                } else {
                    System.out.println("NOT FOUND in cached record for query: " + question);

                    // Forward request to Google DNS (8.8.8.8)
                    InetAddress googleDNS = InetAddress.getByName("8.8.8.8");
                    DatagramPacket forwardPacket = new DatagramPacket(requestData, requestData.length, googleDNS, 53);
                    socket.send(forwardPacket);
                    //System.out.println("forwardSocket.send(forwardPacket);\n");

                    // Receive response from Google DNS
                    byte[] responseFromGoogle = new byte[514];
                    DatagramPacket responsePacket = new DatagramPacket(responseFromGoogle, responseFromGoogle.length);
                    socket.receive(responsePacket);
                    //System.out.println("forwardSocket.receive(responsePacket);\n");

                    // Decode response from Google DNS
                    DNSMessage googleResponse = DNSMessage.decodeMessage(responsePacket.getData());
                    //System.out.println("DNSMessage googleResponse = DNSMessage.decodeMessage(responsePacket.getData());\n");
                    answer = googleResponse.getAnswers();
                    cache.insert(question.get(0), answer.get(0));
                    //System.out.println("cache.insert(question.get(0), answer.get(0));\n: " + question.get(0));

                }

                // Build response message
                DNSMessage responseMessage = DNSMessage.buildResponse(requestMessage, answer);
                //System.out.println("Response message: " + responseMessage);
                // Send response back to client
                byte[] responseData = responseMessage.toBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, requestPacket.getAddress(), requestPacket.getPort());
                socket.send(responsePacket);
                //System.out.println("Sent response to: " + requestPacket.getAddress());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




