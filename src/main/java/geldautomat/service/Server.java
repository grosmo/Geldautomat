package geldautomat.service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// public class Server<ClientSocket>
public class Server {
	private boolean connected;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private ServerSocket serverSocket;

	public Server() {
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

	private void handleRequests (
        final Socket socket, final BufferedReader reader, final BufferedWriter writer) {
		while (this.connected)
		{
            try
			{
				String line = reader.readLine();
                Message reply;

                if(line !=null)
                {
                    Message msg = Message.decode(line);
                    String command = msg.getCommand();
                    switch (command) 
                    {
                        case "AUTH":
                            reply = new Message("OK");
                            break;
    
                        case "BALANCE":
                            reply = new Message("OK", "25000");
                            break;
    
                        case "WITHDRAW":
                            reply = new Message("OK", "APPROVED");
                            break;
                        case "DISPENSE":
                            if(msg.getParam()[1].equals("OK"))
                            {
                                reply = new Message("OK", "COMMITTED");
                            }
                            else if(msg.getParam()[1].equals("FAILED"))
                            {
                                reply = new Message("OK", "CANCELLED");
                            }
                            else
                            {
                                reply = new Message("ERROR");
                            }
                            break;
                        case "QUIT":
                            reply = new Message("OK");
                            break;
                        default:
                            reply = new Message("ERROR");
                            break;
                    }
    
                writer.write(reply.encode());
                writer.flush();
                
                System.out.printf("Vom Client (%s) empfangen: %s%n", socket.getRemoteSocketAddress(), line);
                System.out.printf("Sende an Client (%s): %s%n", socket.getRemoteSocketAddress(), reply.encode());
                }
                else
                {
                    connected = false;
                }
            }
            catch (IOException e)
            {
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
