package geldautomat.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:Project/data/data.db";

    public static Connection connect() throws SQLException {
    try {
        Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
        throw new SQLException("SQLite-Treiber wurde nicht gefunden!", e);
    }

    return DriverManager.getConnection(URL);
}
    public static void main(String[] args) {
    String sql = "SELECT name FROM customers";

    try (Connection connection = connect();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {

        while (result.next()) {
            String name = result.getString("name");
            System.out.println("Kunde gefunden: " + name);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
}