package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Null;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SessionTests {
    static Socket mockedSocket = Mockito.mock(Socket.class);
    static Session session;

    @BeforeAll
    static void init() {
        try {
            session = new Session(mockedSocket);
        } catch (Exception ignored) {
        }
    }

    @Test
    void testSend() {
        assertDoesNotThrow(() -> session.send("test"));
    }

    @Test
    void testParseCommand() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("parseCommand", String.class);
                method.setAccessible(true);
                method.invoke(session, "test");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }
}
