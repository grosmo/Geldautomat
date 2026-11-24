package geldautomat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {
    private Connection connection;
    private Connection originalConnection;
    private Field connectionField;
    private int openedStatements;
    private int closedStatements;
    private int openedResults;
    private int closedResults;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE kunden (cardNumber INTEGER PRIMARY KEY, pin INTEGER NOT NULL, balance INTEGER NOT NULL, blocked BOOLEAN NOT NULL)");
            statement.execute("CREATE TABLE transaktionen (transactionId INTEGER PRIMARY KEY, cardNumber INTEGER NOT NULL, amount INTEGER NOT NULL, status TEXT NOT NULL)");
            statement.execute("INSERT INTO kunden VALUES (111, 1234, 500, 0), (222, 4321, 0, 1)");
        }
        connectionField = DatabaseManager.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        originalConnection = (Connection) connectionField.get(null);
        connectionField.set(null, trackingConnection());
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionField.set(null, originalConnection);
        connection.close();
    }

    @Test
    void readsCustomersAndBalancesWithoutLeavingResourcesOpen() throws SQLException {
        assertEquals(new DatabaseManager.Kunde(1234, false), DatabaseManager.selectKunde(111).orElseThrow());
        assertFalse(DatabaseManager.selectKunde(999).isPresent());
        assertEquals(500, DatabaseManager.selectBalanceByCardNumber(111).orElseThrow());
        assertEquals(0, DatabaseManager.selectBalanceByCardNumber(222).orElseThrow());
        assertFalse(DatabaseManager.selectBalanceByCardNumber(999).isPresent());
        assertEquals(2, DatabaseManager.selectAllFromKunden().size());

        ServerService service = new ServerService();
        assertEquals("OK\r\n", service.authenticate(111, 1234, new String[0]).encode());
        assertEquals("BLOCKED\r\n", service.authenticate(222, 4321, new String[0]).encode());
        assertEquals("ERROR\r\n", service.getBalance(999).encode());
        assertResourcesClosed();
    }

    @Test
    void closesUpdateStatementsOnSuccessAndFailure() throws SQLException {
        DatabaseManager.lockKundeByCardNumber(111);
        assertTrue(DatabaseManager.selectKunde(111).orElseThrow().blocked());
        DatabaseManager.insertTransaktion(1, 111, 100, "APPROVED");
        assertThrows(SQLException.class, () -> DatabaseManager.insertTransaktion(1, 111, 100, "APPROVED"));
        assertResourcesClosed();
    }

    private void assertResourcesClosed() {
        assertEquals(openedStatements, closedStatements);
        assertEquals(openedResults, closedResults);
        assertTrue(openedStatements > 0);
    }

    private Connection trackingConnection() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                Object value = invoke(connection, method, args);
                if (method.getName().equals("prepareStatement")) {
                    openedStatements++;
                    return trackingStatement((PreparedStatement) value);
                }
                return value;
            });
    }

    private PreparedStatement trackingStatement(PreparedStatement statement) {
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
            new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                Object value = invoke(statement, method, args);
                if (method.getName().equals("close")) {
                    closedStatements++;
                } else if (method.getName().equals("executeQuery")) {
                    openedResults++;
                    return trackingResult((ResultSet) value);
                }
                return value;
            });
    }

    private ResultSet trackingResult(ResultSet result) {
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> {
                Object value = invoke(result, method, args);
                if (method.getName().equals("close")) {
                    closedResults++;
                }
                return value;
            });
    }

    private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
