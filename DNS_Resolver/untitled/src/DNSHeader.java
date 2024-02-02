import java.io.*;
import java.lang.reflect.Field;
import java.util.Objects;

/*      +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
         15                       7                    0
        |                      ID                       |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
         7            3  2  1  0 7            3        0
        |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        15                                             0
        |                    QDCOUNT                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        15                                             0
        |                    ANCOUNT                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        15                                             0
        |                    NSCOUNT                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        15                                             0
        |                    ARCOUNT                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

 */
public class DNSHeader {

    private int id; //dns query, matches to a response
    protected Short flags;
    protected int qr; //tells if msg is a query(0) or a response(1)
    protected int opCode; //tells the kind of query in msg; standard(0), inverse(1), server status(2)...
    protected int aa; //is responding DNS server authoritative
    protected int tc; //is msg truncated (too big to send in 1 packet)
    protected int rd; //recursive?
    protected int ra; //does responding DNS server support queries?
    protected int z; // always 0

    protected int rCode; //tells result of query; NoError(0), FormatError(1), ServerFailure(2)...
    private int qdCount; //# of entries in the QUESTION section
    private int anCount; //# of entries in the ANSWER section
    private int nsCount; //# of entries in the AUTHORITY section
    private int adCount; //# of entries in the ADDITIONAL section

    /**
     * Private constructor to prevent instantiation outside the class
     */
    protected DNSHeader() {
        //set default header data to 0
        this.id = 0;
        this.flags = 0;
        this.qr =0;
        this.opCode = 0;
        this.aa =0;
        this.tc = 0;
        this.rd = 0;
        this.ra = 0;
        this.z = 0;
        this.rCode = 0;
        this.qdCount = 0;
        this.anCount = 0;
        this.nsCount = 0;
        this.adCount = 0;
    }
    /**
     * Decodes the header from a byte array input stream.
     *
     * @param byteArrayInputStream The byte array input stream containing the DNS message.
     * @return The decoded DNSHeader object.
     * @throws IOException If an I/O error occurs.
     */
    public static DNSHeader decodeHeader(ByteArrayInputStream byteArrayInputStream) throws IOException {
       //Initialize header object
        DNSHeader header = new DNSHeader();
        //Read in header and store it's data to header obj
        try (DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            // Read 2 bytes for ID using readShort()
            header.id = dataInputStream.readShort();

            // Read 2 bytes for flags, then parse flags
            header.flags = dataInputStream.readShort();
            parseFlags(header.flags, header);

            // Read 2 bytes for qdcount
            header.qdCount = dataInputStream.readShort();

            // Read 2 bytes for ancount
            header.anCount = dataInputStream.readShort();

            // Read 2 bytes for nscount
            header.nsCount = dataInputStream.readShort();

            // Read 2 bytes for arcount
            header.adCount = dataInputStream.readShort();
        }

        return header;
    }

    /**
     * Parses the flags from a short value and sets the header fields accordingly.
     *
     * @param flags The flags value to parse.
     * @param header The DNSHeader object to update.
     */
    public static void parseFlags(Short flags, DNSHeader header) {
        //BYTE 4
        // qr flag -> bit 7
        header.qr = (flags >> 15) & 0x1; //moves the bit at position 15 to the rightmost position (bit 0) then bitwise AND with 0x1 (binary 00000001) ~extracting the rightmost bit

        // opcode flag -> bit 6-3
        header.opCode = (flags >> 11) & 0xF; //moves bits at positions 11-15 to the rightmost positions (bits 0-4) then bitwise AND with 0xF (binary 00001111)

        // aa flag -> bit 2
        header.aa = (flags >> 10) & 0x1;

        // tc flag -> bit 1
        header.tc = (flags >> 9) & 0x1;

        // rd flag -> bit 0
        header.rd = (flags >> 8) & 0x1;

        //BYTE 3
        // ra flag -> bit 7
        header.ra = (flags >> 7) & 0x1;

        // rcode flag -> bit 3-0
        header.rCode = flags & 0xF;
    }

    /**
     * Builds a DNSHeader for the response based on the request's header.
     *
     * @param request The DNSMessage object representing the request.
     * @return The DNSHeader object for the response.
     */
    public static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) {
        DNSHeader requestHeader = request.getHeader();

        // Create a new header using the request's header as a template
        DNSHeader responseHeader = new DNSHeader();
        responseHeader.id = (requestHeader.getId());
        responseHeader.flags = (requestHeader.getFlags());
        responseHeader.qdCount = (requestHeader.getQdCount());
        responseHeader.nsCount = (requestHeader.getNsCount());
        responseHeader.adCount = (requestHeader.getAdCount());

        // Set QR flag to indicate a response
        responseHeader.qr = 1;

        // Set ANCOUNT to 1 if answer is good (otherwise so not send an
        if (response.getAnswerCount() > 0) {

            responseHeader.anCount = 1;
        }

        return responseHeader;
    }


    /**
     * Encodes the header to bytes for sending back to the client.
     *
     * @param outputStream The output stream to write the header bytes.
     * @throws IOException If an I/O error occurs.
     */
    public void writeBytes(ByteArrayOutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        // Write 2 bytes for ID
        dataOutputStream.writeShort(id);

        // Write 2 bytes for flags
        flags = convertFlagsToShort();
        dataOutputStream.writeShort(flags);

        // Write 2 bytes for qdcount
        dataOutputStream.writeShort(qdCount);

        // Write 2 bytes for ancount
        dataOutputStream.writeShort(anCount);

        // Write 2 bytes for nscount
        dataOutputStream.writeShort(nsCount);

        // Write 2 bytes for arcount
        dataOutputStream.writeShort(adCount);

    }
    /**
     * Helper method to convert flag int into two bytes (short)
     */
    public short convertFlagsToShort() {
        short result = 0;

        // BYTE 4
        // qr flag -> bit 7
        result |= (short) ((qr & 0x1) << 15);

        // opcode flag -> bit 6-3
        result |= (short) ((opCode & 0xF) << 11);

        // aa flag -> bit 2
        result |= (short) ((aa & 0x1) << 10);

        // tc flag -> bit 1
        result |= (short) ((tc & 0x1) << 9);

        // rd flag -> bit 0
        result |= (short) ((rd & 0x1) << 8);

        // BYTE 3
        // ra flag -> bit 7
        result |= (short) ((ra & 0x1) << 7);

        // Z bits -> bits 6-4
        result |= (short) ((z & 0x7) << 4);

        // rcode flag -> bit 3-0
        result |= (short) (rCode & 0xF);

        return result;
    }





    //Return a human-readable string version of a header object.
    @Override
    public String toString() {
        return "DNSHeader{" +
                "id=" + id +
                ", flags=" + flags +
                ", qdcount=" + qdCount +
                ", ancount=" + anCount +
                ", nscount=" + nsCount +
                ", arcount=" + adCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSHeader dnsHeader = (DNSHeader) o;
        return id == dnsHeader.id && qr == dnsHeader.qr && opCode == dnsHeader.opCode && aa == dnsHeader.aa && tc == dnsHeader.tc && rd == dnsHeader.rd && ra == dnsHeader.ra && z == dnsHeader.z && rCode == dnsHeader.rCode && qdCount == dnsHeader.qdCount && anCount == dnsHeader.anCount && nsCount == dnsHeader.nsCount && adCount == dnsHeader.adCount && Objects.equals(flags, dnsHeader.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, flags, qr, opCode, aa, tc, rd, ra, z, rCode, qdCount, anCount, nsCount, adCount);
    }
//Getter Functions

    public int getId() {
        return id;
    }

    public short getFlags() {
        return flags;
    }

    public int getQr() {
        return qr;
    }

    public int getOpCode() {
        return opCode;
    }

    public int getAa() {
        return aa;
    }

    public int getTc() {
        return tc;
    }

    public int getRd() {
        return rd;
    }

    public int getRa() {
        return ra;
    }

    public int getRCode() {
        return rCode;
    }
    public int getZ() {
        return z;
    }
    public int getQdCount() {
        return qdCount;
    }

    public int getAnCount() {
        return anCount;
    }

    public int getNsCount() {
        return nsCount;
    }

    public int getAdCount() {
        return adCount;
    }


}




