package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;
import at.aau.se2.ticketToRide_server.dataStructures.Mission;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissionTests
{
    static Destination dest1;
    static Destination dest2;
    static Mission m1;
    static Mission m2;

    @BeforeAll
    static void init()
    {
        dest1 = new Destination("tdest1");
        dest2 = new Destination("tdest2");

        m1 = new Mission(dest1, dest2, 3, 0);
        m2 = new Mission(dest1, dest2, 3, 1);
    }

    @Test
    void testGetters()
    {
        assertEquals(dest1, m1.getDestination1());
        assertEquals(dest2, m1.getDestination2());
        assertEquals(3, m1.getPoints());
        assertEquals(0, m1.getId());
        assertFalse(m1.isDone());
    }

    @Test
    void testSetDone()
    {
        m2.setDone();
        assertTrue(m2.isDone());
    }
}
