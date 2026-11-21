package geldautomat.service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import geldautomat.data.Kunde;

// public class Server<ClientSocket>
public class Server {
    private static final String COMMAND_AUTH = "AUTH";
    private static final String COMMAND_BALANCE = "BALANCE";
    private static final String COMMAND_WITHDRAW = "WITHDRAW";
    private static final String COMMAND_DISPENSE = "DISPENSE";
    private static final String COMMAND_LOCK = "LOCK";
    private static final String COMMAND_QUIT = "QUIT";
    private static final String RESPONSE_OK = "OK";
    private static final String RESPONSE_ERROR = "ERROR";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_COMMITTED = "COMMITTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DISPENSED = "OK";

	private boolean connected;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private ServerSocket serverSocket;
	private List<Kunde> kunden;
    private Kunde currentKunde;

	public Server() {        		
		kunden = List.of(
			new Kunde(111, 1111, 25000),
			new Kunde(222, 2222, 25000),
			new Kunde(333, 3333, 25000),
			new Kunde(444, 4444, 25000),
			new Kunde(555, 5555, 25000)
		);

		try {
			this.serverSocket = new ServerSocket(27999);
			System.out.println("Warte auf Client...");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void run_forever() {
		while(true){
			try {
				this.socket = this.serverSocket.accept();
				this.connected = true;
				System.out.printf("Client hat sich verbunden: %s%n", socket.getRemoteSocketAddress());
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			handleRequests(socket, reader, writer);
		}
	}

	private void handleRequests (final Socket socket, final BufferedReader reader, final BufferedWriter writer) {
		while (this.connected) {
            try {
				String line = reader.readLine();
                Message reply;

                if(line !=null) {
                    Message msg = Message.decode(line);
                    String command = msg.getCommand();
                    switch (command) {
                        case COMMAND_AUTH:
                            int cardNumber = Integer.parseInt(msg.getParam()[0]);
                            int pin = Integer.parseInt(msg.getParam()[1]);
                            boolean authenticated = false;
                            for (Kunde kunde : kunden) {
                                if (kunde.getCardNumber() == cardNumber && kunde.getPin() == pin) {
                                    authenticated = true;
                                    currentKunde = kunde;
                                    break;
                                }
                            }
                            if (!authenticated) {
                                reply = new Message(RESPONSE_ERROR);
                                break;
                            }
                            reply = new Message(RESPONSE_OK);
                            break;
    
                        case COMMAND_BALANCE:
                            reply = new Message(RESPONSE_OK, currentKunde.getBalance() + "");
                            break;
    
                        case COMMAND_WITHDRAW:
                            int amount = Integer.parseInt(msg.getParam()[0]);
                            // was mit transactionId machen?
                            // String transactionId = msg.getParam()[1];
                            currentKunde.setBalance(currentKunde.getBalance() - amount);
                            reply = new Message(RESPONSE_OK, STATUS_APPROVED);
                            break;
                        case COMMAND_DISPENSE:
                            if(msg.getParam()[1].equals(STATUS_DISPENSED))
                                reply = new Message(RESPONSE_OK, STATUS_COMMITTED);
                            else if(msg.getParam()[1].equals(STATUS_FAILED))
                                reply = new Message(RESPONSE_OK, STATUS_CANCELLED);
                            else
                                reply = new Message(RESPONSE_ERROR);
                            break;
                        case COMMAND_LOCK:
                            int cardNumberToLock = Integer.parseInt(msg.getParam()[0]);
                            for (Kunde kunde : kunden) {
                                if (kunde.getCardNumber() == cardNumberToLock) {
                                    kunde.setBlocked();
                                    break;
                                }
                            }
                            reply = new Message(RESPONSE_OK);
                            break;
                        case COMMAND_QUIT:
                            reply = new Message(RESPONSE_OK);
                            break;
                        default:
                            reply = new Message(RESPONSE_ERROR);
                            break;
                    }
    
                    writer.write(reply.encode());
                    writer.flush();
                    
                    System.out.printf("Vom Client (%s) empfangen: %s%n", socket.getRemoteSocketAddress(), line);
                    System.out.printf("Sende an Client (%s): %s%n", socket.getRemoteSocketAddress(), reply.encode());
                } else {
                    connected = false;
                }
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
		}
	}

	public static void main(String[] args) {
        System.out.println("Starting server...");
		Server server = new Server();
		// SERVER.run_forever();
		server.run_forever();

	}
}
