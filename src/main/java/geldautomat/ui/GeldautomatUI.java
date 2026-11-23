package geldautomat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import geldautomat.service.Client;
import geldautomat.service.StringExpressions;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GeldautomatUI extends Application {

	private enum Zustand {
		KARTENNUMMER,
		PIN,
		AKTION,
		KONTOSTAND,
		ABHEBEN
	}

	private Client client;
	private final ExecutorService netzwerk = Executors.newSingleThreadExecutor();

	Zustand zustand = Zustand.KARTENNUMMER;
	
	// global weil effectivly final in lambdas
	String eingabe = UIStringExpressions.LEER;
	String response = UIStringExpressions.LEER;
	int currentKartenNummer = 0;
	int pinCount = 0;

	GridPane contentGrid;

	Label eingabeAufforderungLabel;
	Label eingabeLabel;

	Button btnGeldAbheben;
	Button btnKontostandAnzeigen;
	Button btnKarteAuswerfen;
	
	@Override
	public void start(Stage primaryStage) {

		eingabeAufforderungLabel = new Label();
		// eingabeAufforderungLabel.setFont(new Font("Consola", 18, FontWeight.BOLD));
		eingabeAufforderungLabel.setStyle(
			"-fx-font-family: Consolas;" +
			"-fx-font-size: 18"
		);

		netzwerk.execute(() -> {
			try{
				client = new Client();
				Platform.runLater(() -> {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTENNUMMER);
				});
			} catch (IOException e) {
				Platform.runLater(() -> {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_SERVER_FEHLER);
				});
			}
		});
		try {
			GridPane zahlenGrid = new GridPane();
			zahlenGrid.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
			
			ColumnConstraints clmn = new ColumnConstraints();
			clmn.setPercentWidth(100 / 3);
			clmn.setFillWidth(true);
			zahlenGrid.getColumnConstraints().addAll(clmn, clmn, clmn);
			
			RowConstraints rw = new RowConstraints();
			rw.setPercentHeight(100 / 4);
			rw.setFillHeight(true);
			zahlenGrid.getRowConstraints().addAll(rw, rw, rw, rw);

			List<Button> zahlenButtons = new ArrayList<>();

			eingabeLabel = new Label(eingabe);
			eingabeLabel.setStyle(
				"-fx-font-family: Consolas;" +
				"-fx-font-size: 34px;" +
				"-fx-font-weight: bold;" +
				"-fx-alignment: center;"
			);
			eingabeLabel.setPrefSize(Double.MAX_VALUE, 60);

			btnGeldAbheben = new Button(UIStringExpressions.BUTTON_GELDABHEBEN);
			btnGeldAbheben.setPrefSize(Double.MAX_VALUE, 75);
			btnGeldAbheben.setDisable(true);
			btnGeldAbheben.setStyle(
						"-fx-font-size: 18px;"
					);
			btnGeldAbheben.setOnAction(e -> {
				eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_ABHEBEN);
				zustand = Zustand.ABHEBEN;
			});
			
			btnKontostandAnzeigen = new Button(UIStringExpressions.BUTTON_KONTOSTAND_ANZEIGEN);
			btnKontostandAnzeigen.setPrefSize(Double.MAX_VALUE, 75);
			btnKontostandAnzeigen.setDisable(true);
			btnKontostandAnzeigen.setStyle(
						"-fx-font-size: 18px;"
					);
			btnKontostandAnzeigen.setOnAction(e -> {
				netzwerk.execute(() -> {
					response = client.getBalance();
					String[] responses = response.split(" ");
					float kontostandEuro = Integer.parseInt(responses[1]) / 100.0f;
					Platform.runLater(() -> {
						eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KONTOSTAND.replace("?", String.valueOf(kontostandEuro)));
					});
				});
			});

			btnKarteAuswerfen = new Button(UIStringExpressions.BUTTON_KARTE_AUSWERFEN);
			btnKarteAuswerfen.setPrefSize(Double.MAX_VALUE, 75);
			btnKarteAuswerfen.setDisable(true);
			btnKarteAuswerfen.setStyle(
						"-fx-font-size: 18px;"
					);
			btnKarteAuswerfen.setOnAction(e -> {
				netzwerk.execute(() -> {
					client.quit();
				});
				
				currentKartenNummer = 0;
				pinCount = 0;
				btnGeldAbheben.setDisable(true);
				btnKontostandAnzeigen.setDisable(true);
				btnKarteAuswerfen.setDisable(true);
				eingabeLabel.setText(UIStringExpressions.LEER);
				eingabe = UIStringExpressions.LEER;
				zustand = Zustand.KARTENNUMMER;

				eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTE_AUSGEWORFEN);
				PauseTransition timer = new PauseTransition(Duration.seconds(2));
				contentGrid.setDisable(true);
				timer.setOnFinished(event -> {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTENNUMMER);
					contentGrid.setDisable(false);
				});
				timer.play();
			});

			for (int i = 0; i < UIStringExpressions.zahlenButtonsStrings.length; i++) {
				String[] row = UIStringExpressions.zahlenButtonsStrings[i];
				for (int j = 0; j < row.length; j++) {
					Button btn = new Button(row[j]);
					btn.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
					btn.setStyle(
						"-fx-font-size: 20px;" +
						"-fx-font-weight: bold;"
					);
					zahlenGrid.add(btn, j, i);
					zahlenButtons.add(btn);
				}
			}

			zahlenButtons.forEach(btn -> btn.setOnAction(e -> {
				if(zustand == Zustand.AKTION)
					return;
				String text = btn.getText();
				if (text.equals(UIStringExpressions.CLEAR)) {
					eingabe = UIStringExpressions.LEER;
					eingabeLabel.setText(UIStringExpressions.LEER);
				} else if (text.equals(UIStringExpressions.OK)) {
					return;
				} else {
					int maxLaenge = switch(zustand){
						case KARTENNUMMER -> 3;
						case PIN -> 4;
						case ABHEBEN -> 6;
						default -> Integer.MAX_VALUE;
					};
					if (eingabe.length() < maxLaenge) {
						eingabe += text;
					}
				}
				if(zustand == Zustand.PIN)
					eingabeLabel.setText("*".repeat(eingabe.length()));
				else
					eingabeLabel.setText(eingabe);
			}));

			Button okButtonTemp = zahlenButtons.stream()
				.filter(button -> UIStringExpressions.OK.equals(button.getText()))
				.findFirst()
				.orElse(null);

			if (okButtonTemp != null) {
				okButtonTemp.setOnAction(e -> {
					if (zustand.equals(Zustand.KARTENNUMMER)) {
						currentKartenNummer = Integer.parseInt(eingabe);
						eingabeLabel.setText(UIStringExpressions.LEER);
						eingabe = UIStringExpressions.LEER;
						eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_PIN);
						zustand = Zustand.PIN;
					} else if (zustand.equals(Zustand.PIN)) {
						onPinEingabe();
					} else if(zustand.equals(Zustand.ABHEBEN)) {
						onAbheben();
					}
				});
			}

			VBox vbox = new VBox();
			vbox.setPadding(new Insets(15));
			eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTENNUMMER);
			vbox.getChildren().addAll(eingabeAufforderungLabel, eingabeLabel);

			VBox vboxAktionenButtons = new VBox();
			vboxAktionenButtons.setPadding(new Insets(10));
			vboxAktionenButtons.setAlignment(Pos.CENTER);
			vboxAktionenButtons.setStyle(UIStringExpressions.STYLE_SPACING);
			vboxAktionenButtons.getChildren().addAll(btnGeldAbheben, btnKontostandAnzeigen, btnKarteAuswerfen);

			contentGrid = new GridPane();

			ColumnConstraints col1 = new ColumnConstraints();
			col1.setPercentWidth(40);
			ColumnConstraints col2 = new ColumnConstraints();
			col2.setPercentWidth(60);
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
			primaryStage.setTitle(UIStringExpressions.FENSTER_TITEL);
			primaryStage.setScene(scene);
			primaryStage.setWidth(0.5 * 1280);
			primaryStage.setHeight(0.5 * 720);
			primaryStage.setResizable(false);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onPinEingabe() {
		contentGrid.setDisable(true);
		int pinTemp = Integer.parseInt(eingabe);
		eingabeLabel.setText(UIStringExpressions.LEER);
		eingabe = UIStringExpressions.LEER;
		netzwerk.execute(() -> {
			response = client.sendAuth(currentKartenNummer, pinTemp);
			Platform.runLater(() -> {
				if(response.equals(StringExpressions.RESPONSE_OK)) {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_AUTH_ERFOLGREICH);
					
					btnGeldAbheben.setDisable(false);	
					btnKontostandAnzeigen.setDisable(false);
					btnKarteAuswerfen.setDisable(false);
					
					contentGrid.setDisable(true);
					PauseTransition timer = new PauseTransition(Duration.seconds(2));
					timer.setOnFinished(event -> {
						eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_AKTION);
						zustand = Zustand.AKTION;
						contentGrid.setDisable(false);
					});
					timer.play();
					
				} else if(response.equals(StringExpressions.RESPONSE_BLOCKED)) {
					client.quit();
					
					zustand = Zustand.KARTENNUMMER;
					eingabeLabel.setText(UIStringExpressions.LEER);
					eingabe = UIStringExpressions.LEER;
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTENNUMMER);

					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Fehler");
					alert.setHeaderText(null);
					alert.setContentText("Die Karte ist gesperrt!");
					alert.showAndWait();

				} else {
					if(pinCount >= 2) {
						eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTE_GESPERRT);
						eingabeAufforderungLabel.setStyle(UIStringExpressions.TEXT_STYLE_ROT);
						pinCount = 0;
						currentKartenNummer = 0;
						client.quit();
						eingabeLabel.setText(UIStringExpressions.LEER);
						eingabe = UIStringExpressions.LEER;
						zustand = Zustand.KARTENNUMMER;
						contentGrid.setDisable(true);
						PauseTransition timer = new PauseTransition(Duration.seconds(2));
						timer.setOnFinished(event -> {
							eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_KARTENNUMMER);
							eingabeAufforderungLabel.setStyle(UIStringExpressions.TEXT_STYLE_SCHWARZ);
							contentGrid.setDisable(false);
						});
						timer.play();
					} else{
						eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_PIN_FALSCH);
						pinCount++;
					}
				}
			});
		});
		contentGrid.setDisable(false);
	}

	private void onAbheben() {
		// cent in euro
		int betrag = Integer.parseInt(eingabe) * 100;
		long transactionId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
		eingabe = UIStringExpressions.LEER;
		eingabeLabel.setText(UIStringExpressions.LEER);
		contentGrid.setDisable(true);

		netzwerk.execute(() -> {
			String withdrawalResponse = client.withdraw(betrag, transactionId);
			Platform.runLater(() -> {
				String expectedApproval = StringExpressions.RESPONSE_OK + " "
					+ StringExpressions.STATUS_APPROVED + " " + transactionId;
				if(expectedApproval.equals(withdrawalResponse)) {
					// euro in cent umrechnen
					int betragEuro = betrag / 100;
					ButtonType ja = new ButtonType(UIStringExpressions.BUTTON_JA);
					ButtonType nein = new ButtonType(UIStringExpressions.BUTTON_NEIN);
					Alert dialog = new Alert(
						Alert.AlertType.CONFIRMATION,
						UIStringExpressions.PROMPT_GELDAUSGABE.replace("?", String.valueOf(betragEuro)) + "?",
						ja, nein);
					dialog.setHeaderText(null);
					boolean dispensed = dialog.showAndWait().orElse(nein) == ja;

					zustand = Zustand.AKTION;
					netzwerk.execute(() -> {
						String dispenseResponse = client.dispense(transactionId, dispensed);
						Platform.runLater(() -> {
							String expectedStatus = dispensed ? StringExpressions.STATUS_COMMITTED : StringExpressions.STATUS_CANCELLED;
							String expectedResponse = StringExpressions.RESPONSE_OK + " " + expectedStatus + " " + transactionId;
							if (!expectedResponse.equals(dispenseResponse)) {
								eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_ABHEBUNG_UNGEKLAERT);
								client.quit();
								return;
							}
							eingabeAufforderungLabel.setText(dispensed
								? UIStringExpressions.PROMPT_ABHEBUNG_ERFOLGREICH
								: UIStringExpressions.PROMPT_ABHEBUNG_ABGEBROCHEN);
							PauseTransition timer = new PauseTransition(Duration.seconds(2));
							timer.setOnFinished(event -> {
								eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_AKTION);
								contentGrid.setDisable(false);
							});
							timer.play();
						});
					});
				} else if ((StringExpressions.RESPONSE_ERROR + " " + StringExpressions.STATUS_DECLINED).equals(withdrawalResponse)) {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_ABHEBUNG_ABGELEHNT);
				} else {
					eingabeAufforderungLabel.setText(UIStringExpressions.PROMPT_ABHEBUNG_UNGEKLAERT);
				}
			});
		});
	}
}
