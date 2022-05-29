package at.aau.se2.ticketToRide_server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DestinationTest {
    Destination dest1;
    Destination dest2;

    @BeforeEach
    public void init() {
        dest1 = new Destination("testdest1");
        dest2 = new Destination("testdest2");
    }

    @Test
    public void testSetNameNull() {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName(null));
    }

    @Test
    public void testSetNameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName(""));
    }

    @Test
    public void testGetters() {
        assertEquals("testdest1", dest1.getName());
    }
}
