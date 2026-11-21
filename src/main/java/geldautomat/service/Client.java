package geldautomat.service;

import java.io.*;
import java.net.Socket;

public class Client
{

	public static Client client;

	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;

	public Client()	{
		try
		{
			socket = new Socket("localhost", 27999);
			System.out.println("Client connected end with \"END\"");

			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public String sendText(final Message msg) {
		try {
			System.out.println("Sende an Server: " + msg.encode());
			writer.write(msg.encode());
			writer.flush();
			final String line = reader.readLine();
			// System.out.println("Vom Server empfangen: " + line);
			return line;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void close() {
		try {
			socket.shutdownOutput();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// AUTH Karte123 1234 		OK oder ERROR 		Prüft Kartennummer und PIN anhand der Datenbank.
	// BALANCE 					OK 25000 			Liefert den Kontostand; Beispielwert 25000 Cent.
	// WITHDRAW 5000 T17 		OK APPROVED 		Abhebung von 5000 Cent mit Transaktionskennung T17.

	public String sendAuth(final int cardNumber, final int pin) {
		String line = "AUTH " + cardNumber + " " + pin;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public String getBalance() {
		String line = "BALANCE";
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public String withdraw(final int amount, final String transactionId) {
		String line = "WITHDRAW " + amount + " " + transactionId;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public void LockCard(final int cardNumber) {
		String line = "LOCK " + cardNumber;
		Message msg = Message.decode(line);
		sendText(msg);
	}


	public static void main(String[] args) throws IOException {
		System.out.println("Starting client...");

		client = new Client();

		// final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// while (true)
		// {
		// 	final String line = reader.readLine();
		// 	if (line.equals("END")) {
		// 		break;
		// 	}

		// 	Message msg = Message.decode(line);
		// 	client.sendText(msg);
		// }

		// client.close();
	}
}
