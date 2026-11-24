package geldautomat.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import geldautomat.data.SQLStatements;

public class DatabaseManager {
    private static Connection connection = null;

    private static Connection connect() throws SQLException {
        if (connection == null) {
            try {
                Class.forName(StringExpressions.DB_INIT);
            } catch (ClassNotFoundException e) {
                throw new SQLException(StringExpressions.DB_ERROR_DRIVER_NOT_FOUND, e);
            }
            connection = DriverManager.getConnection(StringExpressions.DB_URL);
        }
        return connection;
    }

    static {
        try {
            connection = connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public record Kunde(int pin, boolean blocked) {}

    public static List<String> selectAllFromKunden() throws SQLException {
        List<String> kunden = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_ALL_FROM_KUNDEN);
             ResultSet result = statement.executeQuery()) {
            ResultSetMetaData metaData = result.getMetaData();
            while (result.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    if (i > 1) {
                        row.append(", ");
                    }
                    row.append(metaData.getColumnName(i)).append("=").append(result.getObject(i));
                }
                kunden.add(row.toString());
            }
        }
        return kunden;
    }

    public static Optional<Kunde> selectKunde(int cardNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_KUNDE_BY_CARD_NUMBER)) {
            statement.setInt(1, cardNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new Kunde(
                        result.getInt(StringExpressions.FIELD_PIN),
                        result.getBoolean(StringExpressions.FIELD_BLOCKED)));
                }
                return Optional.empty();
            }
        }
    }

    public static OptionalInt selectBalanceByCardNumber(int cardNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_BALANCE_BY_CARD_NUMBER)) {
            statement.setInt(1, cardNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return OptionalInt.of(result.getInt(StringExpressions.FIELD_BALANCE));
                }
                return OptionalInt.empty();
            }
        }
    }

    public static void lockKundeByCardNumber(int cardNumber) throws SQLException {
        System.out.println("nummer " + cardNumber);
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_LOCK_KUNDE_BY_CARD_NUMBER)) {
            statement.setInt(1, cardNumber);
            statement.executeUpdate();
        }
    }

    public static void insertTransaktion(int transactionId, int cardNumber, int amount, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_INSERT_TRANSFAKTION)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, cardNumber);
            statement.setInt(3, amount);
            statement.setString(4, status);
            statement.executeUpdate();
        }
    }

    static boolean approveWithdrawal(long id, int cardNumber, int amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_APPROVE_WITHDRAWAL)) {
            statement.setLong(1, id);
            statement.setInt(2, amount);
            statement.setString(3, StringExpressions.STATUS_APPROVED);
            statement.setInt(4, cardNumber);
            statement.setInt(5, amount);
            statement.setInt(6, cardNumber);
            statement.setString(7, StringExpressions.STATUS_APPROVED);
            // wenn kunde gesperrt oder nicht genug Guthaben -> 0 ansonsten -> 1 zeilen
            return statement.executeUpdate() == 1;
        }
    }

    static boolean finishWithdrawal(long id, int cardNumber, boolean dispensed) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException(StringExpressions.DB_TRANSACTION_ACTIVE);
        }
        connection.setAutoCommit(false);
        boolean transactionResolved = false;
        try {
            int amount;
            try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_PENDING_WITHDRAWAL)) {
                statement.setLong(1, id);
                statement.setInt(2, cardNumber);
                statement.setString(3, StringExpressions.STATUS_APPROVED);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        transactionResolved = true;
                        return false;
                    }
                    amount = result.getInt(StringExpressions.DB_TRANSACTION_FIELD_AMOUNT);
                }
            }
            if (dispensed) {
                try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_DEBIT_WITHDRAWAL)) {
                    statement.setInt(1, amount);
                    statement.setInt(2, cardNumber);
                    statement.setInt(3, amount);
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        transactionResolved = true;
                        return false;
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_FINISH_WITHDRAWAL)) {
                statement.setString(1, dispensed ? StringExpressions.STATUS_COMMITTED : StringExpressions.STATUS_CANCELLED);
                statement.setLong(2, id);
                statement.setInt(3, cardNumber);
                statement.setString(4, StringExpressions.STATUS_APPROVED);
                if (statement.executeUpdate() != 1) {
                    connection.rollback();
                    transactionResolved = true;
                    return false;
                }
            }
            connection.commit();
            transactionResolved = true;
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                transactionResolved = true;
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
                try {
                    connection.close();
                } catch (SQLException closeError) {
                    e.addSuppressed(closeError);
                }
            }
            throw e;
        } finally {
            if (transactionResolved) {
                connection.setAutoCommit(true);
            }
        }
    }

    public static void main(String[] args) {
        String eingabe = "";
        try {
            Connection connection = connect();
            while(!eingabe.equals("q")){
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    System.out.print("SQL-Befehl eingeben (q zum Beenden): ");
                    eingabe = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try (PreparedStatement statement = connection.prepareStatement(eingabe);
                     ResultSet result = statement.executeQuery()) {
                    ResultSetMetaData metaData = result.getMetaData();
                    while (result.next()) {
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            System.out.print(metaData.getColumnName(i) + "=" + result.getObject(i));
                            if (i < metaData.getColumnCount()) {
                                System.out.print(", ");
                            }
                        }
                        System.out.println();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}