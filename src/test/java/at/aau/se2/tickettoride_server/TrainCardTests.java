package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.datastructures.MapColor;
import at.aau.se2.tickettoride_server.datastructures.TrainCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrainCardTests
{
    TrainCard t1;
    TrainCard t2;
    TrainCard t3;
    TrainCard t4;
    TrainCard t5;
    TrainCard t6;
    TrainCard t7;
    TrainCard t8;
    TrainCard t9;


    @BeforeEach
    void init()
    {
        t1 = new TrainCard(TrainCard.Type.PINK);
        t2 = new TrainCard(TrainCard.Type.BLUE);
        t3 = new TrainCard(TrainCard.Type.GREEN);
        t4 = new TrainCard(TrainCard.Type.YELLOW);
        t5 = new TrainCard(TrainCard.Type.RED);
        t6 = new TrainCard(TrainCard.Type.WHITE);
        t7 = new TrainCard(TrainCard.Type.ORANGE);
        t8 = new TrainCard(TrainCard.Type.BLACK);
        t9 = new TrainCard(TrainCard.Type.LOCOMOTIVE);
    }

    @Test
    void testGetType()
    {
        assertEquals(TrainCard.Type.BLACK, t8.getType());
    }

    @Test
    void testToString()
    {
        assertEquals("blue", t2.toString());
    }

    @Test
    void testGetByString()
    {
        assertEquals(TrainCard.Type.PINK, TrainCard.Type.getByString("pink"));
        assertEquals(TrainCard.Type.BLUE, TrainCard.Type.getByString("blue"));
        assertEquals(TrainCard.Type.GREEN, TrainCard.Type.getByString("green"));
        assertEquals(TrainCard.Type.YELLOW, TrainCard.Type.getByString("yellow"));
        assertEquals(TrainCard.Type.RED, TrainCard.Type.getByString("red"));
        assertEquals(TrainCard.Type.WHITE, TrainCard.Type.getByString("white"));
        assertEquals(TrainCard.Type.ORANGE, TrainCard.Type.getByString("orange"));
        assertEquals(TrainCard.Type.BLACK, TrainCard.Type.getByString("black"));
        assertEquals(TrainCard.Type.LOCOMOTIVE, TrainCard.Type.getByString("locomotive"));
        assertNull(TrainCard.Type.getByString("notinlist"));
    }

    @Test
    void testMapToTrainCard()
    {
        assertEquals(TrainCard.Type.PINK, TrainCard.map_mapColor_to_TrainCardType(MapColor.PINK));
        assertEquals(TrainCard.Type.BLUE, TrainCard.map_mapColor_to_TrainCardType(MapColor.BLUE));
        assertEquals(TrainCard.Type.GREEN, TrainCard.map_mapColor_to_TrainCardType(MapColor.GREEN));
        assertEquals(TrainCard.Type.YELLOW, TrainCard.map_mapColor_to_TrainCardType(MapColor.YELLOW));
        assertEquals(TrainCard.Type.RED, TrainCard.map_mapColor_to_TrainCardType(MapColor.RED));
        assertEquals(TrainCard.Type.ORANGE, TrainCard.map_mapColor_to_TrainCardType(MapColor.ORANGE));
        assertEquals(TrainCard.Type.BLACK, TrainCard.map_mapColor_to_TrainCardType(MapColor.BLACK));
        assertEquals(TrainCard.Type.WHITE, TrainCard.map_mapColor_to_TrainCardType(MapColor.WHITE));
    }

    @Test
    void testCompareTo()
    {
        assertEquals(-1, t1.compareTo(t2));
        assertEquals(1, t3.compareTo(t4));
        assertEquals(1, t5.compareTo(t6));
        assertEquals(1, t7.compareTo(t8));
        assertEquals(-1, t1.compareTo(t9));

    }
}
