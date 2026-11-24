package geldautomat.service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

public class Server {
    private final int port = 27999;

	private boolean connected;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private ServerSocket serverSocket;
    private int currentCardNumber;

	public Server() {
        currentCardNumber = 0;        		
        try {
			this.serverSocket = new ServerSocket(port);
			System.out.println(StringExpressions.PROMPT_WARTE);
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
				System.out.printf(StringExpressions.PROMPT_VERBUNDEN, socket.getRemoteSocketAddress());
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
        ServerService serverService = new ServerService();
		while (this.connected) {
            Message reply = null;
            try {
                String line = reader.readLine();
                if(line !=null) {
                    Message msg = Message.decode(line);
                    String command = msg.getCommand();
                    try {
                        switch (command) {
                            case StringExpressions.COMMAND_AUTH:
                                int cardNumber = Integer.parseInt(msg.getParam()[0]);
                                int pin = Integer.parseInt(msg.getParam()[1]);
                                reply = serverService.authenticate(cardNumber, pin, msg.getParam());
                                if(reply.getCommand().equals(StringExpressions.RESPONSE_OK))
                                    currentCardNumber = cardNumber;
                                break;
        
                            case StringExpressions.COMMAND_BALANCE:
                                if (currentCardNumber == 0) {
                                    reply = new Message(StringExpressions.RESPONSE_ERROR);
                                    break;
                                }
                                reply = serverService.getBalance(currentCardNumber);
                                break;
        
                            case StringExpressions.COMMAND_WITHDRAW:
                                reply = serverService.withdraw(currentCardNumber, msg.getParam());
                                break;
                                
                            case StringExpressions.COMMAND_DISPENSE:
                                reply = serverService.dispense(currentCardNumber, msg.getParam());
                                break;

                            case StringExpressions.COMMAND_LOCK:
                                reply = serverService.lock(Integer.parseInt(msg.getParam()[0]), msg.getParam());
                                break;

                            case StringExpressions.COMMAND_QUIT:
                                currentCardNumber = 0;
                                reply = new Message(StringExpressions.RESPONSE_OK);
                                break;

                            default:
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                                break;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        reply = new Message(StringExpressions.RESPONSE_ERROR);
                    }
    
                    writer.write(reply.encode());
                    writer.flush();
                    
                    System.out.printf(StringExpressions.PROMPT_CMD_AUSGABE_EMPFANGEN, socket.getRemoteSocketAddress(), line);
                    System.out.printf(StringExpressions.PROMPT_CMD_AUSGABE_SENDEN, socket.getRemoteSocketAddress(), reply.encode());
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
        try {
            for (String kunde : DatabaseManager.selectAllFromKunden()) {
                System.out.println(kunde);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Server server = new Server();
		server.run_forever();
	}
}
