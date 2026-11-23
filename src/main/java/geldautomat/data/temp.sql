-- -- SQLite

-- DROP TABLE kunden;

-- CREATE TABLE kunden (
--     cardNumber INTEGER NOT NULL PRIMARY KEY,
--     pin INTEGER NOT NULL,
--     balance INTEGER NOT NULL,
--     blocked BOOLEAN NOT NULL
-- );

-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (111, 1111, 2500000, FALSE);
-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (222, 2222, -350000, FALSE);
-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (333, 3333, 15000, FALSE);
-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (444, 4444, 32425025, FALSE);
-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (555, 5555, 1313559, FALSE);
-- INSERT INTO kunden 
-- (cardNumber, pin, balance, blocked) 
-- VALUES (666, 6666, -1563546, TRUE);

-- CREATE TABLE transaktionen(
--     transactionId INTEGER NOT NULL PRIMARY KEY,
--     cardNumber INTEGER NOT NULL,
--     amount INTEGER NOT NULL,
--     status TEXT NOT NULL,
--     FOREIGN KEY (cardNumber) REFERENCES kunden(cardNumber)
-- );

-- UPDATE kunden SET blocked = FALSE WHERE cardNumber = 111;
-- UPDATE kunden SET blocked = FALSE WHERE cardNumber = 222;

-- DELETE FROM transaktionen;
-- DELETE FROM kunden;