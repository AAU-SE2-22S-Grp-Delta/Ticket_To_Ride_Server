package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.datastructures.Destination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DestinationTest {
    static Destination dest1;
    static Destination dest2;

    @BeforeAll
    static void init() {
        dest1 = new Destination("DestinationTest1");
        dest2 = new Destination("DestinationTest2");
    }

    @Test
    void testSetNameNull() {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName(null));
    }

    @Test
    void testSetNameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName(""));
    }

    @Test
    void testGetters() {
        assertEquals("DestinationTest1", dest1.getName());
    }

    @Test
    void testSetName() {
        dest1.setName("DestinationTestUpdate1");
        assertEquals("DestinationTestUpdate1", dest1.getName());
    }

    @Test
    void testSetNameTaken() {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName(null));
    }
}
