package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.datastructures.Destination;
import at.aau.se2.tickettoride_server.datastructures.MapColor;
import at.aau.se2.tickettoride_server.datastructures.RailroadLine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RailroadLineTest {
    static Destination dest1;
    static Destination dest2;
    static Destination dest3;
    static Destination dest4;
    static RailroadLine r1;
    static RailroadLine r2;

    @BeforeAll
    static void init() {
        dest1 = new Destination("RailroadLineTest1");
        dest2 = new Destination("RailroadLineTest2");
        dest3 = new Destination("RailroadLineTest3");
        dest4 = new Destination("RailroadLineTest4");
        r1 = new RailroadLine(dest1, dest2, MapColor.BLUE, 3);
    }

    @Test
    void testConnectionFirstNull() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(null, dest2, MapColor.BLUE, 3));
    }

    @Test
    void testConnectionSecondNull() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(dest1, null, MapColor.BLUE, 3));
    }

    @Test
    void testConnectionSameDest() {
        assertThrows(IllegalArgumentException.class, () -> r2 = new RailroadLine(dest1, dest1, MapColor.BLUE, 3));
    }

    @Test
    void testDestEquals() {
        r1 = new RailroadLine(dest1, dest2, MapColor.BLUE, 3);
        assertEquals(r1, new RailroadLine(dest2, dest1, MapColor.BLUE, 3));
    }

    @Test
    void testGetters() {
        assertEquals(MapColor.BLUE, r1.getColor());
        assertEquals(dest1, r1.getDestination1());
        assertEquals(dest2, r1.getDestination2());
        assertEquals(3, r1.getDistance());
        assertNull(r1.getOwner());
    }

    @Test
    void testEquals() {
        assertEquals(r1, new RailroadLine(dest1, dest2, MapColor.BLUE, 3));
        assertEquals(r1, new RailroadLine(dest2, dest1, MapColor.BLUE, 3));
        assertNotEquals(r1, new RailroadLine(dest1, dest3, MapColor.BLUE, 3));
    }
}
