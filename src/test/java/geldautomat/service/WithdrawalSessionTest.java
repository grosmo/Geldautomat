package geldautomat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithdrawalSessionTest {
    private Connection connection;
    private WithdrawalSession session;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE kunden (cardNumber INTEGER PRIMARY KEY, balance INTEGER NOT NULL, blocked BOOLEAN NOT NULL)");
            statement.execute("CREATE TABLE transaktionen (transactionId INTEGER PRIMARY KEY, cardNumber INTEGER NOT NULL, amount INTEGER NOT NULL, status TEXT NOT NULL)");
            statement.execute("INSERT INTO kunden VALUES (111, 1000, 0)");
        }
        session = new WithdrawalSession();
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void approvedWithdrawalIsCommittedOnce() throws SQLException {
        assertEquals("OK APPROVED 123\r\n", session.withdraw(connection, 111, new String[] {"300", "123"}).encode());
        assertEquals(1000, balance());
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT amount FROM transaktionen WHERE transactionId = 123")) {
            result.next();
            assertEquals(300, result.getInt(1));
        }
        assertEquals("OK COMMITTED 123\r\n", session.dispense(connection, 111, new String[] {"123", "OK"}).encode());
        assertEquals(700, balance());
        assertEquals("COMMITTED", status(123));
        assertEquals("ERROR\r\n", session.dispense(connection, 111, new String[] {"123", "OK"}).encode());
        assertEquals(700, balance());
    }

    @Test
    void failedDispenseCancelsWithoutDebiting() throws SQLException {
        session.withdraw(connection, 111, new String[] {"300", "123"});
        assertEquals("OK CANCELLED 123\r\n", session.dispense(connection, 111, new String[] {"123", "FAILED"}).encode());
        assertEquals(1000, balance());
        assertEquals("CANCELLED", status(123));
    }

    @Test
    void invalidRequestsDoNotChangePendingTransaction() throws SQLException {
        assertEquals("ERROR\r\n", session.withdraw(connection, null, new String[] {"300", "123"}).encode());
        assertEquals("ERROR DECLINED\r\n", session.withdraw(connection, 111, new String[] {"-1", "123"}).encode());
        assertEquals("ERROR DECLINED\r\n", session.withdraw(connection, 111, new String[] {"1001", "123"}).encode());
        assertEquals("ERROR DECLINED\r\n", session.withdraw(connection, 111, new String[] {"300", "T123"}).encode());
        assertEquals("ERROR DECLINED\r\n", session.withdraw(connection, 111, new String[] {"300", "0"}).encode());
        assertEquals("ERROR\r\n", session.withdraw(connection, 111, new String[] {"300"}).encode());
        assertEquals("ERROR\r\n", session.dispense(connection, 111, new String[] {"123", "OK"}).encode());
        session.withdraw(connection, 111, new String[] {"300", "123"});
        assertEquals("ERROR\r\n", session.withdraw(connection, 111, new String[] {"100", "124"}).encode());
        assertEquals("ERROR DECLINED\r\n", new WithdrawalSession().withdraw(connection, 111, new String[] {"100", "124"}).encode());
        assertEquals("ERROR\r\n", session.dispense(connection, 111, new String[] {"124", "OK"}).encode());
        assertEquals("ERROR\r\n", session.dispense(connection, 222, new String[] {"123", "OK"}).encode());
        assertEquals("ERROR\r\n", session.dispense(connection, 111, new String[] {"123", "UNKNOWN"}).encode());
        assertEquals("APPROVED", status(123));
        assertEquals(1000, balance());
        assertEquals("OK COMMITTED 123\r\n", session.dispense(connection, 111, new String[] {"123", "OK"}).encode());
    }

    @Test
    void disconnectedSessionCannotCompleteAnotherSessionsPendingWithdrawal() throws SQLException {
        session.withdraw(connection, 111, new String[] {"300", "123"});
        WithdrawalSession newSession = new WithdrawalSession();
        assertEquals("ERROR\r\n", newSession.dispense(connection, 111, new String[] {"123", "OK"}).encode());
        assertEquals("ERROR DECLINED\r\n", newSession.withdraw(connection, 111, new String[] {"300", "124"}).encode());
        assertEquals("APPROVED", status(123));
        assertEquals(1000, balance());
    }

    @Test
    void blockedCardAndReusedIdAreRejected() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE kunden SET blocked = 1");
        }
        assertEquals("ERROR DECLINED\r\n", session.withdraw(connection, 111, new String[] {"300", "123"}).encode());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE kunden SET blocked = 0");
        }
        session.withdraw(connection, 111, new String[] {"300", "123"});
        session.dispense(connection, 111, new String[] {"123", "FAILED"});
        assertThrows(SQLException.class, () -> session.withdraw(connection, 111, new String[] {"300", "123"}));
        assertEquals("CANCELLED", status(123));
        assertEquals(1000, balance());
    }

    @Test
    void databaseFailureRollsBackDebitAndKeepsPending() throws SQLException {
        session.withdraw(connection, 111, new String[] {"300", "123"});
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_update BEFORE UPDATE ON transaktionen BEGIN SELECT RAISE(ABORT, 'test'); END");
        }
        assertThrows(SQLException.class, () -> session.dispense(connection, 111, new String[] {"123", "OK"}));
        assertEquals(1000, balance());
        assertEquals("APPROVED", status(123));
    }

    private int balance() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT balance FROM kunden WHERE cardNumber = 111")) {
            result.next();
            return result.getInt(1);
        }
    }

    private String status(long id) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT status FROM transaktionen WHERE transactionId = " + id)) {
            result.next();
            return result.getString(1);
        }
    }
}
