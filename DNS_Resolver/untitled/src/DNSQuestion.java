import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
/*
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                                               |
|                      QNAME                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                      QTYPE                    |
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
|                      QCLASS                   |
 +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
/**
 * Represents a DNS question in a DNS message.
 * A DNS question includes the domain name being queried (QNAME),
 * the type of query (QTYPE), and the class of the query (QCLASS).
 */
public class DNSQuestion {

    protected String[] qname; // Contains the domain name being queried ("example.com")
    protected int qtype; // Specifies the type of query (e.g., A for IPv4 address, AAAA for IPv6 address, MX for mail exchange)
    protected int qclass; // Specifies the class type of the query (e.g., IN for internet, CH for chaos)

    DNSMessage message_; // Reference to the DNS message containing this question

    /**
     * Default constructor for DNSQuestion.
     * Initializes fields with default values.
     */
    public DNSQuestion() {
        this.qname = new String[]{""};
        this.qtype = 0;
        this.qclass = 0;
    }


    /**
     * Decode a DNS question from the input stream.
     * Reads QNAME, QTYPE, and QCLASS fields from the input stream.
     * Due to compression, it may request the DNSMessage containing this question to read some fields.
     *
     * @param inputStream The input stream to read from.
     * @param dnsMessage  The DNS message containing this question.
     * @return The decoded DNS question.
     * @throws IOException If an I/O error occurs.
     */
    static DNSQuestion decodeQuestion(InputStream inputStream, DNSMessage dnsMessage) throws IOException {
        DNSQuestion question = new DNSQuestion();

        try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            question.qname = dnsMessage.readDomainName(inputStream);
            // QTYPE is two bytes, so read in two bytes
            question.qtype = dataInputStream.readShort();
            question.qclass = dataInputStream.readShort();

            return question;
        }
    }

    /**
     * Write the question bytes to be sent to the client.
     * Uses a hash map for compression (see the DNSMessage class).
     *
     * @param byteArrayOutputStream The output stream to write to.
     * @param domainNameLocations   A hash map containing domain name locations for compression pointers.
     * @throws IOException If an I/O error occurs.
     */
    // Called in main
    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String, Integer> domainNameLocations) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        DNSMessage.writeDomainName(byteArrayOutputStream, domainNameLocations, qname);

        // Write QTYPE (2 bytes)
        dataOutputStream.writeShort(qtype);

        // Write QCLASS (2 bytes)
        dataOutputStream.writeShort(qclass);
    }

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "qname=" + Arrays.toString(qname) +
                ", qtype=" + qtype +
                ", qclass=" + qclass +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return qtype == that.qtype &&
                qclass == that.qclass &&
                Arrays.equals(qname, that.qname);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(qtype, qclass);
        result = 31 * result + Arrays.hashCode(qname);
        return result;
    }
}




