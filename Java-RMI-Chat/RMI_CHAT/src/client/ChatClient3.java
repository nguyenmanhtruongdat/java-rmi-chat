package client;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;

import server.ChatServerIF;


public class ChatClient3  extends UnicastRemoteObject implements ChatClient3IF {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7468891722773409712L;
	ClientRMIGUI chatGUI;
	private String hostName = "localhost";
	private String serviceName = "GroupChatService";
	private String clientServiceName;
	private String name;
	protected ChatServerIF serverIF;
	protected boolean connectionProblem = false;
	private String encryptionKey = "SECRET_KEY";
	
	/**
	 * class constructor,
	 * note may also use an overloaded constructor with 
	 * a port no passed in argument to super
	 * @throws RemoteException
	 */
	public ChatClient3(ClientRMIGUI aChatGUI, String userName) throws RemoteException {
		super();
		this.chatGUI = aChatGUI;
		this.name = userName;
		this.clientServiceName = "ClientListenService_" + userName;
	}

	
	/**
	 * Register our own listening service/interface
	 * lookup the server RMI interface, then send our details
	 * @throws RemoteException
	 */
	public void startClient() throws RemoteException {		
		String[] details = {name, hostName, clientServiceName};	

		try {
			Naming.rebind("rmi://" + hostName + "/" + clientServiceName, this);
			serverIF = ( ChatServerIF )Naming.lookup("rmi://" + hostName + "/" + serviceName);	
		} 
		catch (ConnectException  e) {
			JOptionPane.showMessageDialog(
					chatGUI.frame, "The server seems to be unavailable\nPlease try later",
					"Connection problem", JOptionPane.ERROR_MESSAGE);
			connectionProblem = true;
			e.printStackTrace();
		}
		catch(NotBoundException | MalformedURLException me){
			connectionProblem = true;
			me.printStackTrace();
		}
		if(!connectionProblem){
			registerWithServer(details);
		}	
		System.out.println("Client Listen RMI Server is running...\n");
	}


	/**
	 * pass our username, hostname and RMI service name to
	 * the server to register out interest in joining the chat
	 * @param details
	 */
	public void registerWithServer(String[] details) {		
		try{
			serverIF.passIDentity(this.ref);//now redundant ??
			serverIF.registerListener(details);			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	//=====================================================================
	/**
	 * Receive a string from the chat server
	 * this is the clients RMI method, which will be used by the server 
	 * to send messages to us
	 */
	@Override
	public void messageFromServer(String encryptedMessage) throws RemoteException {
		if (encryptedMessage.startsWith("[Server]:")) {
			// Server message, display it without decryption
			chatGUI.textArea.append(encryptedMessage + "\n");
		}else {
			// Regular user message, decrypt it
			String decryptedMessage = decryptMessage(encryptedMessage);
			chatGUI.textArea.append(decryptedMessage + "\n");
		}

		// Make the GUI display the last appended text, i.e., scroll to the bottom
		chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());
	}

	private String decryptMessage(String encryptedMessage) {
		StringBuilder decryptedText = new StringBuilder();

		// Split the message into username and actual message
		String[] parts = encryptedMessage.split(":", 2);

		if (parts.length == 2) {
			String username = parts[0].trim();
			String message = parts[1].trim();

			for (int i = 0; i < message.length(); i++) {
				char currentChar = message.charAt(i);
				char decryptedChar;

				if (Character.isLetter(currentChar)) {
					decryptedChar = (char) (currentChar - encryptionKey.length());

					// Wrap around for characters outside the alphabet
					if (Character.isUpperCase(currentChar) && decryptedChar < 'A') {
						decryptedChar = (char) (decryptedChar + 26);
					} else if (Character.isLowerCase(currentChar) && decryptedChar < 'a') {
						decryptedChar = (char) (decryptedChar + 26);
					}
				} else {
					decryptedChar = currentChar;
				}

				decryptedText.append(decryptedChar);
			}

			return username + ": " + decryptedText.toString();
		} else {
			return encryptedMessage;
		}
	}

	/**
	 * A method to update the display of users 
	 * currently connected to the server
	 */
	@Override
	public void updateUserList(String[] currentUsers) throws RemoteException {

		if(currentUsers.length < 2){
			chatGUI.privateMsgButton.setEnabled(false);
		}
		chatGUI.userPanel.remove(chatGUI.clientPanel);
		chatGUI.setClientPanel(currentUsers);
		chatGUI.clientPanel.repaint();
		chatGUI.clientPanel.revalidate();
	}

}//end class













