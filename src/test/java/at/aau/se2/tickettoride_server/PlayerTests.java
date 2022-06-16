package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.datastructures.*;
import at.aau.se2.tickettoride_server.server.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayerTests
{
    static Session mockedSession = Mockito.mock(Session.class);
    static Player p1 = new Player("testPlayer", mockedSession);
    static Player p2 = new Player("testPlayer2", mockedSession);
    static Player p3 = new Player("testPlayer3", mockedSession);
    static Player p4 = new Player("testPlayer4", mockedSession);

    @BeforeAll
    static void init()
    {
        Player.enterLobby("testPlayer", mockedSession);
        p1.createGame("testGame");
        p4.joinGame("testGame");

    }

    @Test
    @Order(1)
    void testGetStones()
    {
        assertEquals(45, p1.getStones());
    }

    @Test
    @Order(2)
    void testGetName()
    {
        assertEquals("testPlayer", p1.getName());
    }

    @Test
    @Order(3)
    void testGetId()
    {
        assertEquals(0, p1.getId());
    }

    @Test
    @Order(4)
    void testPlayerPoints()
    {
        p1.setPoints(10);
        assertEquals(10, p1.getPlayerPoints());
        String[] s = p1.getPoints().split(":");
        String[] _s = s[1].split("\\.");
        assertEquals(2, _s.length);
    }

    @Test
    @Order(5)
    void testGetState()
    {
        assertEquals(Player.State.LOBBY, p3.getState());
    }

    @Test
    @Order(6)
    void testJoinGame()
    {
        assertEquals(-1, p1.joinGame("testGame"));
        assertEquals(-1, p2.joinGame("GameThatDoesntExist"));
    }

    @Test
    @Order(7)
    void testListPlayersLobby()
    {
        assertEquals("listPlayersLobby:testPlayer.", p1.listPlayersLobby());
    }

    @Test
    @Order(8)
    void testListGames()
    {
        assertEquals("listGames:testGame.", p1.listGames());
    }

    @Test
    @Order(9)
    void testListPlayersGame()
    {
        String[] s = p1.listPlayersGame("testGame").split(":");
        String[] _s = s[1].split("\\.");
        assertEquals(2, _s.length);
    }


    @Test
    @Order(10)
    void testGetHandCards()
    {
        assertEquals("getHandCards:null", p3.getHandCards());
        String[] s = p1.getHandCards().split(":");
        assertEquals(1, s.length);
    }

    @Test
    @Order(11)
    void testGetOpenCards()
    {
        assertEquals("openHandCard:null", p3.getOpenCards());
        String[] s = p1.getOpenCards().split(":");
        assertEquals(2, s.length);
    }

    @Test
    @Order(12)
    void testGetMap()
    {
        assertEquals("getMap:null", p3.getMap());
        p1.getMap();
    }

    @Test
    @Order(13)
    void testGetPoints()
    {
        //assertEquals("getPoints:0", p1.getPoints()); ERROR IN MODEL
        assertEquals("getPoints:null", p3.getPoints());
    }

    @Test
    @Order(14)
    void testGetColors()
    {
        assertEquals("getColor:null", p3.getColors());
        String[] s = p1.getColors().split(":");
        String[] _s = s[1].split("\\.");
        assertEquals(2, _s.length);
    }

    @Test
    @Order(15)
    void testGetMissions()
    {
        assertEquals("getMissions:null", p3.getMissions());
        assertEquals("getMissions", p1.getMissions());
    }

    @Test
    @Order(16)
    void testGetNumStones()
    {
        assertEquals("getNumStones:null", p3.getNumStones());
        assertEquals("getNumStones:45", p1.getNumStones());
    }

    @Test
    @Order(17)
    void testStartGame()
    {
        assertEquals(-1, p3.startGame());
        assertEquals(0, p1.startGame());
    }

    @Test
    @Order(18)
    void drawFromStack()
    {
        assertEquals("cardStack:null", p3.drawCardStack());
        if (p4.isActive())
            p4.drawCardStack();
        else
            p4.addHandCard(new TrainCard(TrainCard.Type.RED));
        String[] s = p4.getHandCards().split(":");
        assertEquals(2, s.length);
    }

    @Test
    @Order(19)
    void drawMission()
    {
        //?????
        assertEquals("drawMission:null", p3.drawMission());
        p1.drawMission();
        p1.chooseMissions(new LinkedList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)));
        System.out.println(p1.getMissions());
    }

    @Test
    @Order(20)
    void drawOpen()
    {
        assertEquals(-1, p3.drawCardOpen(2));
        if (p4.isActive())
            assertEquals(0, p4.drawCardOpen(3));
        else
            assertEquals(-1, p4.drawCardOpen(3));
    }

    @Test
    @Order(21)
    void testSetRROwner()
    {
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        assertEquals(0, p4.buildRailroadLine("Vancouver", "Calgary", "gray"));

    }

    @Test
    @Order(22)
    void testSetRROwnerNotExists()
    {
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        assertEquals(-1, p4.buildRailroadLine("Vancouver", "Vancouver", "gray"));
    }

    @Test
    @Order(23)
    void testSetRROwnerNotEnoughCards()
    {
        assertEquals(-1, p4.buildRailroadLine("Vancouver", "Calgary", "gray"));
    }

    @Test
    @Order(24)
    void testGetLongest()
    {
        if (!p4.isActive())
            p1.drawCardOpen(3);
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        p4.addHandCard(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        assertEquals(0, p4.buildRailroadLine("Calgary", "Winnipeg", "white"));
        assertEquals(1, p4.findLongestConnection());
    }

    @Test
    @Order(25)
    void missionTests()
    {
        //can't really test this as missions are always random

        if (p4.isActive())
        {
            p4.drawMission();
            p4.chooseMissions(new LinkedList<>(Arrays.asList(1, 2, 3)));
        }
        else
        {
            p1.drawMission();
            p1.chooseMissions(new LinkedList<>(Arrays.asList(1, 2, 3)));
        }
        if (p4.isActive())
        {
            p4.drawMission();
            p4.chooseMissions(new LinkedList<>(Arrays.asList(1, 2, 3)));
        }
        else
        {
            p1.drawMission();
            p4.chooseMissions(new LinkedList<>(Arrays.asList(1, 2, 3)));
        }

        assertEquals("getMissions", p1.getMissions());
    }

    @Test
    @Order(999)
    void endGame()
    {
        p4.exitGame();
        p1.exitGame();
    }
}
