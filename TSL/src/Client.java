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
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;

public class Client {
    static BigInteger clientDHPrivateKey;
    static BigInteger sharedSecret;





    public static void main(String[] args) throws IOException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

        /*----------------------------------------------------------------------
                                        HANDSHAKE
         ----------------------------------------------------------------------*/
        //1. The client generates a nonce and a Diffie-Hellman private key, then sends the nonce to the server.
        BigInteger nonce = new BigInteger(new SecureRandom().generateSeed(32)); //tls content is 32 bytes

        //Message history
        ByteArrayOutputStream messageHistory = new ByteArrayOutputStream();

        //Establish a socket connection
        Socket clientSocket = new Socket("localhost", 8080);
        ObjectOutputStream clientOut = new ObjectOutputStream(clientSocket.getOutputStream());


        //sending message 1 of the handshake client -> server
        clientOut.writeObject(nonce);
        messageHistory.write(nonce.toByteArray());
        System.out.println("[CLIENT] sending HS-MSG(1)");




        /*3. a. Client reads in server-certificate, server-DH-public-key and signed server-DH-public-key from server
             b. The client verifies the server's certificate and signature
             c. Generates a shared secret
             d. Client sends its certificate, client-DH-public-key, and signed client-DH-public-key to the server.
             e. Compute session keys using shared secret.*/
        //(a.) Read-in
        ObjectInputStream clientIn = new ObjectInputStream(clientSocket.getInputStream());
        System.out.println("[CLIENT] reading HS-MSG(2)");
        Certificate serverCertificate = (Certificate) clientIn.readObject();
        BigInteger serverDHPublicKey = (BigInteger) clientIn.readObject();
        BigInteger signedServerDHPublicKey = (BigInteger) clientIn.readObject();


        //(b.) Verify
        Shared.verifyCertificate(serverCertificate);

        //Store sever->client messages in message history
        messageHistory.write(serverCertificate.toString().getBytes());
        messageHistory.write(serverDHPublicKey.toByteArray());
        messageHistory.write(signedServerDHPublicKey.toByteArray());


        //(c.) Generates shared secret
        clientDHPrivateKey = new BigInteger(new SecureRandom().generateSeed(32));
        sharedSecret = Shared.getSharedSecret(serverDHPublicKey, clientDHPrivateKey);
        System.out.println("[CLIENT] shared secret: " + sharedSecret);

        //(d.) Send client's certificate, DH public key, and signed DH public key

        //Generates clients Diffie-Hellman public key
        BigInteger clientDHPublicKey = Shared.getDHPublicKey(clientDHPrivateKey);

        //Generate signed clientDHPublicKey
        Certificate clientCertificate = Shared.getCertificate("../CASignedClientCertificate.pem");
        PublicKey clientRSAPublicKey = clientCertificate.getPublicKey();
        BigInteger signed_clientDHPublicKey = Shared.getSignedDHPublicKey("../clientPrivateKey.der", clientDHPublicKey, clientRSAPublicKey);

        //Send HS-MSG 3
        clientOut.writeObject(clientCertificate);
        clientOut.writeObject(clientDHPublicKey);
        clientOut.writeObject(signed_clientDHPublicKey);
        System.out.println("[CLIENT] sending HS-MSG(3)");


        //Add sent messages to messageHistory
        messageHistory.write(clientCertificate.toString().getBytes());
        messageHistory.write(clientDHPublicKey.toByteArray());
        messageHistory.write(signed_clientDHPublicKey.toByteArray());

        //Compute session keys using shared secret
        Shared.makeSecretKeys(nonce, sharedSecret);


        /**
         * 6. (a.) Receive 'server Handshake Finish Msg'
         *    (b.) Compare message server's mac with our mac, they should be the same
         *    (c.)If 'server Handshake Finish Msg' is valid, send 'client Handshake Finish Msg'
         */
        byte[] serverHandshakeFinishMsg = (byte[]) clientIn.readObject();
        Shared.checkForValidMAC(serverHandshakeFinishMsg, messageHistory.toByteArray(), Shared.serverMAC);

        //If the server's Mac message is valid, add it to messageHistory, then send 'client Handshake Finish Msg'
        messageHistory.write(serverHandshakeFinishMsg);
        System.out.println("[CLIENT] reading HS-MSG(5)");

        byte[] clientHandshakeFinishMsg = Shared.macMessage(messageHistory.toByteArray(), Shared.clientMAC);
        clientOut.writeObject(clientHandshakeFinishMsg);
        messageHistory.write(clientHandshakeFinishMsg);
        System.out.println("[CLIENT] sending HS-MSG(5)");

        System.out.println("[CLIENT] Handshake was successful. Good to proceed with post-handshake messaging");
        System.out.println("-------------------------------MESSAGING--------------------------------");

        /*----------------------------------------------------------------------
                                        MESSAGING
         ----------------------------------------------------------------------*/

        //RECEIVING MESSAGE 1 AND SENDING ACKNOWLEDGEMENT
        //Read in and decrypt message from server
        byte[] msgFromServer = (byte[]) clientIn.readObject();
        String decrptedMsgFromServer = Shared.decrypt(msgFromServer, Shared.serverEncrypt, Shared.serverInitVector, Shared.serverMAC);
        System.out.println("[CLIENT] Msg 1| Incoming message before decryption: " + msgFromServer);
        System.out.println("[CLIENT] Msg 1| Incoming message after decryption: " + decrptedMsgFromServer);

        //Send Acknowledgement message
        String ack = "ACK";
        byte[] encryptedACKMsg = Shared.encrypt(ack.getBytes(), Shared.clientEncrypt, Shared.clientInitVector, Shared.clientMAC);
        clientOut.writeObject(encryptedACKMsg);
        System.out.println("[CLIENT] Msg 1| Acknowledgement message before encryption: " + ack);
        System.out.println("[CLIENT] Msg 1| Acknowledgement message after encryption: " + encryptedACKMsg);

        //SENDING MESSAGE 2 AND RECEIVING ACKNOWLEDGEMENT
        //Sending message 2
        String msgFromClient = "Yeah, what do you want?";
        byte[] encryptedMsgFromClient = Shared.encrypt(msgFromClient.getBytes(), Shared.clientEncrypt, Shared.clientInitVector, Shared.clientMAC);
        clientOut.writeObject(encryptedMsgFromClient);
        System.out.println("[CLIENT] Msg 2| Outgoing message before encryption: " + msgFromClient);
        System.out.println("[CLIENT] Msg 2| Outgoing message after encryption: " + encryptedMsgFromClient);

        //Receive ack message from server
        byte[] acknowledgementFromServer = (byte[]) clientIn.readObject();

        //Decrypt message
        String decryptedAcknowledgementFromServer = Shared.decrypt(acknowledgementFromServer, Shared.serverEncrypt, Shared.serverInitVector, Shared.serverMAC);
        System.out.println("[CLIENT] Msg 2| Acknowledgement before decryption: " + acknowledgementFromServer);
        System.out.println("[CLIENT] Msg 2| Acknowledgement after decryption: " + decryptedAcknowledgementFromServer);

        //check that the message sent to the client is the servers "ACK"
        if (!decryptedAcknowledgementFromServer.equals("ACK")){
            throw new RuntimeException("ACK not received.");
        }

    }



}