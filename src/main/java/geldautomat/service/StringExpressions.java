package geldautomat.service;
public class StringExpressions {

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
	public static final String FIELD_BLOCKED = "blocked";

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
    public static final String RESPONSE_BLOCKED = "BLOCKED";
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
	public static final String DB_TRANSACTION_ACTIVE = "Eine andere Datenbanktransaktion ist bereits aktiv.";

	public static final String DB_TRANSACTION_FIELD_AMOUNT = "amount";

}
