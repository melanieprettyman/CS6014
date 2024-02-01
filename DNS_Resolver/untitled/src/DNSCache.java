import java.util.HashMap;

public class DNSCache {
    private final HashMap<DNSQuestion, DNSRecord> cache;

    // Private constructor to prevent instantiation
    public DNSCache() {
        cache = new HashMap<>();
    }

// Method to query the cache for a DNSRecord based on a DNSQuestion
    public  DNSRecord query(DNSQuestion question) {

        // Check if the cache contains the question
        if(cache.containsKey(question)){
        // If it does, check if the record is expired
         DNSRecord record = cache.get(question);
            // If the record is expired, remove it from the cache and return "not found"
            if(record.isExpired()){
                 cache.remove(question);
                 return null;
             }
            // If the record is not expired, return the record
            else{
                return record;
            }
        }
        else {
            // If the cache does not contain the question, return "not found"
            return null;
        }
    }

    // Method to insert a DNSRecord into the cache based on a DNSQuestion
    public void insert(DNSQuestion question, DNSRecord answer) {
            cache.put(question, answer);

    }

}
