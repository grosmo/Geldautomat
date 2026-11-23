package geldautomat.ui;

public class UIStringExpressions {
	
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
	public static final String PROMPT_ABHEBUNG_ABGELEHNT = "Abhebung abgelehnt:\nBetrag, Guthaben, Sperre oder offener Vorgang prüfen.";
	public static final String PROMPT_ABHEBUNG_UNGEKLAERT = "Abhebung konnte nicht bestätigt werden. Bitte Vorgang prüfen lassen.";
	public static final String PROMPT_ABHEBUNG_ERFOLGREICH = "Abhebung erfolgreich abgeschlossen.";
	public static final String PROMPT_ABHEBUNG_ABGEBROCHEN = "Geldausgabe fehlgeschlagen; Abhebung abgebrochen.";
	// public static final String PROMPT_ABHEBUNG_LAEUFT = "Abhebung wird verarbeitet...";
    public static final String PROMPT_SERVER_FEHLER = "Fehler beim Verbinden mit dem Server.";
	public static final String PROMPT_GELDAUSGABE = "Befinden sich ? € im CD Laufwerk";
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
}
