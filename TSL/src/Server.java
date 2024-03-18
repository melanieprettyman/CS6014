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
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class Server {
    static BigInteger serverDHPrivateKey;
    static BigInteger sharedSecret;


    public static void main(String[] args) throws IOException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        //Establish up Server connection
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Server started and waiting for connections...");
        Socket socket = serverSocket.accept();
        ObjectInputStream serverIn = new ObjectInputStream(socket.getInputStream());

        //HANDSHAKE
        /*2. a. The server receives the nonce
             b. Generate server Diffie-Hellman private key
             c. Server sends back its certificate, Diffie-Hellman public key, and the signed Diffie-Hellman public key.*/

        //Message history
        ByteArrayOutputStream messageHistory = new ByteArrayOutputStream();

        //(a.) Receive message 1 from client
        BigInteger nonce = (BigInteger) serverIn.readObject();
        messageHistory.write(nonce.toByteArray());
        System.out.println("[SERVER] reading HS-MSG(1)");

        //(b.)Generates server's Diffie-Hellman private key
        serverDHPrivateKey = new BigInteger(new SecureRandom().generateSeed(32));

        //(c.) Send server certificate, server-DH-public-key and signed server-DH-public-key
        ObjectOutputStream serverOut = new ObjectOutputStream(socket.getOutputStream());

        //server DH-public key
        BigInteger serverDHPublicKey = Shared.getDHPublicKey(serverDHPrivateKey);

        //Generate server certificate
        Certificate serverCertificate = Shared.getCertificate("../CASignedServerCertificate.pem");

        //Generate signed serverDHPublicKey
        PublicKey serverRSAPublicKey = serverCertificate.getPublicKey();
        BigInteger signed_serverDHPublicKey = Shared.getSignedDHPublicKey("../serverPrivateKey.der", serverDHPublicKey, serverRSAPublicKey);

        System.out.println("[SERVER] sending HS-MSG(2)");
        serverOut.writeObject(serverCertificate);
        serverOut.writeObject(serverDHPublicKey);
        serverOut.writeObject(signed_serverDHPublicKey);


        //Add sent messages to messageHistory
        messageHistory.write(serverCertificate.toString().getBytes());
        messageHistory.write(serverDHPublicKey.toByteArray());
        messageHistory.write(signed_serverDHPublicKey.toByteArray());


        /* 4. a. Server reads in client-certificate, client-DH-public-key and signed client-DH-public-key from client
              b. Verify the client's certificate and signature
              c. Generates a shared secret,
              d. Compute session keys using shared secret.*/

        //(a.) Read-in
        System.out.println("[SERVER] reading HS-MSG(3)");
        Certificate clientCertificate = (Certificate) serverIn.readObject();
        BigInteger clientDHPublicKey = (BigInteger) serverIn.readObject();
        BigInteger signedClientDHPublicKey = (BigInteger) serverIn.readObject();

        //(b.) Verify
        Shared.verifyCertificate(clientCertificate);

        //Store client->server messages in message history
        messageHistory.write(clientCertificate.toString().getBytes());
        messageHistory.write(clientDHPublicKey.toByteArray());
        messageHistory.write(signedClientDHPublicKey.toByteArray());

        //(c.)Generates a shared secret
        sharedSecret = Shared.getSharedSecret(clientDHPublicKey, serverDHPrivateKey);
        System.out.println("[SERVER] shared secret: " + sharedSecret);

        //(d.) Compute session keys using shared secret.
        Shared.makeSecretKeys(nonce, sharedSecret);

        /*5. Send 'server Handshake Finish Msg',
        msg = HMAC( server-Mac-key, history so far) */

        byte[] serverHandshakeFinishMsg = Shared.macMessage(messageHistory.toByteArray(), Shared.serverMAC);
        serverOut.writeObject(serverHandshakeFinishMsg);
        messageHistory.write(serverHandshakeFinishMsg);
        System.out.println("[SERVER] sending HS-MSG(4)");

        /* 6.Receive 'client Handshake Finish Msg'  */

        byte[] clientHandshakeFinishMsg = (byte[]) serverIn.readObject();
        System.out.println("[SERVER] reading HS-MSG(5)");


        //Compare message client's mac with our mac, they should be the same
        Shared.checkForValidMAC(clientHandshakeFinishMsg, messageHistory.toByteArray(), Shared.clientMAC);
        messageHistory.write(clientHandshakeFinishMsg);

        //Proceed to post-handshake messaging
        System.out.println("[SERVER]: Handshake was successful. Good to proceed with post-handshake messaging");
        System.out.println("-------------------------------MESSAGING--------------------------------");


        /*----------------------------------------------------------------------
                                        MESSAGING
         ----------------------------------------------------------------------*/
        //SENDING MESSAGE 1 AND RECEIVING ACKNOWLEDGEMENT
        //Encrypt and send message 1
        String serverMsg = new String("Is anyone there?");
        byte[] encryptedServerMsg = Shared.encrypt(serverMsg.getBytes(), Shared.serverEncrypt, Shared.serverInitVector, Shared.serverMAC);
        serverOut.writeObject(encryptedServerMsg);
        System.out.println("[SERVER] Msg 1| Outgoing message before encryption: " + serverMsg);
        System.out.println("[SERVER] Msg 1| Outgoing message after encryption: " + encryptedServerMsg);


        //Receive ack message from server
        byte[] acknowledgementFromClient = (byte[]) serverIn.readObject();

        //decrypt message
        String decryptedAcknowledgementFromClient = Shared.decrypt(acknowledgementFromClient, Shared.clientEncrypt, Shared.clientInitVector, Shared.clientMAC);
        System.out.println("[SERVER] Msg 1| Acknowledgement before decryption: " + acknowledgementFromClient);
        System.out.println("[SERVER] Msg 1| Acknowledgement after decryption: " + decryptedAcknowledgementFromClient);

        //check that the message sent to the server is the clients "ACK"
        if (!decryptedAcknowledgementFromClient.equals("ACK")){
            throw new RuntimeException("ACK not received.");
        }

        //RECEIVING MESSAGE 2 AND SENDING ACKNOWLEDGEMENT
        //Read in and decrypt message from server
        byte[] msgFromClient = (byte[]) serverIn.readObject();
        String decrptedMsgFromClient = Shared.decrypt(msgFromClient, Shared.clientEncrypt, Shared.clientInitVector, Shared.clientMAC);
        System.out.println("[SERVER] Msg 2| Incoming message before decryption: " + msgFromClient);
        System.out.println("[SERVER] Msg 2| Incoming message after decryption: " + decrptedMsgFromClient);

        //Send Acknowledgement message
        String ack = "ACK";
        byte[] encryptedACKMsg = Shared.encrypt(ack.getBytes(), Shared.serverEncrypt, Shared.serverInitVector, Shared.serverMAC);
        serverOut.writeObject(encryptedACKMsg);
        System.out.println("[SERVER] Msg 2| Acknowledgement message before encryption: " + ack);
        System.out.println("[SERVER] Msg 2| Acknowledgement message after encryption: " + encryptedACKMsg);


    }
}