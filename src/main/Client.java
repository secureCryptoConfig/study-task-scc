package main;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import org.securecryptoconfig.SCCException;
import org.securecryptoconfig.SCCKey;
import org.securecryptoconfig.SCCKey.KeyUseCase;
import org.securecryptoconfig.SCCSignature;
import org.securecryptoconfig.SecureCryptoConfig;

import com.fasterxml.jackson.core.JsonProcessingException;

import COSE.CoseException;
import main.Message.MessageType;

/**
 * Class that simulates the behavior of a Client that interacts with a stock-server.
 * 
 * Clients can be created automatically and register themselves at the server with their public key.
 * Orders of different types can be created automatically that will be send signed to the server.
 * The client has also the possibility to ask for already send orders
 *
 */
public class Client implements Runnable {
	
	//maximum timeout of client used in "run" Method
	private static int sendFrequency = 5000;
	
	int clientID;
	byte[] keyBytes;
	Server server;

	/**
	 * Constructor of client
	 * @param clientID
	 * @param keyBytes
	 * @param server
	 */
	private Client(int clientID, byte[] keyBytes, Server server) {
		this.clientID = clientID;
		this.keyBytes = keyBytes;
		this.server = server;
	}

	/**
	 * Getter for client ID
	 * @return int : Id of client
	 */
	public int getID() {
		return this.clientID;
	}

	
	/**
	 * Methods that signs the client order with the corresponding key
	 * @param order
	 * @param keyBytes
	 * @return byte[] : signature
	 * @throws CoseException
	 */
	private static byte[] signMessage(String order, byte[] keyBytes) throws CoseException {
		
		SCCKey key = SCCKey.createFromExistingKey(keyBytes);
		
		//TODO: Perform signing of the parameter "order" with the given SCCKey "key"
		// Catch possible occurring exceptions
		
		return new byte[0];
	}

	/**
	 * Clients are registered with their public key by the server.
	 * 
	 * The server needs the client public key for validation of signed messages.
	 * First a key for the client is generated which will then be send to the
	 * server. The server gives back a clientId and a new client will be generated.
	 * 
	 * @param server
	 * @return Client: new generated client
	 * @throws NoSuchAlgorithmException
	 * @throws CoseException
	 * @throws IllegalStateException
	 */
	public static Client generateNewClient(Server server)
			throws NoSuchAlgorithmException, CoseException, IllegalStateException {

		SCCKey key = null;
		try {
			key = SCCKey.createKey(KeyUseCase.Signing);

			byte[] keyBytes = key.decodeObjectToBytes();

			int clientID = server.registerClient(keyBytes);
			if (clientID == -1) {
				throw new IllegalStateException("server does not seem to accept the client registration!");
			}

			Client c = new Client(clientID, keyBytes, server);
			return c;

		} catch (SCCException | COSE.CoseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Automatically generates a order of a specific type (BuyStock, SellStock, GetOrders). 
	 * Orders for buying/selling are containing an amount of stock to buy/sell from a specific stock
	 * 
	 * @return String: message
	 * @param type
	 * @throws NumberFormatException
	 * @throws JsonProcessingException
	 */
	private static String generateRandomMessage(MessageType type) throws NumberFormatException, JsonProcessingException {
        
        switch (type) {
            case BuyStock: return Message.createBuyStockMessage(generateRandomString(12), generateRandomNumber(3));
            case SellStock: return Message.createSellStockMessage(generateRandomString(12), generateRandomNumber(10));
            case GetOrders: return Message.createGetOrdersMessage();
            default: return Message.createGetOrdersMessage();
        }
	}

	/**
	 * Sending of signed message for buying/selling stock to server. Server sends a
	 * response. Message is accepted if signature can be validated.
	 * 
	 * @param message
	 * @throws JsonProcessingException
	 */
	private void sendMessage(String message) throws CoseException, JsonProcessingException {
        p("creating signature for message: " + message);
        byte[] signature = signMessage(message, keyBytes);
        p("signature is (base64 encoded): " + (signature.length > 0 ? Base64.getEncoder().encodeToString(signature) : "null"));
		String signedMessage = SignedMessage.createSignedMessage(this.clientID, message, signature);

		p("sending to server: " + signedMessage);
		String result = server.acceptMessage(signedMessage);
		p("result from server: " + result);

	}

	/**
	 * Auxiliary method for generating a String of a given length.
	 * Result simulates amount of stock that should be bought.
	 * @param length
	 * @return String
	 */
	private static String generateRandomNumber(int length) {

		String AlphaNumericString = "01234567890";
		StringBuilder sb = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int index = (int) (AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}

		return sb.toString();
	}

	/**
	 * Auxiliary method for generating a String of a given length.
	 * Result simulates id of stock that should be bought.
	 * @param length
	 * @return String
	 */
	private static String generateRandomString(int length) {

		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int index = (int) (AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}

		return sb.toString();
	}

	/**
	 * Auxiliary method for showing some responses/requests in the communication between client and server
	 * @param s
	 */
	private void p(String s) {
		System.out.println(Instant.now().toString() + " client " + this.clientID + ": " + s);
	}

	@Override
	public void run() {
		//while (true) {
			try {
				Thread.sleep((long)(Math.random() * sendFrequency + 1));
                sendMessage(generateRandomMessage(MessageType.BuyStock));
                sendMessage(generateRandomMessage(MessageType.SellStock));
                sendMessage(generateRandomMessage(MessageType.GetOrders));
			} catch (InterruptedException | CoseException e) {
				 e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		//}

	}
}
