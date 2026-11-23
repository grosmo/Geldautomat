package geldautomat.data;

public class SQLStatements {
    public static final String SQL_SELECT_ALL_FROM_KUNDEN = "SELECT * FROM kunden;";
	public static final String SQL_SELECT_KUNDE_BY_CARD_NUMBER = "SELECT * FROM kunden WHERE cardNumber = ?;";
	public static final String SQL_SELECT_BALANCE_BY_CARD_NUMBER = "SELECT balance FROM kunden WHERE cardNumber = ?;";
	public static final String SQL_LOCK_KUNDE_BY_CARD_NUMBER = "UPDATE kunden SET blocked = 1 WHERE cardNumber = ?;";
	public static final String SQL_UPDATE_BALANCE_BY_CARD_NUMBER = "UPDATE kunden SET balance = ? WHERE cardNumber = ?;";
	public static final String SQL_INSERT_TRANSFAKTION = "INSERT INTO transaktionen (transactionId, cardNumber, amount, status) VALUES (?, ?, ?, ?);";
	public static final String SQL_APPROVE_WITHDRAWAL = "INSERT INTO transaktionen (transactionId, cardNumber, amount, status) "
		+ "SELECT ?, cardNumber, ?, ? FROM kunden WHERE cardNumber = ? AND blocked = 0 AND balance >= ? "
		+ "AND NOT EXISTS (SELECT 1 FROM transaktionen WHERE cardNumber = ? AND status = ?)";
	public static final String SQL_SELECT_PENDING_WITHDRAWAL = "SELECT amount FROM transaktionen WHERE transactionId = ? AND cardNumber = ? AND status = ?";
	public static final String SQL_DEBIT_WITHDRAWAL = "UPDATE kunden SET balance = balance - ? WHERE cardNumber = ? AND balance >= ?";
	public static final String SQL_FINISH_WITHDRAWAL = "UPDATE transaktionen SET status = ? WHERE transactionId = ? AND cardNumber = ? AND status = ?";    
}
