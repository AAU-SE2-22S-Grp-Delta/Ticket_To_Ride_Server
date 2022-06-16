package at.aau.se2.tickettoride_server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MainTests {
    @Test
    void testMain() {
        assertDoesNotThrow(() -> {
            String[] args = new String[]{};
            Main.main(args);
        });
    }
}
