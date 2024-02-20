

#include <iostream>
#include <array>
#include <vector>
#include <algorithm> // for std::shuffle
#include <random> // for std::default_random_engine
#include <chrono> // for std::chrono::system_clock

//------------------------NOTES-------------------------//
/*SUB TABLES
                 Encryption table (ET)
                 ~~~~~~~~~~~~~~~~
Index (Original)|0	|1	|2	|3	|4	|5	|6	|7	|8	|9
Encrypted Value	| 2	|5	|7	|9	|3	|0	|8	|4	|6	|1

-When building the DT, i.e. DT index 0, I go to ET and find the index at value 0, it's 5.

                 Decryption table (DT)
                 ~~~~~~~~~~~~~~~~
Index (Encrypted Value)|0  |1  |2  |3  |4  |5	|6	|7	|8	|9
Original Value     	   |5  |9  |0  |4  |7  |1	|8	|2	|6	|3

-During decryption, I see the value 0. In the decryption table index 0 is value 5. So sub 0 with the OG value 5.
 */


/*ENCRYPT BIT ROTATIONS

 Example: block with 2 bytes: 11010101 01101100

b.) Isolate and Shift Leftmost Bits to tempBlock:
        Byte 1: 00000001
        Byte 2: 00000000
c.) Shift message Left:
        Byte 1: 10101010
        Byte 2: 11011000
d.) Wrap the Leftmost Bit from Byte 1 to Byte 2:
        Byte 1: 10101010
        Byte 2: 11011001

 For an entire block, each byte's leftmost bit is stored in tempBlock,
all message bytes are shifted left, and tempBlock bits are then used to correctly adjust
the bits in message for the left rotation across the entire block.
*/

//--------------------END OF NOTES-------------------------//

//Alias declaration (a new name to an existing type)
using Block = std::array<uint8_t, 8>; // 64-bit (8 bytes) block
using Key = Block; // 64-bit key
using SubstitutionTable = std::vector<std::byte>; //sub table is a vector of bytes
using SubstitutionTables = std::vector<std::vector<std::byte>>; //vector of sub-tables
#define MAX_TABLE_SIZE 256  //a byte can represent 2^8 = 256 different values

//Function that generates a key based on the password
Key generateKey(const std::string& password) {
    //Initiate key- 8 bytes array filled w 0's
    Key key = {0, 0, 0, 0, 0, 0, 0, 0};
    //loop over each char in password
    for (size_t i = 0; i < password.length(); ++i) {
        //  key[i mod 8] = key[i mod 8] xor password[i]
        key[i % 8] ^= password[i];
    }
    return key;
}

// Helper function to randomly shuffles byte table using the Fisher-Yates shuffle algorithm
void shuffleTable(SubstitutionTable& table) {
    //Loop through the table in reverse order:
    for (int i = MAX_TABLE_SIZE -1; i > 0; i--) {
        //For each i from 255 down to 1, generate a random index j between 0 and i
        int j = rand() % (i + 1);
        //Swap the elements at indices i and j
        std::swap(table[i], table[j]);
    }
}

//Creates a decryption array that is used to map each encrypted byte back to its original byte value
// Used in reversing the encryption substitution process
//**note
SubstitutionTable createDecryptTable(const SubstitutionTable& encryptTable) {
    //initialize decryption array
    SubstitutionTable decryptTable(MAX_TABLE_SIZE);

    //Iterate over all 256 possible byte values.
    /*-i represents the original value and the index
      -Decrypted array goes to the index equal to the encrypted-value and sets its value to the original value (i)
      EX: index = 5
      the original byte 5 was encrypted to byte 200 (encryptTable[5] = 200), then for decryption, decryptTable[200] = 5
    */
    for (int i = 0; i < MAX_TABLE_SIZE; ++i) {
        //Get the encrypted value at index i
        int encryptedValue = static_cast<unsigned char>(encryptTable[i]); //needs to be cast to an integer to be used as an index
        decryptTable[encryptedValue] = static_cast<std::byte>(i);
    }
    return decryptTable;
}




// Function to create substitution tables
void createSubstitutionTables(SubstitutionTables& encryptionTables, SubstitutionTables& decryptionTables) {
    //Seed random number generator with current time
    srand(static_cast<unsigned>(time(nullptr)));

    // Initialize and fill the unshuffled table with 256 bytes (0-255)
    SubstitutionTable unshuffledTable(MAX_TABLE_SIZE);
    for (int i = 0; i < MAX_TABLE_SIZE; i++) {
        unshuffledTable[i] = static_cast<std::byte>(i);
    }

    encryptionTables.push_back(unshuffledTable); // First table (of 8 tables) is unshuffled
    decryptionTables.push_back(unshuffledTable); // Corresponding decryption table

    // Generate 7 shuffled substitution tables
    for (int i = 1; i <= 7; ++i) {
        SubstitutionTable shuffledTable = unshuffledTable;
        shuffleTable(shuffledTable);
        //Add encryption table (shuffled table) to list of encryption tables
        encryptionTables.push_back(shuffledTable);
        //Reverse the encryption table to create the decryption table, add the decryption table to list of decryption tables
        decryptionTables.push_back(createDecryptTable(shuffledTable));
    }
}

//Function to encrypt string message. Takes the message, list of encryption tables and a key as parameters.
Block encryptMessage(std::string stringMessage, SubstitutionTables& encryptionTables, Key key) {
    //initialize the state by converting message into a block (8 byte [])
    Block message;
    for (int i = 0; i < message.size(); ++i) {
        message[i] = stringMessage[i];
    }

    //encryption
    //Repeat for 16 rounds
    for (int i = 0; i < 16; i++) {
        //1.) XOR every byte of the message with the key
        for (int j = 0; j < message.size(); j++) {
            message[j] = message[j] ^ key[j];
        }
        //2.) substitution table
        //Iterate over each byte in message and set its value to the same index in the kth encryption table
        //i.e message byte 0 uses table 0
        for (int k = 0; k < message.size(); k++) {
            int encryptionTableIndex = message[k];
            message[k] = (char) encryptionTables[k][encryptionTableIndex];
        }
        //3.) rotation (bitwise); shift the whole state 1 bit to the left (wrap around)
        //**see note
        //a.) Copy the Block
        Block tempBlock = message;
        //b.) isolate the LMB and save it to tempBlock. Shift message bits left, clearing the LMB
        for (int l = 0; l < message.size(); ++l) {
            tempBlock[l] = (message[l] & 0x80) >> 7;
            message[l] = (message[l] & 0x7F) << 1;
        }
        //c.) Restore the LMB to the Last Byte
        message[7] = message[7] | tempBlock[0];

        //d.) Shift the LMB right across the message
        for (int m = 0; m < message.size() - 1; ++m) {
            message[m] = message[m] | tempBlock[m + 1];
        }
    }
    return message;

}
//Function to decrypt string message. Takes the message, list of decryption tables and a key as parameters.
void decryptMessage(Block& message, SubstitutionTables& decryptionTables, Key key) {
    for (int i = 0; i < 16; i++) {
        //decryption
        //1.) rotate state right(bitwise)- reverse of encryption
        Block tempBlockDecrypt = message;
        for (int j = 0; j < message.size(); ++j) {
            tempBlockDecrypt[j] = (message[j] & 0x01) << 7;
            message[j] = (message[j] >> 1) & 0x7F;

        }
        message[0] = message[0] | tempBlockDecrypt[7];
        for (int k = 1; k < message.size(); ++k) {
            message[k] = message[k] | tempBlockDecrypt[k - 1];
        }
        //2.) substitution table
        //Same as encryption but with the decryption table
        for (int l = 0; l < message.size(); l++) {
            message[l] = (char) decryptionTables[l][message[l]];
        }
        //3.) XOR every byte of the message with the key
        for (int m = 0; m < message.size(); m++) {
            message[m] = message[m] ^ key[m];
        }
    }

}


//Prints the contents of a Block
void printBlock(const Block message) {
    for (int i = 0; i < message.size(); ++i) {
        std::cout << message[i];
    }
    std::cout << std::endl;
}


int main() {
    //Generate key
    std::string password = "myneckmybacklickmypussyandmycrack";
    Key key = generateKey(password);

    //Create substitution tables for encryption and decryption (will hold 8 substitution tables)
    SubstitutionTables encryptionTables;
    SubstitutionTables decryptionTables;
    createSubstitutionTables(encryptionTables, decryptionTables);

    //Message to encrypt
    std::string stringMessage = "melanie!";
    std::cout << "Message before encryption: " << stringMessage << "\n";

    //Encrypt message
    Block message = encryptMessage(stringMessage, encryptionTables, key);
    std::cout << "Message after encryption: ";
    printBlock(message);

    //commented out line shows what happens when a single bit is flipped
    //message[5] = message[5] ^ 0x40;

    //Decrypt message
    decryptMessage(message, decryptionTables, key);
    std::cout << "Message after decryption: ";
    printBlock(message);
    return 0;
}