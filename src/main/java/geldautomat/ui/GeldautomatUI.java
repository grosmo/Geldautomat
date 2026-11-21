package geldautomat.ui;

import geldautomat.service.Client;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class GeldautomatUI extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			Client.main(new String[]{});
			HBox hbox = new HBox(10);
			hbox.getChildren().addAll();
			BorderPane border = new BorderPane();
			border.setCenter(hbox);
			Scene scene = new Scene(border);
			scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
			primaryStage.setTitle("Geldautomat");
			primaryStage.setScene(scene);
			primaryStage.setWidth(0.5 * 1280);
			primaryStage.setHeight(0.5 * 720);
			primaryStage.show();


		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
