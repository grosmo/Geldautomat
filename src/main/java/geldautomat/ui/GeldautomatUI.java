package geldautomat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import geldautomat.service.Client;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
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
	private static final String FENSTER_TITEL = "Geldautomat";
	private static final String PROMPT_AKTION = "Was wollen Sie tun?";
	private static final String PROMPT_ABHEBEN = "Betrag eingeben:";
	
	private Client client;
	private final ExecutorService netzwerk = Executors.newSingleThreadExecutor();

	private final String[][] zahlenButtonsStrings = {
		{"1", "2", "3"},
		{"4", "5", "6"},
		{"7", "8", "9"},
		{"0", OK, CLEAR}
	};

	String eingabe = "";
	String response = "";
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
		netzwerk.execute(() -> {
			try{
				client = new Client();
				Platform.runLater(() -> {
					eingabeAufforderungLabel.setText(PROMPT_KARTENNUMMER);
				});
			} catch (IOException e) {
				Platform.runLater(() -> {
					eingabeAufforderungLabel.setText("Fehler beim Verbinden mit dem Server.");
				});
			}
		});
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

			eingabeAufforderungLabel.setFont(new Font(18));

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
						netzwerk.execute(() -> {
							response = client.sendAuth(kartennummerTemp, pinTemp);
							Platform.runLater(() -> {
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
										eingabeAufforderungLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; ");
										netzwerk.execute(() -> {
											client.LockCard(kartennummerTemp);
										});
										pinCount = 0;
										kartennummerTemp = 0;
										PauseTransition timer = new PauseTransition(Duration.seconds(4));
										timer.setOnFinished(event -> {
											eingabeAufforderungLabel.setText(PROMPT_KARTENNUMMER);
											eingabeAufforderungLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal; ");
										});
										timer.play();
									} else{
										eingabeAufforderungLabel.setText(PROMPT_PIN_FALSCH);
										pinCount++;
									}
								}
							});
						});
					} else if(eingabeAufforderungLabel.getText().equals(PROMPT_ABHEBEN)) {
						int betrag = Integer.parseInt(eingabe);
						eingabe = LEER;
						
						netzwerk.execute(() -> {
							response = client.withdraw(betrag);
							Platform.runLater(() -> {
								String[] responses = response.split(" ");
								if(responses[0].equals(OK)) {
									float betragEuro = betrag / 100.0f;
									eingabeAufforderungLabel.setText(betragEuro + " € befinden sich im CD Laufwerk!");
								} else {
									eingabeAufforderungLabel.setText("Fehler beim Abheben");
								}
							});
						});
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
			btnGeldAbheben.setOnAction(e -> {
				eingabeAufforderungLabel.setText(PROMPT_ABHEBEN);
			});
			
			btnKontostandAnzeigen = new Button("Kontostand anzeigen");
			btnKontostandAnzeigen.setPrefSize(Double.MAX_VALUE, 75);
			btnKontostandAnzeigen.setDisable(true);
			btnKontostandAnzeigen.setOnAction(e -> {
				netzwerk.execute(() -> {
					response = client.getBalance();
					String[] responses = response.split(" ");
					float kontostandEuro = Integer.parseInt(responses[1]) / 100.0f;
					Platform.runLater(() -> {
						eingabeAufforderungLabel.setText("Kontostand: " + kontostandEuro + " €");
					});
				});
			});

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

			Scene scene = new Scene(border);
			primaryStage.setTitle(FENSTER_TITEL);
			primaryStage.setScene(scene);
			primaryStage.setWidth(0.5 * 1280);
			primaryStage.setHeight(0.5 * 720);
			primaryStage.setResizable(false);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
