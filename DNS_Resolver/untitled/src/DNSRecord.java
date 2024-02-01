import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+|
//|                  NAME                           |  unknown size
//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
//|                  TYPE                           |  2 BYTE
//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+|
//|                  CLASS                          |  2 BYTE
//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+|
//|                  TTL                            |
//|                                                 |  4 BYTE
//| +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
//|                 RDLENGTH                        |  2 BYTE
//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+-- |
//|                  RDATA                          |  unknown size
//|+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+|
public class DNSRecord {
    private String[] name; //A variable-length field containing a domain name.
    private int type; //A 16-bit integer specifying the type of resource record.
    private int rclass; //A 16-bit integer specifying the class of the resource record.
    private int ttl; //A 32-bit unsigned integer specifying the time interval that the resource record may be cached.
    private int rdLength; //A 16-bit integer specifying the length of the RDATA field.
    private byte[] rdata; //A variable-length field containing data specific to the resource record type.
    private Date creationDate;


    /**
     * Decodes a DNS record from the input stream.
     *
     * @param inputStream The input stream to read from.
     * @param dnsMessage  The DNS message to which the record belongs.
     * @return The decoded DNS record.
     * @throws RuntimeException If an I/O error occurs.
     */
    static DNSRecord decodeRecord(InputStream inputStream, DNSMessage dnsMessage){
        DNSRecord record = new DNSRecord();
        // Set creation date
        record.creationDate = new Date();


        try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {

            inputStream.mark(2); // bookmarks our place in stream

            int firstByte = dataInputStream.readShort();
            // Check if there's a pointer to a prior occurrence
            if (ispointerToPriorOccurrence(firstByte)) {
                //int pointer = (firstByte & 0x3F) | (dataInputStream.readByte() & 0xFF);
                int pointer= firstByte & (short) 0x3FFF ;
                record.name = dnsMessage.readDomainName(pointer);
            } else {
                // reset
                inputStream.reset();
                // Read name, type, class, and TTL
                record.name = dnsMessage.readDomainName(inputStream);
            }
                record.type = dataInputStream.readShort();
                record.rclass = dataInputStream.readShort();
                record.ttl = dataInputStream.readInt();

                // Read RDLENGTH
                record.rdLength = dataInputStream.readShort(); //This field contains the length of the RDATA field

                // Read RDATA
                record.rdata = new byte[record.rdLength];
                dataInputStream.readFully(record.rdata);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return record;
    }
    // Method to check if the label length indicates a pointer to a prior occurrence
    static Boolean ispointerToPriorOccurrence(int labelLength) {
        return (labelLength & 0xC000) == 0xC000;
    }

    /**
     * Writes the DNS record data to the output stream.
     * @param outputStream The ByteArrayOutputStream to write the record data.
     * @param domainNameLocations A hashmap containing domain name locations for compression pointers.
     * @throws IOException If an I/O error occurs.
     */
    public void writeBytes(ByteArrayOutputStream outputStream, HashMap<String, Integer> domainNameLocations) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        // Write domain name bytes
        DNSMessage.writeDomainName(outputStream, domainNameLocations, name);

        // Write type (2 bytes)
        dataOutputStream.writeShort(type);
        // Write class (2 bytes)
        dataOutputStream.writeShort(rclass);
        // Write time to live (4 bytes)
        dataOutputStream.writeInt(ttl);
        // Write length of the RDATA field (2 bytes)
        dataOutputStream.writeShort(rdLength);
        // Write RDATA field
        if(rdLength > 0) {
            dataOutputStream.write(rdata);
        }
    }


    /**
     * Checks whether this DNS record has expired based on its TTL (Time to Live).
     * @return true if the record has expired, false otherwise.
     */
    public boolean isExpired() {
        // Get the current date and time
        Date now = new Date();
        // Get the current time in milliseconds since the epoch
        long currentTimeMillis = now.getTime();
        // Calculate the expiration time in milliseconds by adding the TTL (which is in secs) to the creation date
        long expirationTimeMillis = creationDate.getTime() + (ttl * 1000);
        // Compare the current time to the expiration time to determine if the record has expired
        return currentTimeMillis > expirationTimeMillis;
    }
    @Override
    public String toString() {
        return "DNSRecord{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", rclass=" + rclass +
                ", ttl=" + ttl +
                ", rdLength=" + rdLength +
                ", rdata=" + new String(rdata) +
                ", creationDate=" + creationDate +
                '}';
    }

}
