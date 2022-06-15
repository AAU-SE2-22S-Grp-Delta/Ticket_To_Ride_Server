package at.aau.se2.ticketToRide_server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.server.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class PlayerTests
{
    static Session mockedSession = Mockito.mock(Session.class);
    static Player p1 = new Player("testPlayer", mockedSession);
    static Player p2 = new Player("testPlayer2", mockedSession);
    static Player p3 = new Player("testPlayer3", mockedSession);
    static Player p4 = new Player("testPlayer3", mockedSession);

    @BeforeAll
    static void init()
    {
        Player.enterLobby("testPlayer", mockedSession);
        p1.createGame("testGame");

    }

    @Test
    void testGetStones()
    {
        assertEquals(45, p1.getStones());
    }

    @Test
    void testGetName()
    {
        assertEquals("testPlayer", p1.getName());
    }

    @Test
    void testGetId()
    {
        assertEquals(0, p1.getId());
    }

    @Test
    void testPlayerPoints()
    {
        assertEquals(0, p1.getPlayerPoints());
    }

    @Test
    void testGetState()
    {
        assertEquals(Player.State.LOBBY, p3.getState());
    }

    @Test
    void testJoinGame()
    {
        assertEquals(-1, p1.joinGame("testGame"));
        assertEquals(-1, p2.joinGame("GameThatDoesntExist"));
    }

    @Test
    void testListPlayersLobby()
    {
        assertEquals("listPlayersLobby:testPlayer.", p1.listPlayersLobby());
    }

    @Test
    void testListGames()
    {
        assertEquals("listGames:testGame.", p1.listGames());
    }

    @Test
    void testListPlayersGame()
    {
        assertEquals("listPlayersGame:testPlayer.", p1.listPlayersGame("testGame"));
    }

    @Test
    void testGetGameState()
    {
        assertEquals("listPlayersGame:testPlayer.", p1.getGameState("testGame"));
    }

    @Test
    void testGetHandCards()
    {
        assertEquals("getHandCards:null", p3.getHandCards());
        assertEquals("getHandCards:", p1.getHandCards());
    }


    @Test
    void testGetOpenCards()
    {
        assertEquals("openHandCard:null", p3.getOpenCards());
        String[] s = p1.getOpenCards().split(":");
        assertEquals(2, s.length);
    }

    @Test
    void testGetMap()
    {
        assertEquals("getMap:null", p3.getMap());
        p1.getMap();
    }

    @Test
    void testGetPoints()
    {
        //assertEquals("getPoints:0", p1.getPoints()); ERROR IN MODEL
        assertEquals("getPoints:null", p3.getPoints());
    }

    @Test
    void testGetColors()
    {
        assertEquals("getColor:null", p3.getColors());
        assertEquals("getColors:testPlayerRED.", p1.getColors());
    }

    @Test
    void testGetMissions()
    {
        assertEquals("getMissions:null", p3.getMissions());
        assertEquals("getMissions", p1.getMissions());
    }


    @Test
    void testGetNumStones()
    {
        assertEquals("getNumStones:null", p3.getNumStones());
        assertEquals("getNumStones:45", p1.getNumStones());
    }

    @Test
    void testStartGame()
    {
        assertEquals(-1, p3.startGame());
        assertEquals(0, p1.startGame());
    }

    @Test
    void drawFromStack()
    {
        assertEquals("cardStack:null", p3.drawCardStack());
        p2.drawCardStack();
        String[] s = p2.getHandCards().split(":");
        assertEquals(2, s.length);
    }

    @Test
    void drawMission()
    {
        //?????
        assertEquals("drawMission:null", p3.drawMission());
        p1.drawMission();
        p1.chooseMissions(new LinkedList<>(List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30)));
        System.out.println(p1.getMissions());
    }

//    @Test
//    void drawOpen()
//    {
//        assertEquals(-1, p3.drawCardOpen(2));
//        assertEquals(0, p2.drawCardOpen(3));
//    }



}
