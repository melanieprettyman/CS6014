import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//+---------------------
//|        Header       |
//+---------------------+
//|       Question      | the question for the name server
//+---------------------+
//|        Answer       | RRs answering the question
//+---------------------+
//|      Authority      | RRs pointing toward an authority
//+---------------------+
//|      Additional     | RRs holding additional information
//+---------------------+
// Class representing a DNS message
public class DNSMessage {
    // DNS message components
    private DNSHeader header; // Header information
    private ArrayList<DNSQuestion> questions; // List of questions
    private ArrayList<DNSRecord> answers; // List of answers
    private ArrayList<DNSRecord> authorityRecords; // List of authority records
    private ArrayList<DNSRecord> additionalRecords; // List of additional records
    private byte[] messageBytes; // Byte array containing the complete message

    // Constructor to initialize DNSMessage
    public DNSMessage() {
        // Initialize message components
        this.header = new DNSHeader();
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();
        this.authorityRecords = new ArrayList<>();
        this.additionalRecords = new ArrayList<>();
        this.messageBytes = null;
    }

    // Method to decode a DNS message from byte array
    public static DNSMessage decodeMessage(byte[] bytes) throws IOException {

        DNSMessage dnsMessage = new DNSMessage();
        dnsMessage.messageBytes = Arrays.copyOf(bytes, bytes.length);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            // Decode DNS header
            dnsMessage.header = DNSHeader.decodeHeader(byteArrayInputStream);

            // Read questions
            readQuestions(byteArrayInputStream, dnsMessage);

            // Read answers
            readRecords(byteArrayInputStream, dnsMessage.answers, dnsMessage.header.getAnCount(), dnsMessage);

            // Read authority records
            readRecords(byteArrayInputStream, dnsMessage.authorityRecords, dnsMessage.header.getNsCount(), dnsMessage);

            // Read additional records
            readRecords(byteArrayInputStream, dnsMessage.additionalRecords, dnsMessage.header.getAdCount(), dnsMessage);


        }

        return dnsMessage;
    }

    // Helper method to read questions from input stream
    private static void readQuestions(ByteArrayInputStream byteArrayInputStream, DNSMessage dnsMessage) throws IOException {
        int questionCount = dnsMessage.header.getQdCount();
        for (int i = 0; i < questionCount; i++) {
            DNSQuestion question = DNSQuestion.decodeQuestion(byteArrayInputStream, dnsMessage);
            dnsMessage.questions.add(question);
        }
    }

    // Helper method to read records from input stream
    private static void readRecords(ByteArrayInputStream byteArrayInputStream, ArrayList<DNSRecord> records, int count, DNSMessage dnsMessage) throws IOException {
        for (int i = 0; i < count; i++) {
            DNSRecord record = DNSRecord.decodeRecord(byteArrayInputStream, dnsMessage);
            records.add(record);
        }
    }

    // Method to read the pieces of a domain name from input stream
    // If the name is empty, return an empty list
    public String[] readDomainName(InputStream inputStream) throws IOException {
        ArrayList<String> domainNameParts = new ArrayList<>();
        int currentByte = inputStream.read();

        // If the first byte is 0, indicating an empty name, return an empty list
        if (currentByte == 0) {
            return new String[0];
        }

        // Read the bytes until the length is not 0 (indicating the end of QNAME)
        while (currentByte > 0) {
            // Initialize a byte array to store the label bytes
            byte[] labelBytes = new byte[currentByte];
            // Read the label bytes from the input stream
            inputStream.read(labelBytes, 0, labelBytes.length);
            // Convert the byte array to a string and append it to the domain name parts list
            String label = new String(labelBytes, StandardCharsets.UTF_8);
            domainNameParts.add(label);
            currentByte = inputStream.read();
        }

        // Convert the ArrayList to a String array and return
        return domainNameParts.toArray(new String[0]);
    }



    // Method to read the pieces of a domain name from a specified byte position
    public String[] readDomainName(int firstByte) throws IOException {
        // Create a ByteArrayInputStream starting at the specified byte position
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(messageBytes, firstByte, messageBytes.length - firstByte);
        // Call the existing readDomainName(InputStream) method passing the created ByteArrayInputStream
        return readDomainName(byteArrayInputStream);

    }

    // Method to build a response DNS message
    public static DNSMessage buildResponse(DNSMessage request, ArrayList<DNSRecord> answers) {
        // Create a new DNSMessage for the response
        DNSMessage response = new DNSMessage();

        // Build the header for the response
        response.header = DNSHeader.buildHeaderForResponse(request);

        // Set the number of questions, answers, authorities, and additional records
        response.questions = request.getQuestions();
        response.answers = answers;
        response.authorityRecords = request.getAuthorityRecords();
        response.additionalRecords = request.getAdditionalRecords();

        return response;
    }

    // Method to convert DNSMessage to byte array
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Initialize domainLocations hashmap
        HashMap<String, Integer> domainLocations = new HashMap<>();

        // Write header, questions, answers, authority records, and additional records
        header.writeBytes(byteArrayOutputStream);

        for (DNSQuestion question : questions) {
            question.writeBytes(byteArrayOutputStream, domainLocations);
        }
        //Write answer
        answers.get(0).writeBytes(byteArrayOutputStream, domainLocations);

        for (DNSRecord ar : authorityRecords) {
            ar.writeBytes(byteArrayOutputStream, domainLocations);
        }

        for (DNSRecord ad : additionalRecords) {
            ad.writeBytes(byteArrayOutputStream, domainLocations);
        }

        // Convert ByteArrayOutputStream to byte array
        return byteArrayOutputStream.toByteArray();
    }

    // Method to write a domain name to ByteArrayOutputStream
    public static void writeDomainName(ByteArrayOutputStream outputStream, HashMap<String, Integer> domainLocations, String[] domainPieces) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        // Join domain name pieces
        String domainName = joinDomainName(domainPieces);

        // If domain name is already in domainLocations, write pointer
        if (domainLocations.containsKey(domainName)) {
            int location = domainLocations.get(domainName);
            location |= 0xC000;
            // Set the pointer flag and write the pointer (2 bytes)
            dataOutputStream.writeShort(location);
        } else {
            // Else, write domain name labels and update domainLocations
            Integer location = outputStream.size();
            domainLocations.put(domainName, location);

            // Write domain name labels
            for (String label : domainPieces) {
                // Write label length (1 byte)
                dataOutputStream.writeByte(label.length());
                // Write label bytes
                dataOutputStream.writeBytes(label);
            }
            // Write null byte to terminate QNAME
            dataOutputStream.writeByte(0);

        }

    }

    // Method to join the pieces of a domain name with dots
    public static String joinDomainName(String[] pieces) {
        // Join domain name pieces with dots
        return String.join(".", pieces);
    }


    // Getters and setters
    public DNSHeader getHeader() {
        return header;
    }

    public ArrayList<DNSQuestion> getQuestions() {
        return questions;
    }

    public ArrayList<DNSRecord> getAnswers() {
        return answers;
    }

    public ArrayList<DNSRecord> getAuthorityRecords() {
        return authorityRecords;
    }

    public ArrayList<DNSRecord> getAdditionalRecords() {
        return additionalRecords;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }


    @Override
    public String toString() {
        return "\nDNSMessage{" +
                "\nheader=" + header +
                "\n, questions=" + questions +
                "\n, answers=" + answers +
                "\n, authorityRecords=" + authorityRecords +
                "\n, additionalRecords=" + additionalRecords +
                '}';
    }
}
