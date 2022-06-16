package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.datastructures.MapColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapColorTests
{
    @Test
    void testGetByString()
    {
        assertEquals(MapColor.BLUE, MapColor.getByString("blue"));
        assertEquals(MapColor.BLACK, MapColor.getByString("black"));
        assertEquals(MapColor.GRAY, MapColor.getByString("gray"));
        assertEquals(MapColor.GREEN, MapColor.getByString("green"));
        assertEquals(MapColor.ORANGE, MapColor.getByString("orange"));
        assertEquals(MapColor.PINK, MapColor.getByString("pink"));
        assertEquals(MapColor.RED, MapColor.getByString("red"));
        assertEquals(MapColor.WHITE, MapColor.getByString("white"));
        assertEquals(MapColor.YELLOW, MapColor.getByString("yellow"));
        assertNull(MapColor.getByString("no_color"));
    }

    @Test
    void testToString() {
        assertEquals("black", MapColor.BLACK.toString());
    }
}
