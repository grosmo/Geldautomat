package geldautomat.data;

public class Kunde {
    private int cardNumber;
    private int pin;
    private int balance;
    private boolean blocked;

    public Kunde(int cardNumber, int pin, int balance) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.balance = balance;
        this.blocked = false;
    }

    public int getCardNumber() {
        return cardNumber;
    }

    public int getPin() {
        return pin;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
    
    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked() {
        this.blocked = !blocked;
    }
}
