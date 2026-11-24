package geldautomat.service;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;

class ServerService {
    private Long pendingId;
    private int previousCardNumber;
    private int anmeldeVersuche;

    // TODO -> offene Transaktionen
    protected boolean hasPending() {
        return pendingId != null;
    }

    // TODO checken ob noch offene Überweisungen da sind
    protected Message authenticate(int cardNumber, int pin, String[] params) throws SQLException {
        // Message reply = null;
        // if(serverService.hasPending()) {
        //     reply = new Message(StringExpressions.RESPONSE_ERROR);
        //     break;
        // }
        boolean authenticated = false;
        try {
            Optional<DatabaseManager.Kunde> kunde = DatabaseManager.selectKunde(cardNumber);
            if (kunde.isPresent() && kunde.get().blocked())
                return new Message(StringExpressions.RESPONSE_BLOCKED);
            else if (kunde.isPresent() && kunde.get().pin() == pin) {
                authenticated = true;
                // currentCardNumber = result.getInt(StringExpressions.FIELD_KARTENNUMMER);
            } else {
                previousCardNumber = cardNumber;
                anmeldeVersuche++;
                if (anmeldeVersuche >= 3) {
                    try {
                        DatabaseManager.lockKundeByCardNumber(previousCardNumber);
                    } catch (SQLException e) {
                        throw new SQLException(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
        if (!authenticated) 
            return new Message(StringExpressions.RESPONSE_ERROR);
        return new Message(StringExpressions.RESPONSE_OK);
    }

    protected Message getBalance(Integer cardNumber) throws SQLException {
        OptionalInt balance = DatabaseManager.selectBalanceByCardNumber(cardNumber);
        if (balance.isPresent()) {
            return new Message(StringExpressions.RESPONSE_OK, Integer.toString(balance.getAsInt()));
        }
        return new Message(StringExpressions.RESPONSE_ERROR);
    }
    
    protected Message withdraw(Integer cardNumber, String[] params) throws SQLException {
        if (cardNumber == null || hasPending() || params.length != 2) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        try {
            int amount = Integer.parseInt(params[0]);
            long id = Long.parseLong(params[1]);
            if (amount <= 0 || id <= 0 || !Long.toString(id).equals(params[1])) {
                return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
            }
            if (!DatabaseManager.approveWithdrawal(id, cardNumber, amount)) {
                return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
            }
            pendingId = id;
            return new Message(StringExpressions.RESPONSE_OK, StringExpressions.STATUS_APPROVED, params[1]);
        } catch (NumberFormatException e) {
            return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
        }
    }

    protected Message dispense(Integer cardNumber, String[] params) throws SQLException {
        if (cardNumber == null || pendingId == null || params.length != 2
                || !pendingId.toString().equals(params[0])) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        boolean dispensed = params[1].equals(StringExpressions.RESPONSE_OK);
        if (!dispensed && !params[1].equals(StringExpressions.STATUS_FAILED)) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        if (!DatabaseManager.finishWithdrawal(pendingId, cardNumber, dispensed)) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        String id = pendingId.toString();
        pendingId = null;
        return new Message(StringExpressions.RESPONSE_OK,
                dispensed ? StringExpressions.STATUS_COMMITTED : StringExpressions.STATUS_CANCELLED, id);
    }

    protected Message lock(Integer cardNumber, String[] params) throws SQLException {
        try {
            DatabaseManager.lockKundeByCardNumber(cardNumber);
        } catch (SQLException e) {
            throw e;
        }
        return new Message(StringExpressions.RESPONSE_OK);
    }
}