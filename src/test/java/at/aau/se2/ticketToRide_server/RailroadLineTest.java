package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;
import at.aau.se2.ticketToRide_server.dataStructures.MapColor;
import at.aau.se2.ticketToRide_server.dataStructures.RailroadLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RailroadLineTest {
    Destination dest1;
    Destination dest2;
    Destination dest3;
    Destination dest4;
    RailroadLine r1;
    RailroadLine r2;

    @BeforeEach
    public void init() {
        dest1 = new Destination("testdest1");
        dest2 = new Destination("testdest2");
        dest3 = new Destination("testdest3");
        dest4 = new Destination("testdest4");
        r1 = new RailroadLine(dest1, dest2, MapColor.BLUE, 3);
    }

    @Test
    public void testConnectionFirstNull() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(null, dest2, MapColor.BLUE, 3));
    }

    @Test
    public void testConnectionSecondNull() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(dest1, null, MapColor.BLUE, 3));
    }

    @Test
    public void testConnectionSameDest() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(dest1, dest1, MapColor.BLUE, 3));
    }

    @Test
    public void testDestEquals() {
        r1 = new RailroadLine(dest1, dest2, MapColor.BLUE, 3);
        assertEquals(r1, new RailroadLine(dest2, dest1, MapColor.BLUE, 3));
    }

    @Test
    public void testGetters() {
        assertEquals(MapColor.BLUE, r1.getColor());
        assertEquals(dest1, r1.getDestination1());
        assertEquals(dest2, r1.getDestination2());
        assertEquals(3, r1.getDistance());
        assertNull(r1.getOwner());
    }

    @Test
    public void testEquals() {
        assertEquals(r1, new RailroadLine(dest1, dest2, MapColor.BLUE, 3));
        assertEquals(r1, new RailroadLine(dest2, dest1, MapColor.BLUE, 3));
        assertNotEquals(r1, new RailroadLine(dest1, dest3, MapColor.BLUE, 3));
    }
}
