//////////////////////////////////////////////////////////////////////////////
// Melanie Prettyman
// CS6014
// Spring 2024
//
// About: TSL Project is a simplified version of the TLS protocol
// How to use: Open two separate terminals and cd into TSL src folder.
//             Compile files using 'javac Server.java Client.java'.
//             Run 'java Client' in one terminal and 'java Server' in the other.
//             Server - client handshake and messages will print in the terminal.
//////////////////////////////////////////////////////////////////////////////
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class Shared {


    //DIFFIE-HELLMAN SHARED PARAMS
    //n is made from a base-16 safe prime number 1536 (from https://www.ietf.org/rfc/rfc3526.txt)
    static private final String safePrime_1536 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";
    static public BigInteger n = new BigInteger(safePrime_1536, 16);
    static public BigInteger g = new BigInteger("2"); //generator is 2 based on doc

    static Certificate _CACertificate = Shared.getCertificate("../CAcertificate.pem");

    //SESSION KEYS
    static byte[] serverEncrypt;
    static byte[] clientEncrypt;
    static byte[] serverMAC;
    static byte[] clientMAC;
    static byte[] serverInitVector;
    static byte[] clientInitVector;


    //Generate a public key from Certificate
    static public Certificate getCertificate(String filepath) {
        try {
            //Get public key from passed in file path
            FileInputStream fileInput = new FileInputStream(filepath);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            //Certificate
            return certificateFactory.generateCertificate(fileInput);

        }catch (FileNotFoundException | CertificateException e){
            throw new RuntimeException(e);
        }
    }

    //Generate DH-public key from DH shared params and DH-private key
    public static BigInteger getDHPublicKey(BigInteger _DHprivateKey) {
        return g.modPow(_DHprivateKey, n);
    }

    //Generate signed DH-public key from privateKet.der file, DH-public key and RSA-public key
    static public BigInteger getSignedDHPublicKey(String privateKeyDer_file, BigInteger _DHpublicKey, PublicKey _RSAPublicKey){
        try {
            //Read in privateKeyDer file, extract the private keys bytes and encode it
            FileInputStream privateKeyDer_file_input = new FileInputStream(privateKeyDer_file);
            byte[] privateKeyBytes = privateKeyDer_file_input.readAllBytes();
            PKCS8EncodedKeySpec encodedPrivateKeyBytes = new PKCS8EncodedKeySpec(privateKeyBytes);

            //Generate private-RSA-key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateRSAKey =  keyFactory.generatePrivate(encodedPrivateKeyBytes);

            //Sign with private key
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateRSAKey);//private key is used to generate the signature, ensuring that only the holder of the private key can sign the data
            signature.update(_DHpublicKey.toByteArray());// adds the data to be signed by the signature object
            byte[] signedPublicKey = signature.sign(); //computes the signature of the provided data (the DH public key) using the private RSA key

            //Verification and update
            signature.initVerify(_RSAPublicKey); //re-initialize Signature. Public key is used to verify that the signature was generated by the matching private key
            signature.update(_DHpublicKey.toByteArray()); //original data (DH public key) to compute its own signature for comparison

            return new BigInteger(signedPublicKey);

        }catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException |
                SignatureException e){
            throw new RuntimeException(e);
        }
    }


    public static void verifyCertificate(Certificate certificate) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        PublicKey _CApublicKey = Shared._CACertificate.getPublicKey(); //CA's public key (used to sign other certificates and verify the signatures on certificates issued by the CA)
        certificate.verify(_CApublicKey);
    }

    //Generate shared secret
    //client: serverPublicDHKey and clientPrivateDHKey
    //server: clientDHpublicKey and serverPrivateDHKey
    public static BigInteger getSharedSecret(BigInteger _publicDHKey, BigInteger _privateDHKey) {
        return _publicDHKey.modPow(_privateDHKey, n);
    }

    //Key derivation function (KDF) hdkf. Uses the HmacSHA256 Hashing MAC (HMAC) function.
    private static byte[] hdkfExpand(byte[] masterKey, String tag) {
        try {
            //Create mac with master key
            Mac HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(masterKey, "HmacSHA256");
            HMAC.init(secretKeySpec);

            //tag concatenated with a byte with value 1
            HMAC.update(tag.getBytes());
            HMAC.update((byte) 0x01);

            //compute the HMAC and return first 16 bytes of HMAC
            return Arrays.copyOf(HMAC.doFinal(), 16);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    //Generate session keys
    //Each key is the result of hashing one of the other keys and adding in an extra "tag"
    // to make sure they can't accidentally be mistaken for one another.
    public static void makeSecretKeys(BigInteger clientNonce, BigInteger sharedSecretFromDiffieHellman) {
        try {
            //Create mac with nonce to start the HMAC sequence
            Mac HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(clientNonce.toByteArray(), "HmacSHA256");
            HMAC.init(secretKeySpec);

            //Add shared secret and computer mac (pkr)
            HMAC.update(sharedSecretFromDiffieHellman.toByteArray());
            byte[] prk = HMAC.doFinal();

            //Use hdkf key derivation function to create session keys
            serverEncrypt = hdkfExpand(prk, "server encrypt");
            clientEncrypt = hdkfExpand(serverEncrypt, "client encrypt");
            serverMAC = hdkfExpand(clientEncrypt, "server MAC");
            clientMAC = hdkfExpand(serverMAC, "client MAC");
            serverInitVector = hdkfExpand(clientMAC, "server IV");
            clientInitVector = hdkfExpand(serverInitVector, "client IV");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    //Calculates a Message Authentication Code (MAC)
    public static byte[] macMessage(byte[] message, byte[] macKey) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        Mac HMAC = Mac.getInstance("HmacSHA256");

        //Generate a new key from macKey and initializes the Mac instance with the new key
        SecretKeySpec secretKeySpec = new SecretKeySpec(macKey, "HmacSHA256");
        HMAC.init(secretKeySpec);

        //initializes HMAC with messageHisotry and computes the MAC
        HMAC.update(message);
        return HMAC.doFinal();

    }

    // Compare message sender's mac with receivers mac, they should be the same
    public static void checkForValidMAC(byte[] sendersMacMsg, byte[] myMessageHistory, byte[] macKey) throws NoSuchAlgorithmException, IOException, InvalidKeyException {
        byte[] myMacMsg = Shared.macMessage(myMessageHistory, macKey);
        if (!Arrays.equals(myMacMsg, sendersMacMsg)){
            throw new RuntimeException("Message MACS mismatch");
        }

    }

    public static Cipher createCipher( Boolean isEncrptCipher, byte[] encryptKey, byte[] initializationVector) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        //Create an AES/CBC/PKCS5Padding cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        //Initialize a SecretKeySpec with encryptKey for AES encryption
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");

        //Initialize an IvParameterSpec with the initializationVector key
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);

        //Check if this cipher is for encrytion or decrytion
        if(isEncrptCipher){
            //Initialize the cipher in ENCRYPT_MODE with the SecretKeySpec and IvParameterSpec
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec); //decrpt mode
        }
        else{
            //Initialize the cipher in DECRYPT_MODE with the SecretKeySpec and IvParameterSpec
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec); //decrpt mode
        }

        return cipher;
    }

    public static byte[] encrypt(byte[] message, byte[] encryptKey, byte[] initializationVector, byte[] macKey) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //Initialize message to byte array output stream
        ByteArrayOutputStream encryptedMessage = new ByteArrayOutputStream();
        encryptedMessage.write(message);

        //Generate a MAC for the message using macKey and append this MAC to the end of msg_byteArrayOutputStream
        byte[] macMsg = macMessage(message, macKey);
        encryptedMessage.write(macMsg);

        //Create a AES/CBC/PKCS5Padding cipher for encryption with encryptKey and initializationVector
        Cipher cipher = createCipher(true, encryptKey, initializationVector);

        //Using the cipher, encrypt the data in encryptedMessage
        return cipher.doFinal(encryptedMessage.toByteArray());

    }

    public static String decrypt(byte[] cipherText, byte[] encryptKey, byte[] initializationVector, byte[] macKey) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //Create an AES/CBC/PKCS5Padding cipher for decryption with encryptKey and initializationVector
        Cipher cipher = createCipher(false, encryptKey, initializationVector);

        //Decrypt the cipherText using the decryption cipher to get plainText
        byte[] plainText = cipher.doFinal(cipherText);

        //Separate the decrypted data into the original message (w.o MAC)
        byte[] decryptedMsg = Arrays.copyOf(plainText, plainText.length - 32);

        return new String(decryptedMsg, StandardCharsets.UTF_8);

    }
}
