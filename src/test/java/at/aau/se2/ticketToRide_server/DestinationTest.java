package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DestinationTest {
    static Destination dest1;
    static Destination dest2;

    @BeforeAll
    public static void init() {
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

    @Test
    public void testSetName()
    {
        dest1.setName("testdestupdate1");
        assertEquals("testdestupdate1", dest1.getName());    }

    @Test
    void testSetNameTaken()
    {
        assertThrows(IllegalArgumentException.class, () -> dest1.setName("testdest2"));
    }
}

