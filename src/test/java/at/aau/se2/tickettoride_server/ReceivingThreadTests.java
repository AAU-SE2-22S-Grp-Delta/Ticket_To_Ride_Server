package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.ReceivingThread;
import at.aau.se2.tickettoride_server.server.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceivingThreadTests {
    static Socket mockedSocket = Mockito.mock(Socket.class);
    static Session mockedSession = Mockito.mock(Session.class);

    static ReceivingThread receivingThread;

    @BeforeAll
    static void init() {
        try {
            receivingThread = new ReceivingThread(mockedSocket, mockedSession);
        } catch (Exception ignored) {
        }
    }

    @Test
    void testClass() {
        assertEquals(ReceivingThread.class, receivingThread.getClass());
    }

    @Test
    void testRun(){
        assertDoesNotThrow(() -> receivingThread.start());
    }
}
