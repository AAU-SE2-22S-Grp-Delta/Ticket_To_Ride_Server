package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.SendingThread;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SendingThreadTests {
    static Socket mockedSocket = Mockito.mock(Socket.class);

    static SendingThread sendingThread;

    @BeforeAll
    static void init() {
        try {
            sendingThread = new SendingThread(mockedSocket);
        } catch (Exception ignored) {
        }
    }

    @Test
    void testClass() {
        assertEquals(SendingThread.class, sendingThread.getClass());
    }

    @Test
    void testRun(){
        assertDoesNotThrow(() -> sendingThread.start());
    }

    @Test
    void testSendCommand(){
        assertDoesNotThrow(() -> sendingThread.sendCommand("test"));
    }
}
