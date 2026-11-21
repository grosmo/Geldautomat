package geldautomat;

import geldautomat.service.Server;
import geldautomat.ui.GeldautomatUI;

public class GeldautomatApp {

	public static void main(String[] args) {

		Thread serverThread = new Thread(() -> {
			try {
				Server.main(new String[]{});
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		serverThread.start();
		GeldautomatUI.launch(GeldautomatUI.class, args);
	}
}
