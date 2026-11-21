package geldautomat.ui;

import java.util.ArrayList;
import java.util.List;

import geldautomat.service.Client;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GeldautomatUI extends Application {

	private static final String LEER = "";
	private static final String OK = "OK";
	private static final String CLEAR = "CLEAR";
	private static final String PROMPT_KARTENNUMMER = "Kartennummer eingeben:";
	private static final String PROMPT_PIN = "PIN eingeben:";
	private static final String PROMPT_AUTH_ERFOLGREICH = "Authentifizierung erfolgreich.";
	private static final String PROMPT_KARTE_GESPERRT = "Zu viele Fehlversuche - Karte ist gesperrt.";
	private static final String PROMPT_PIN_FALSCH = "Falscher PIN, neu versuchen.";
	private static final String STYLESHEET = "/application.css";
	private static final String FENSTER_TITEL = "Geldautomat";
	private static final String PROMPT_AKTION = "Was wollen Sie tun?";

	private final String[][] zahlenButtonsStrings = {
		{"1", "2", "3"},
		{"4", "5", "6"},
		{"7", "8", "9"},
		{"0", OK, CLEAR}
	};

	String eingabe = "";
	Label eingabeAufforderungLabel = new Label();
	Label eingabeLabel = new Label(eingabe);
	int kartennummerTemp = 0;
	int pinTemp = 0;
	int pinCount = 0;

	Button btnGeldAbheben;
	Button btnKontostandAnzeigen;
	List<Button> actionButtons = new ArrayList<>();

	@Override
	public void start(Stage primaryStage) {
		try {
			GridPane zahlenGrid = new GridPane();
			ColumnConstraints clmn = new ColumnConstraints();
			clmn.setPercentWidth(100 / 3);
			clmn.setFillWidth(true);
			zahlenGrid.getColumnConstraints().addAll(clmn, clmn, clmn);
			
			RowConstraints rw = new RowConstraints();
			rw.setPercentHeight(100 / 4);
			rw.setFillHeight(true);
			zahlenGrid.getRowConstraints().addAll(rw, rw, rw, rw);

			zahlenGrid.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
			List<Button> zahlenButtons = new ArrayList<>();

			eingabeLabel.setText(eingabe);
			for (int i = 0; i < zahlenButtonsStrings.length; i++) {
				String[] row = zahlenButtonsStrings[i];
				for (int j = 0; j < row.length; j++) {
					String label = row[j];
					Button btn = new Button(label);
					btn.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
					zahlenGrid.add(btn, j, i);
					zahlenButtons.add(btn);
				}
			}

			zahlenButtons.forEach(btn -> btn.setOnAction(e -> {
				String text = btn.getText();
				if (text.equals(CLEAR)) {
					eingabe = LEER;
				} else if (text.equals(OK)) {
					if (eingabeAufforderungLabel.getText().equals(PROMPT_KARTENNUMMER)) {
						kartennummerTemp = Integer.parseInt(eingabe);
						eingabeAufforderungLabel.setText(PROMPT_PIN);
						eingabe = LEER;
					} else if (eingabeAufforderungLabel.getText().equals(PROMPT_PIN) || eingabeAufforderungLabel.getText().equals(PROMPT_PIN_FALSCH)) {
						pinTemp = Integer.parseInt(eingabe);
						eingabe = LEER;
						String response = Client.client.sendAuth(kartennummerTemp, pinTemp);
						if(response.equals(OK)) {
							eingabeAufforderungLabel.setText(PROMPT_AUTH_ERFOLGREICH);
							btnGeldAbheben.setDisable(false);
							btnKontostandAnzeigen.setDisable(false);
							PauseTransition timer = new PauseTransition(Duration.seconds(1));
							timer.setOnFinished(event -> eingabeAufforderungLabel.setText(PROMPT_AKTION));
							timer.play();
							
						} else {
							if(pinCount >= 2) {
								eingabeAufforderungLabel.setText(PROMPT_KARTE_GESPERRT);
								Client.client.LockCard(kartennummerTemp);
								pinCount = 0;
								kartennummerTemp = 0;
							} else{
								eingabeAufforderungLabel.setText(PROMPT_PIN_FALSCH);
								pinCount++;
							}
						}
					}
				} else {
					eingabe += text;
				}
				eingabeLabel.setText(eingabe);
			}));

			eingabeLabel.setFont(new Font(24));
			eingabeLabel.setTextAlignment(TextAlignment.RIGHT);
			eingabeLabel.setPrefSize(Double.MAX_VALUE, 75);
			VBox vbox = new VBox();
			eingabeAufforderungLabel.setText(PROMPT_KARTENNUMMER);
			vbox.getChildren().addAll(eingabeAufforderungLabel, eingabeLabel);
			
			btnGeldAbheben = new Button("Geld abheben");
			btnGeldAbheben.setPrefSize(Double.MAX_VALUE, 75);
			btnGeldAbheben.setDisable(true);
			
			btnKontostandAnzeigen = new Button("Kontostand anzeigen");
			btnKontostandAnzeigen.setPrefSize(Double.MAX_VALUE, 75);
			btnKontostandAnzeigen.setDisable(true);

			VBox vboxAktionenButtons = new VBox();
			vboxAktionenButtons.setPadding(new Insets(10));
			vboxAktionenButtons.setAlignment(Pos.CENTER);
			vboxAktionenButtons.setStyle("-fx-spacing: 10;");
			vboxAktionenButtons.getChildren().addAll(btnGeldAbheben, btnKontostandAnzeigen);

			GridPane contentGrid = new GridPane();

			ColumnConstraints col1 = new ColumnConstraints();
			col1.setPercentWidth(30);
			ColumnConstraints col2 = new ColumnConstraints();
			col2.setPercentWidth(70);
			contentGrid.getColumnConstraints().addAll(col1, col2);
			
			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(100);
			contentGrid.getRowConstraints().add(row1);

			contentGrid.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
			contentGrid.add(vboxAktionenButtons, 0, 0);
			contentGrid.add(zahlenGrid, 1, 0);
			
			BorderPane border = new BorderPane();
			border.setTop(vbox);
			border.setCenter(contentGrid);

			// Client.client.sendAuth(111, 1111);
			
			// Button authButton = new Button("Auth");
			// authButton.setPrefSize(Double.MAX_VALUE, 75);
			// authButton.setOnAction(e -> 
			// 	Client.client.sendAuth(111, 1234)
			// );

			// border.setBottom(authButton);
			
			Scene scene = new Scene(border);
			scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
			primaryStage.setTitle(FENSTER_TITEL);
			primaryStage.setScene(scene);
			primaryStage.setWidth(0.5 * 1280);
			primaryStage.setHeight(0.5 * 720);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
