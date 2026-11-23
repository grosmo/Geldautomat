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

import geldautomat.data.SQLStatements;

public class DatabaseManager {
    private static Connection connection = null;

    public static Connection connect() throws SQLException {
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

    public static ResultSet selectAllFromKunden() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_ALL_FROM_KUNDEN);
        return statement.executeQuery();
    }

    public static ResultSet selectKunde(int cardNumber) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_KUNDE_BY_CARD_NUMBER);
        statement.setInt(1, cardNumber);
        return statement.executeQuery();
    }

    public static ResultSet selectBalanceByCardNumber(int cardNumber) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_SELECT_BALANCE_BY_CARD_NUMBER);
        statement.setInt(1, cardNumber);
        return statement.executeQuery();
    }

    public static void lockKundeByCardNumber(int cardNumber) throws SQLException {
        System.out.println("nummer " + cardNumber);
        PreparedStatement statement = connection.prepareStatement(SQLStatements.SQL_LOCK_KUNDE_BY_CARD_NUMBER);
        statement.setInt(1, cardNumber);
        statement.executeUpdate();
    }

    public static void insertTransaktion(int transactionId, int cardNumber, int amount, String status) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            SQLStatements.SQL_INSERT_TRANSFAKTION
        );
        statement.setInt(1, transactionId);
        statement.setInt(2, cardNumber);
        statement.setInt(3, amount);
        statement.setString(4, status);
        statement.executeUpdate();
    }

    static boolean approveWithdrawal(Connection connection, long id, int cardNumber, int amount) throws SQLException {
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

    static boolean finishWithdrawal(Connection connection, long id, int cardNumber, boolean dispensed) throws SQLException {
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

                PreparedStatement statement = connection.prepareStatement(eingabe);
                ResultSet result = statement.executeQuery();
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}