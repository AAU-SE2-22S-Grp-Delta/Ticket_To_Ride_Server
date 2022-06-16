package at.aau.se2.tickettoride_server;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor<Logger> constructor = Logger.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
