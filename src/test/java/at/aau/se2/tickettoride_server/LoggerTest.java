package at.aau.se2.tickettoride_server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LoggerTest {
    @Test
    void testLoggerDebug() {
        assertDoesNotThrow(() -> Logger.debug("Test"));
    }

    @Test
    void testLoggerException() {
        assertDoesNotThrow(() -> Logger.exception("Test"));
    }

    @Test
    void testLoggerFatal() {
        assertDoesNotThrow(() -> Logger.fatal("Test"));
    }

    @Test
    void testLoggerLog() {
        assertDoesNotThrow(() -> Logger.log("Test"));
    }

    @Test
    void testLoggerVerbose() {
        assertDoesNotThrow(() -> Logger.verbose("Test"));
    }
}
