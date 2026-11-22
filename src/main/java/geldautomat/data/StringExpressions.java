package geldautomat.data;
public class StringExpressions {

	/*
	 * String-Ausdrücke für die Benutzeroberfläche.
	*/
	public static final String FENSTER_TITEL = "Geldautomat";
	public static final String BUTTON_JA = "Ja";
	public static final String BUTTON_NEIN = "Nein";
	public static final String BUTTON_GELDABHEBEN = "Geld abheben";
	public static final String BUTTON_KONTOSTAND_ANZEIGEN = "Kontostand anzeigen";
	public static final String BUTTON_KARTE_AUSWERFEN = "Karte auswerfen";
	
	
	public static final String LEER = "";
	public static final String OK = "OK";
	public static final String CLEAR = "CLEAR";
	public static final String PROMPT_KARTENNUMMER = "Kartennummer eingeben:";
	public static final String PROMPT_PIN = "PIN eingeben:";
	public static final String PROMPT_AUTH_ERFOLGREICH = "Authentifizierung erfolgreich.";
	public static final String PROMPT_KARTE_GESPERRT = "Zu viele Fehlversuche - Karte ist gesperrt.";
	public static final String PROMPT_PIN_FALSCH = "Falscher PIN, neu versuchen.";
	public static final String PROMPT_AKTION = "Was wollen Sie tun?";
	public static final String PROMPT_ABHEBEN = "Betrag eingeben:";
	public static final String PROMPT_KARTE_AUSGEWORFEN = "Karte ausgeworfen - Auf Wiedersehen!";
	public static final String PROMPT_FEHLER_BEIM_ABHEBEN = "Unbekannter Fehler beim Abheben - Auf Wiedersehen!";
	public static final String PROMPT_ABHEBUNG_ABGELEHNT = "Abhebung abgelehnt: Betrag, Guthaben, Sperre oder offener Vorgang prüfen.";
	public static final String PROMPT_ABHEBUNG_UNGEKLAERT = "Abhebung konnte nicht bestätigt werden. Bitte Vorgang prüfen lassen.";
	public static final String PROMPT_ABHEBUNG_ERFOLGREICH = "Abhebung erfolgreich abgeschlossen.";
	public static final String PROMPT_ABHEBUNG_ABGEBROCHEN = "Geldausgabe fehlgeschlagen; Abhebung abgebrochen.";
	public static final String PROMPT_ABHEBUNG_LAEUFT = "Abhebung wird verarbeitet...";
    public static final String PROMPT_SERVER_FEHLER = "Fehler beim Verbinden mit dem Server.";
	public static final String PROMPT_GELDAUSGABE = "Befinden sich ? € im CD Laufwerk?";
	public static final String PROMPT_KONTOSTAND = "Kontostand: ? €";
    
	public static final String[][] zahlenButtonsStrings = {
		{"1", "2", "3"},
		{"4", "5", "6"},
		{"7", "8", "9"},
		{"0", OK, CLEAR}
	};

	public static final String TEXT_STYLE_ROT = "-fx-text-fill: red; -fx-font-weight: bold; ";
	public static final String TEXT_STYLE_SCHWARZ = "-fx-text-fill: black; -fx-font-weight: normal; ";
	public static final String STYLE_SPACING = "-fx-spacing: 10;";

	/*
	 * Server Kommunikation
	 */
	public static final String PROMPT_WARTE = "Warte auf Client...";
	public static final String PROMPT_VERBUNDEN = "Client hat sich verbunden: %s%n";
	public static final String PROMPT_CMD_AUSGABE_EMPFANGEN = "Vom Client (%s) empfangen: %s%n";
	public static final String PROMPT_CMD_AUSGABE_SENDEN = "Sende an Client (%s): %s%n";
	public static final String FIELD_PIN = "pin";
	public static final String FIELD_KARTENNUMMER = "cardNumber";
	public static final String FIELD_BALANCE = "balance";

	/*
	 * Command und response strings zur Kommunikation mit dem Server.
	 */
	public static final String COMMAND_AUTH = "AUTH";
    public static final String COMMAND_BALANCE = "BALANCE";
    public static final String COMMAND_WITHDRAW = "WITHDRAW";
    public static final String COMMAND_DISPENSE = "DISPENSE";
    public static final String COMMAND_LOCK = "LOCK";
    public static final String COMMAND_QUIT = "QUIT";
    public static final String RESPONSE_OK = "OK";
    public static final String RESPONSE_ERROR = "ERROR";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_COMMITTED = "COMMITTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DISPENSED = "DISPENSED";

	/*
	 * DB-Kommunikation
	 */
	public static final String DB_URL = "jdbc:sqlite:src/main/java/geldautomat/data/data.db";
	public static final String DB_INIT = "org.sqlite.JDBC";
	public static final String DB_ERROR_DRIVER_NOT_FOUND = "SQLite-Treiber wurde nicht gefunden!";

	/*
	 * SQL-Ausdrücke für die Datenbank.
	 */
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
