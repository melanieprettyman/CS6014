#include <iostream>
#include "RC4.h"

using Stream = std::vector<uint8_t>;
using Key = std::string;


Stream encryptDecryptRC4(const Stream & input, const Key & key) {
    //Generate KSA with rc4

    RC4 rc4(key);

    Stream output(input.size());

    //XOR the keystream bytes with the plaintext bytes to encrypt or with the ciphertext bytes to decrypt
    for (size_t i = 0; i < input.size(); ++i) {
        output[i] = input[i] ^ rc4.getNextByte();
    }

    return output; //return plain or cipher text
}
void demonstrateRC4Works() {
    // Example message and key
    std::string messageStr = "park that big mac truck right in this little garage";
    std::cout << "Message before encryption: " << messageStr;
    Stream message(messageStr.begin(), messageStr.end());
    Key key = "cardiB";

    //encrypt the message
    Stream encryptedMessage = encryptDecryptRC4(message, key);
    std::cout << "Encrypted Message: ";
    for (auto b : encryptedMessage) std::cout << std::hex << +b << " ";
    std::cout << "\n";

    //decrypt the message
    Stream decryptedMessage = encryptDecryptRC4(encryptedMessage, key);
    std::cout << "Decrypted Message: ";
    for (auto b : decryptedMessage) std::cout << (b);
    std::cout << "\n";

}
void demonstrateDecryptionWithDifferentKeys() {
    const Stream originalMessage = {'W', 'A', 'P'};
    const Key keyForEncryption = "cardiB";
    const Key keyForDecryption = "fardiC";

    Stream encryptedMessage = encryptDecryptRC4(originalMessage, keyForEncryption);
    Stream decryptedWithDifferentKey = encryptDecryptRC4(encryptedMessage, keyForDecryption);

    std::cout << "Decrypted with different key: ";
    for (auto ch : decryptedWithDifferentKey) {
        std::cout << ch << " ";
    }
    std::cout << std::endl;
}

void demonstrateKeystreamReuseVulnerability() {
    const Stream message1 = {'A', 'B', 'C'};
    const Stream message2 = {'X', 'Y', 'Z'};
    Key key = "password";

    Stream encryptedMessage1 = encryptDecryptRC4(message1, key);
    Stream encryptedMessage2 = encryptDecryptRC4(message2, key);

    Stream xorOfEncryptedMessages(message1.size());
    std::cout << "XOR of two encrypted messages: ";
    for (size_t i = 0; i < message1.size(); ++i) {
        xorOfEncryptedMessages[i] = encryptedMessage1[i] ^ encryptedMessage2[i];
        std::cout << std::hex << +xorOfEncryptedMessages[i] << " ";
    }
    std::cout << std::endl;
}
void performBitFlippingAttack() {
    std::string originalMessage = "Your salary is $1000";
    Key key = "grandTheftAuto";
    Stream encryptedMessage = encryptDecryptRC4(Stream(originalMessage.begin(), originalMessage.end()), key);

    //Attacker knows the position of "1000" in the message and want to change it to "9999"
    Stream attackStream = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x08, 0x09, 0x09, 0x9};
    for (int i = 0; i < encryptedMessage.size(); ++i) {
        encryptedMessage[i] = encryptedMessage[i] ^ attackStream[i];
    }

    Stream decryptedMessage = encryptDecryptRC4(encryptedMessage, key);
    std::cout << "After bit-flipping attack: ";
    for (auto ch : decryptedMessage) {
        std::cout << ch;
    }
    std::cout << std::endl;
}



int main() {
    demonstrateRC4Works();
    demonstrateDecryptionWithDifferentKeys();
    demonstrateKeystreamReuseVulnerability();
    performBitFlippingAttack();
    return 0;
}
