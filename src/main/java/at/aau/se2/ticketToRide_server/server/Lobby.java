package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.util.ArrayList;


public class Lobby {
    private static Lobby lobby;
    private static String DEFAULT_GAME_NAME = "game ";
    private static int gameCounter = 0;

    private ArrayList<Player> players;
    private ArrayList<GameModel> games;

    private Lobby() {
        players = new ArrayList<>();
        games = new ArrayList<>();
    }

    public static Lobby getInstance() {
        if (lobby == null) lobby = new Lobby();
        return lobby;
    }

    public void startNewGame(Player owner, String name) {
        GameModel game = new GameModel(name, owner);
        games.add(game);
    }

    /**
     * creates a new Player within the Lobby
     * @param name unique (in this Lobby) name
     * @return 0 if successful, -1 on fail
     */
    public int createPlayer(String name) {
        for (Player player : players) if (player.getName().equals(name)) return -1; //if name is already in use
        players.add(new Player(name));
        return 0;
    }

    public Player getPlayerByName(String name) {
        for (Player player : players) if (player.getName().equals(name)) return player;
        return null;
    }

    /**
     *
     * @param name this name is listed in the loby
     * @param owner Player who starts the game
     * @return 0 if successful, -1 on fail
     */
    public int createGame(String name, Player owner) {
        if (name == null || name.length() == 0) name = DEFAULT_GAME_NAME + ++gameCounter;
        GameModel game = new GameModel(name, owner);
        this.games.add(game);
        return 0;
    }


    /**
     *
     * @param id
     * @param player
     * @return 0 if successful, -1 on fail
     */
    public int joinGame(int id, Player player) {
        for (GameModel game : games) {
            if (game.getId() == id) {
                game.addPlayer(player);
                return 0;
            }
        }
        return -1;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<GameModel> getGames() {
        return games;
    }
}
