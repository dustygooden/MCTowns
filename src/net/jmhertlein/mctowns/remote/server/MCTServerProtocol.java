package net.jmhertlein.mctowns.remote.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import net.jmhertlein.core.crypto.CryptoManager;
import net.jmhertlein.mctowns.remote.AuthenticationChallenge;
import net.jmhertlein.mctowns.remote.EncryptedSecretKey;
import net.jmhertlein.mctowns.remote.RemoteAction;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author joshua
 */
public class MCTServerProtocol {
    private static final int NUM_CHECK_BYTES = 50;
    private Socket client;
    private File authKeysDir;
    private CryptoManager cMan;
    private PublicKey serverPubKey;
    private String clientName;
    private PrivateKey serverPrivateKey;
    private Map<Integer, ClientSession> sessionKeys;
    private Plugin p;
    private static volatile Integer nextSessionID = 0;

    public MCTServerProtocol(Plugin p, Socket client, PrivateKey serverPrivateKey, PublicKey serverPublicKey, File authKeysDir, Map<Integer, ClientSession> sessionKeys) {
        this.authKeysDir = authKeysDir;
        this.serverPubKey = serverPublicKey;
        this.cMan = new CryptoManager();
        this.client = client;
        this.serverPrivateKey = serverPrivateKey;
        this.sessionKeys = sessionKeys;
        this.p = p;
    }

    private boolean exchangeKeys(ObjectInputStream ois) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        System.out.println("Loading client key from disk.");
        PublicKey clientKey = cMan.loadPubKey(new File(authKeysDir, clientName + ".pub"));

        ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
        
        if (clientKey == null) {
            System.out.println("Didnt have public key for user. Exiting.");
            oos.writeObject(false);
            return false;
        } else
            oos.writeObject(true);
        
        //init ciphers
        Cipher outCipher = Cipher.getInstance("RSA");
        outCipher.init(Cipher.ENCRYPT_MODE, clientKey);
        
        Cipher inCipher = Cipher.getInstance("RSA");
        inCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
        
        System.out.println("Sending our public key.");
        oos.writeObject(serverPubKey);
        
        Boolean clientAcceptsPublicKey = (Boolean) ois.readObject();
        
        if(!clientAcceptsPublicKey) {
            System.out.println("Client did not accept our public key- did not match their cached copy.");
            return false;
        }
        
        //send client auth challenge
        AuthenticationChallenge originalChallenge = new AuthenticationChallenge(NUM_CHECK_BYTES);
        oos.writeObject(originalChallenge.encrypt(outCipher));
        
        AuthenticationChallenge clientResponse = (AuthenticationChallenge) ois.readObject();
        clientResponse = clientResponse.decrypt(inCipher);
        
        if(clientResponse.equals(originalChallenge)) {
            oos.writeObject(true);
            System.out.println("Accepting client challenge response.");
        } else {
            oos.writeObject(false);
            System.out.println("Rejecting client challenge response.");
            return false;
        }
        
        AuthenticationChallenge clientChallenge = (AuthenticationChallenge) ois.readObject();
        oos.writeObject(clientChallenge.decrypt(inCipher).encrypt(outCipher));
        
        Boolean clientAcceptsUs = (Boolean) ois.readObject();
        
        if(!clientAcceptsUs) {
            System.out.println("Client did not accept us as server they wanted to connect to.");
            return false;
        } else
            System.out.println("Client accepts us.");

        System.out.println("Making new session key.");
        SecretKey newKey = CryptoManager.newAESKey(p.getConfig().getInt("remoteAdminSessionKeyLength"));
        System.out.println("Session key made.");

        System.out.println("Writing key...");
        oos.writeObject(new EncryptedSecretKey(newKey, outCipher));
        System.out.println("Wrote key.");
        
        int assignedSessionID = nextSessionID;
        nextSessionID++;
        
        sessionKeys.put(assignedSessionID, new ClientSession(assignedSessionID, clientName, newKey));
        System.out.println("Client assigned session id " + assignedSessionID);
        
        System.out.println("Writing session ID.");
        oos.writeObject(assignedSessionID);
        System.out.println("Wrote session ID");

        System.out.println("Done handling client.");
        return true;
    }

    public void handleAction() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        System.out.println("Opening input stream.");
        ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

        System.out.println("Getting action.");
        RemoteAction clientAction = (RemoteAction) ois.readObject();
        System.out.println("Getting username.");
        String username = (String) ois.readObject();
        clientName = username;

        System.out.println("Handling action.");
        switch (clientAction) {
            case KEY_EXCHANGE:
                if (!exchangeKeys(ois))
                    System.err.println("Username " + username + " tried to connect, but was not authorized.");
                break;
            case TERMINATE_SESSION:
                clearSessionKey(username);
                break;
        }

        client.close();
        System.out.println("Finished handling action.");
    }

    private void clearSessionKey(String username) {
        sessionKeys.remove(username);
    }
}