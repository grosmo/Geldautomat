package geldautomat.service;

import java.sql.Connection;
import java.sql.SQLException;

import geldautomat.data.StringExpressions;

class WithdrawalSession {
    private Long pendingId;

    boolean hasPending() {
        return pendingId != null;
    }

    Message withdraw(Connection connection, Integer cardNumber, String[] params) throws SQLException {
        if (cardNumber == null || hasPending() || params.length != 2) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        try {
            int amount = Integer.parseInt(params[0]);
            long id = Long.parseLong(params[1]);
            if (amount <= 0 || id <= 0 || !Long.toString(id).equals(params[1])) {
                return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
            }
            if (!DatabaseManager.approveWithdrawal(connection, id, cardNumber, amount)) {
                return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
            }
            pendingId = id;
            return new Message(StringExpressions.RESPONSE_OK, StringExpressions.STATUS_APPROVED, params[1]);
        } catch (NumberFormatException e) {
            return new Message(StringExpressions.RESPONSE_ERROR, StringExpressions.STATUS_DECLINED);
        }
    }

    Message dispense(Connection connection, Integer cardNumber, String[] params) throws SQLException {
        if (cardNumber == null || pendingId == null || params.length != 2
                || !pendingId.toString().equals(params[0])) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        boolean dispensed = params[1].equals(StringExpressions.RESPONSE_OK);
        if (!dispensed && !params[1].equals(StringExpressions.STATUS_FAILED)) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        if (!DatabaseManager.finishWithdrawal(connection, pendingId, cardNumber, dispensed)) {
            return new Message(StringExpressions.RESPONSE_ERROR);
        }
        String id = pendingId.toString();
        pendingId = null;
        return new Message(StringExpressions.RESPONSE_OK,
                dispensed ? StringExpressions.STATUS_COMMITTED : StringExpressions.STATUS_CANCELLED, id);
    }
}
