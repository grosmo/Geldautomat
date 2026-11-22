package geldautomat.service;

import java.io.*;
import java.net.Socket;

import geldautomat.data.StringExpressions;

public class Client {
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;
	private static final int SERVER_PORT = 27999;

	public Client()	throws IOException {
		socket = new Socket("localhost", SERVER_PORT);
		System.out.println("Client connected end with \"END\"");

		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

	public String sendAuth(final int cardNumber, final int pin) {
		String line = StringExpressions.COMMAND_AUTH + " " + cardNumber + " " + pin;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public String getBalance() {
		String line = StringExpressions.COMMAND_BALANCE;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public String withdraw(final int amount, final long transactionId) {
		String line = StringExpressions.COMMAND_WITHDRAW + " " + amount + " " + transactionId;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public String dispense(final long transactionId, final boolean dispensed) {
		String status = dispensed ? StringExpressions.RESPONSE_OK : StringExpressions.STATUS_FAILED;
		String line = StringExpressions.COMMAND_DISPENSE + " " + transactionId + " " + status;
		Message msg = Message.decode(line);
		return sendText(msg);
	}

	public void LockCard(final int cardNumber) {
		String line = StringExpressions.COMMAND_LOCK + " " + cardNumber;
		Message msg = Message.decode(line);
		sendText(msg);
	}
}
