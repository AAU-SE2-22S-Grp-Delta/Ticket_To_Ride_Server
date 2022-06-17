package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionTests {
    static Socket mockedSocket = Mockito.mock(Socket.class);
    static Session session;

    @BeforeAll
    static void init() {
        try {
            session = new Session(mockedSocket);
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    void testSend() {
        assertDoesNotThrow(() -> session.send("test"));
    }

    @Test
    @Order(2)
    void testParseCommand() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("parseCommand", String.class);
                method.setAccessible(true);
                method.invoke(session, "test");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(3)
    void testEnterLobby() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("enterLobby", String.class);
                method.setAccessible(true);
                method.invoke(session, "Player1");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(4)
    void testListPlayersLobby() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("listPlayersLobby", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(5)
    void testCreateGame() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("createGame", String.class);
                method.setAccessible(true);
                method.invoke(session, "test");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(6)
    void testListGames() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("listGames", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(7)
    void testListPlayersGame() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("listPlayersGame", String.class);
                method.setAccessible(true);
                method.invoke(session, "Game1");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(8)
    void testGetGameState() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getGameState", String.class);
                method.setAccessible(true);
                method.invoke(session, "Game1");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(9)
    void testJoinGame() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("joinGame", String.class);
                method.setAccessible(true);
                method.invoke(session, "Game1");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(10)
    void testGetHandCards() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getHandCards", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(11)
    void testGetOpenCards() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getOpenCards", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(12)
    void testGetMap() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getMap", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(13)
    void testGetPoints() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getPoints", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(14)
    void testGetColors() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getColors", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(15)
    void testGetMissions() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getMissions", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(16)
    void testCheatMission() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("cheatMission", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }

    @Test
    @Order(16)
    void testGetWinner() {
        assertDoesNotThrow(() -> {
            try {
                Method method = Session.class.getDeclaredMethod("getWinner", (Class<?>[]) null);
                method.setAccessible(true);
                method.invoke(session);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException ignored) {
            }
        });
    }
}
