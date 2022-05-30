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

    public void joinGame(String gameName, Player player) throws IllegalArgumentException {
        GameModel game = null;
        for (GameModel g : games) {
            if (g.getName().equals(gameName)) {
                game = g;
                break;
            }
        }
        if (game == null) throw new IllegalArgumentException("No game of the name " + gameName);
        for (Player p : game.getPlayers())
            if (player.getName().equals(player.getName()))
                throw new IllegalArgumentException("Player of name " + player.getName() + "has already joint game of name " + gameName);
        game.addPlayer(player);
    }

    /**
     * creates a new Player within the Lobby
     *
     * @param name unique (in this Lobby) name
     * @return the Player if successful, null on fail
     */
    public Player createPlayer(String name, Session session) {
        for (Player player : players) if (player.getName().equals(name)) return null; //if name is already in use
        Player player = new Player(name, session);
        players.add(player);
        return player;
    }

    public Player getPlayerByName(String name) {
        for (Player player : players) if (player.getName().equals(name)) return player;
        return null;
    }

    /**
     * Creates a game of the specified name
     * @param name  this name is listed in the lobby
     * @param owner Player who starts the game
     * @return the game if created, else null
     */
    public GameModel createGame(String name, Player owner) {
        if (name == null || name.length() == 0) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Lobby.createGame() Illegal game format: " + name);
            return null;
        }

        if (getGameByName(name)!=null) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Lobby.createGame() game of name " + name + " already exists");
            return null;
        }

//        name = DEFAULT_GAME_NAME + ++gameCounter;
        GameModel game = new GameModel(name, owner);
        this.games.add(game);
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\t Lobby.createGame() game of name " + name + " created");
        return game;
    }


    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<GameModel> getGames() {
        return games;
    }


    /**
     * Searches for the game of the specified name
     * @param name
     * @return the game on success, null on fail
     */
    public GameModel getGameByName(String name) {
        for(GameModel game : this.games) {
            if(name.equals(game.getName())) return game;
        }
        return null;
    }
}
