package geldautomat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testEncodeWithoutParameters() {
        Message message = new Message("BALANCE");

        assertEquals("BALANCE\r\n", message.encode());
    }

    @Test
    void testEncodeWithOneParameter() {
        Message message = new Message("WITHDRAW", "5000");

        assertEquals("WITHDRAW 5000\r\n", message.encode());
    }

    @Test
    void testEncodeWithMultipleParameters() {
        Message message = new Message("AUTH", "Karte123", "PIN_BLOCK_ABC");

        assertEquals("AUTH Karte123 PIN_BLOCK_ABC\r\n", message.encode());
    }

    @Test
    void testDecodeWithoutParameters() {
        Message decoder = new Message("TEST");

        Message result = decoder.decode("BALANCE");

        assertEquals("BALANCE\r\n", result.encode());
    }

    @Test
    void testDecodeWithMultipleParameters() {
        Message decoder = new Message("TEST");

        Message result = decoder.decode("WITHDRAW 5000 T17");

        assertEquals("WITHDRAW 5000 T17\r\n", result.encode());
    }

    @Test
    void testDecodeWithMultipleSpaces() {
        Message decoder = new Message("TEST");

        Message result = decoder.decode("WITHDRAW   5000    T17");

        assertEquals("WITHDRAW 5000 T17\r\n", result.encode());
    }

    @Test
    void testDecodeEmptyMessage() {
        Message decoder = new Message("TEST");

        assertThrows(
            IllegalArgumentException.class,
            () -> decoder.decode("")
        );
    }

    @Test
    void testDecodeOnlyWhitespace() {
        Message decoder = new Message("TEST");

        assertThrows(
            IllegalArgumentException.class,
            () -> decoder.decode("   ")
        );
    }
}
