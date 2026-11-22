package geldautomat.service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import geldautomat.data.StringExpressions;

public class Server {
    private final int port = 27999;

	private boolean connected;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private ServerSocket serverSocket;
    private Integer currentCardNumber;

	public Server() {        		
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
        currentCardNumber = null;
        WithdrawalSession withdrawal = new WithdrawalSession();
		while (this.connected) {
            Message reply = null;
            try {
                String line = reader.readLine();
                if(line !=null) {
                    Message msg = Message.decode(line);
                    String command = msg.getCommand();
                    switch (command) {
                        case StringExpressions.COMMAND_AUTH:
                            if (withdrawal.hasPending()) {
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                                break;
                            }
                            currentCardNumber = null;
                            int cardNumber = Integer.parseInt(msg.getParam()[0]);
                            int pin = Integer.parseInt(msg.getParam()[1]);
                            boolean authenticated = false;
                            try {
                                ResultSet result = DatabaseManager.selectKunde(cardNumber);
                                if (result.next() && result.getInt(StringExpressions.FIELD_PIN) == pin) {
                                    authenticated = true;
                                    currentCardNumber = result.getInt(StringExpressions.FIELD_KARTENNUMMER);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            if (!authenticated) {
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                                break;
                            }
                            reply = new Message(StringExpressions.RESPONSE_OK);
                            break;
    
                        case StringExpressions.COMMAND_BALANCE:
                            if (currentCardNumber == null) {
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                                break;
                            }
                            ResultSet result = null;
                            try {
                                result = DatabaseManager.selectBalanceByCardNumber(currentCardNumber);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            int balance = 0;
                            if (result != null && result.next()) {
                                balance = result.getInt(StringExpressions.FIELD_BALANCE);
                                reply = new Message(StringExpressions.RESPONSE_OK, balance + "");
                            }
                            break;
    
                        case StringExpressions.COMMAND_WITHDRAW:
                            try {
                                reply = withdrawal.withdraw(DatabaseManager.connect(), currentCardNumber, msg.getParam());
                            } catch (SQLException e) {
                                e.printStackTrace();
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                            }
                            break;
                            
                        case StringExpressions.COMMAND_DISPENSE:
                            try {
                                reply = withdrawal.dispense(DatabaseManager.connect(), currentCardNumber, msg.getParam());
                            } catch (SQLException e) {
                                e.printStackTrace();
                                reply = new Message(StringExpressions.RESPONSE_ERROR);
                            }
                            break;

                        case StringExpressions.COMMAND_LOCK:
                            int cardNumberToLock = Integer.parseInt(msg.getParam()[0]);
                            try {
                                DatabaseManager.lockKundeByCardNumber(cardNumberToLock);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            reply = new Message(StringExpressions.RESPONSE_OK);
                            break;

                        case StringExpressions.COMMAND_QUIT:
                            reply = new Message(StringExpressions.RESPONSE_OK);
                            break;

                        default:
                            reply = new Message(StringExpressions.RESPONSE_ERROR);
                            break;
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
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
		}
	}

	public static void main(String[] args) {
        System.out.println("Starting server...");
        Connection connection = null;
		try {
            connection = DatabaseManager.connect();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (connection != null) {
            try {
                ResultSet result = DatabaseManager.selectAllFromKunden();
                ResultSetMetaData metaData = result.getMetaData();
                while (result.next()) {
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        System.out.print(metaData.getColumnName(i) + "=" + result.getObject(i));
                        if (i < metaData.getColumnCount()) {
                            System.out.print(", ");
                        }
                    }
                    System.out.println();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        Server server = new Server();
		// SERVER.run_forever();
		server.run_forever();

	}
}
