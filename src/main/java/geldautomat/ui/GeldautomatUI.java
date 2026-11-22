package geldautomat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import geldautomat.data.StringExpressions;
import geldautomat.service.Client;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GeldautomatUI extends Application {
	private Client client;
	private final ExecutorService netzwerk = Executors.newSingleThreadExecutor();

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
					eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTENNUMMER);
				});
			} catch (IOException e) {
				Platform.runLater(() -> {
					eingabeAufforderungLabel.setText(StringExpressions.PROMPT_SERVER_FEHLER);
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
			for (int i = 0; i < StringExpressions.zahlenButtonsStrings.length; i++) {
				String[] row = StringExpressions.zahlenButtonsStrings[i];
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
				if (text.equals(StringExpressions.CLEAR)) {
					eingabe = StringExpressions.LEER;
				} else if (text.equals(StringExpressions.OK)) {
					if (eingabeAufforderungLabel.getText().equals(StringExpressions.PROMPT_KARTENNUMMER)) {
						kartennummerTemp = Integer.parseInt(eingabe);
						eingabeAufforderungLabel.setText(StringExpressions.PROMPT_PIN);
						eingabe = StringExpressions.LEER;
					} else if (eingabeAufforderungLabel.getText().equals(StringExpressions.PROMPT_PIN) 
						|| eingabeAufforderungLabel.getText().equals(StringExpressions.PROMPT_PIN_FALSCH)) {
						pinTemp = Integer.parseInt(eingabe);
						eingabe = StringExpressions.LEER;
						netzwerk.execute(() -> {
							response = client.sendAuth(kartennummerTemp, pinTemp);
							Platform.runLater(() -> {
								if(response.equals(StringExpressions.OK)) {
									eingabeAufforderungLabel.setText(StringExpressions.PROMPT_AUTH_ERFOLGREICH);
									btnGeldAbheben.setDisable(false);
									btnKontostandAnzeigen.setDisable(false);
									PauseTransition timer = new PauseTransition(Duration.seconds(1));
									timer.setOnFinished(event -> eingabeAufforderungLabel.setText(StringExpressions.PROMPT_AKTION));
									timer.play();
									
								} else {
									if(pinCount >= 2) {
										eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTE_GESPERRT);
										eingabeAufforderungLabel.setStyle(StringExpressions.TEXT_STYLE_ROT);
										netzwerk.execute(() -> {
											client.LockCard(kartennummerTemp);
										});
										pinCount = 0;
										kartennummerTemp = 0;
										PauseTransition timer = new PauseTransition(Duration.seconds(4));
										timer.setOnFinished(event -> {
											eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTENNUMMER);
											eingabeAufforderungLabel.setStyle(StringExpressions.TEXT_STYLE_SCHWARZ);
										});
										timer.play();
									} else{
										eingabeAufforderungLabel.setText(StringExpressions.PROMPT_PIN_FALSCH);
										pinCount++;
									}
								}
							});
						});
					} else if(eingabeAufforderungLabel.getText().equals(StringExpressions.PROMPT_ABHEBEN)) {
						int betrag;
						try {
							betrag = Integer.parseInt(eingabe);
							if (betrag <= 0) {
								throw new NumberFormatException("Betrag muss positiv sein");
							}
						} catch (NumberFormatException ex) {
							eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBUNG_ABGELEHNT);
							eingabe = StringExpressions.LEER;
							eingabeLabel.setText(eingabe);
							return;
						}
						eingabe = StringExpressions.LEER;
						long transactionId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
						btnGeldAbheben.setDisable(true);
						btnKontostandAnzeigen.setDisable(true);
						eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBUNG_LAEUFT);

						netzwerk.execute(() -> {
							String withdrawalResponse = client == null ? null : client.withdraw(betrag, transactionId);
							Platform.runLater(() -> {
								String expectedApproval = StringExpressions.RESPONSE_OK + " "
									+ StringExpressions.STATUS_APPROVED + " " + transactionId;
								if(expectedApproval.equals(withdrawalResponse)) {
									float betragEuro = betrag / 100.0f;

									ButtonType ja = new ButtonType(StringExpressions.BUTTON_JA);
									ButtonType nein = new ButtonType(StringExpressions.BUTTON_NEIN);
									Alert dialog = new Alert(
										Alert.AlertType.CONFIRMATION,
										StringExpressions.PROMPT_GELDAUSGABE.replace("?", String.valueOf(betragEuro)),
										ja, nein);
									dialog.setHeaderText(null);
									boolean dispensed = dialog.showAndWait().orElse(nein) == ja;
									kartennummerTemp = 0;
									pinTemp = 0;
									pinCount = 0;
									netzwerk.execute(() -> {
										String dispenseResponse = client.dispense(transactionId, dispensed);
										Platform.runLater(() -> {
											String expectedStatus = dispensed ? StringExpressions.STATUS_COMMITTED : StringExpressions.STATUS_CANCELLED;
											String expectedResponse = StringExpressions.RESPONSE_OK + " " + expectedStatus + " " + transactionId;
											if (!expectedResponse.equals(dispenseResponse)) {
												eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBUNG_UNGEKLAERT + " ID: " + transactionId);
												return;
											}
											eingabeAufforderungLabel.setText(dispensed
												? StringExpressions.PROMPT_ABHEBUNG_ERFOLGREICH
												: StringExpressions.PROMPT_ABHEBUNG_ABGEBROCHEN);
											PauseTransition timer = new PauseTransition(Duration.seconds(3));
											timer.setOnFinished(event -> eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTENNUMMER));
											timer.play();
										});
									});
								} else if ((StringExpressions.RESPONSE_ERROR + " " + StringExpressions.STATUS_DECLINED).equals(withdrawalResponse)) {
									eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBUNG_ABGELEHNT);
									btnGeldAbheben.setDisable(false);
									btnKontostandAnzeigen.setDisable(false);
								} else {
									eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBUNG_UNGEKLAERT + " ID: " + transactionId);
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
			eingabeLabel.setPrefSize(Double.MAX_VALUE, 60);
			VBox vbox = new VBox();
			vbox.setPadding(new Insets(15));
			eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTENNUMMER);
			vbox.getChildren().addAll(eingabeAufforderungLabel, eingabeLabel);
			
			btnGeldAbheben = new Button(StringExpressions.BUTTON_GELDABHEBEN);
			btnGeldAbheben.setPrefSize(Double.MAX_VALUE, 75);
			btnGeldAbheben.setDisable(true);
			btnGeldAbheben.setOnAction(e -> {
				eingabeAufforderungLabel.setText(StringExpressions.PROMPT_ABHEBEN);
			});
			
			btnKontostandAnzeigen = new Button(StringExpressions.BUTTON_KONTOSTAND_ANZEIGEN);
			btnKontostandAnzeigen.setPrefSize(Double.MAX_VALUE, 75);
			btnKontostandAnzeigen.setDisable(true);
			btnKontostandAnzeigen.setOnAction(e -> {
				netzwerk.execute(() -> {
					response = client.getBalance();
					String[] responses = response.split(" ");
					float kontostandEuro = Integer.parseInt(responses[1]) / 100.0f;
					Platform.runLater(() -> {
						eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KONTOSTAND.replace("?", String.valueOf(kontostandEuro)));
					});
				});
			});

			Button btnKarteAuswerfen = new Button(StringExpressions.BUTTON_KARTE_AUSWERFEN);
			btnKarteAuswerfen.setPrefSize(Double.MAX_VALUE, 75);
			btnKarteAuswerfen.setOnAction(e -> {
				kartennummerTemp = 0;
				pinTemp = 0;
				pinCount = 0;
				eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTE_AUSGEWORFEN);
				PauseTransition timer = new PauseTransition(Duration.seconds(2));
				timer.setOnFinished(event -> {
					eingabeAufforderungLabel.setText(StringExpressions.PROMPT_KARTENNUMMER);
				});
				timer.play();
			});


			VBox vboxAktionenButtons = new VBox();
			vboxAktionenButtons.setPadding(new Insets(10));
			vboxAktionenButtons.setAlignment(Pos.CENTER);
			vboxAktionenButtons.setStyle(StringExpressions.STYLE_SPACING);
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
			primaryStage.setTitle(StringExpressions.FENSTER_TITEL);
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
