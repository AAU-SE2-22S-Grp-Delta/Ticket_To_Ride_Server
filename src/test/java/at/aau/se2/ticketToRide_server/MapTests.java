package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;
import at.aau.se2.ticketToRide_server.dataStructures.Map;
import at.aau.se2.ticketToRide_server.dataStructures.MapColor;
import at.aau.se2.ticketToRide_server.dataStructures.RailroadLine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapTests
{
    static Set destSet = new HashSet<Destination>();
    static Set roadSet = new HashSet<RailroadLine>();

    static Map map = new Map();
    static Destination dest1;
    static Destination dest2;
    static Destination dest4;
    static Destination dest5;
    static RailroadLine r1;
    static RailroadLine r3;

    @BeforeAll
    static void init()
    {
        dest1 = new Destination("tMapDest1");
        dest2 = new Destination("tMapDest2");
        dest4 = new Destination("tMapDest4");
        dest5 = new Destination("tMapDest5");

        map.addDestination(dest4);
        map.addDestination(dest5);
        destSet.add(dest4);
        destSet.add(dest5);
        r3 = new RailroadLine(dest4, dest5, MapColor.BLUE, 3);
        r1 = new RailroadLine(dest1, dest2, MapColor.BLUE, 3);
    }

    @Test
    void testAddRoadTaken()
    {
    }

    @Test
    void testAddDest()
    {
        map.addDestination(dest1);
        destSet.add(dest1);
        map.addDestination(dest2);
        destSet.add(dest2);
        assertEquals(destSet, map.getDestinations());
    }

    @Test
    void testAddRoad()
    {
        map.addRailroadLine(r3);
        roadSet.add(r3);
        assertEquals(roadSet, map.getRailroadLines());
        assertThrows(IllegalStateException.class, ()->map.addRailroadLine(r3));

    }

    @Test
    void testAddRoadThrows()
    {
        Destination dest3 = new Destination("tMdest3");
        RailroadLine r2 = new RailroadLine(dest1, dest3, MapColor.BLUE, 2);
        RailroadLine r3 = new RailroadLine(dest3, dest1, MapColor.BLUE, 2);

        assertThrows(IllegalArgumentException.class, () -> map.addRailroadLine(r2));
        assertThrows(IllegalArgumentException.class, () -> map.addRailroadLine(r3));
    }

    @Test
    void testGetDest()
    {
        assertEquals(destSet, map.getDestinations());
    }

    @Test
    void testGetRoads()
    {
        assertEquals(roadSet, map.getRailroadLines());
    }

    @Test
    void testGetDestByName()
    {
        assertEquals(dest4, map.getDestinationByName("tMapDest4"));
    }

    @Test
    void testGetDestByNameNull()
    {
        assertNull(map.getDestinationByName("notinset"));
    }
}
