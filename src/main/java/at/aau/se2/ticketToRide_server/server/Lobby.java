package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.util.ArrayList;


public class Lobby {
    private static Lobby lobby;
    private static String DEFAULT_GAME_NAME = "game ";
    private static int gameCounter = 0;

    private final ArrayList<Player> players;
    private final ArrayList<GameModel> games;

    private Lobby() {
        players = new ArrayList<>();
        games = new ArrayList<>();
    }

    public static Lobby getInstance() {
        if (lobby == null) lobby = new Lobby();
        return lobby;
    }




    //region ----------------------------------- LOBBY REQUESTS ------------------------------------------------------

    //Format listPlayersLobby:Player1.Player2.
    public String listPlayersLobby() {
        StringBuilder builder = new StringBuilder("listPlayersLobby:");

        synchronized (players) {
            for (Player player : this.players) {
                builder.append(player.getName()).append(".");
            }
           players.notify();
        }
        return builder.toString();
    }

    //Format listGames:Game1.Game2.
    public String listGames() {
        StringBuilder builder = new StringBuilder("listGames:");

        synchronized (this) {
            for (GameModel game: this.games) {
                builder.append(game.getName()).append(".");
            }
            this.notify();
        }
        return builder.toString();
    }


    public String listPlayersGame(String gameName) {
        String playersList = "listPlayersGame:noSuchGame";
        synchronized (this) {
            for (GameModel game : this.games) {
                if (game.getName().equals(gameName)) {
                    playersList = game.listPlayersGame();
                    break;
                }
            }
            this.notify();
        }
        return playersList;
    }


    private String getGameState(String gameName) {
        String gameState = "getGameState:noSuchGame";
        synchronized (this) {
            for (GameModel game : games) {
                if (game.getName().equals(gameName)) {
                    gameState = "getGameState:" + game.getState();
                }
            }
            this.notify();
        }
        return gameState;
    }


    //endregion




    //region ----------------------------------- LOBBY ACTIONS -------------------------------------------------------


    /**
     * creates a new Player within the Lobby
     *
     * @param name unique (in this Lobby) name
     * @return the Player if successful, null on fail
     */
    public Player enterLobby(String name, Session session) {
        Player player = null;
        synchronized (players) {
            boolean found = false;
            for (Player p : players) {
                if (p.getName().equals(name)) {//if name is already in use
                    if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Lobby.enterLobby() Name " + name + "already in use");
                    found = false;
                    break;
                }
            }
            if (!found) {
                player = new Player(name, session);
                players.add(player);
            }
            players.notify();
        }
        return player;
    }


    /**
     * Creates a game of the specified gameName
     * @param gameName  this gameName is listed in the lobby
     * @param owner Player who starts the game
     * @return the game if created, else null
     */
    public GameModel createGame(String gameName, Player owner) {
        if (gameName == null || gameName.length() == 0) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Lobby.createGame() Illegal game format: " + gameName);
            return null;
        }

        GameModel game = null;
        synchronized (games) {
            if (getGameByName(gameName) != null) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\t Lobby.createGame() game of gameName " + gameName + " already exists");
            }
            else {
                game = new GameModel(gameName, owner);
                this.games.add(game);
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\t Lobby.createGame() game of gameName " + gameName + " created");
            }
            games.notify();
        }
        return game;
    }


    public GameModel joinGame(String gameName, Player player) {
        GameModel game = null;
        synchronized (games) {
            for (GameModel g : games) {
                if (g.getName().equals(gameName)) {
                    game = g;
                    break;
                }
            }
            if (game == null) {
                if (Configuration_Constants.debug) System.out.println("(DEBUG)\tLobby.joinGame() No game of name " + gameName);
            }
            else {
                if (game.addPlayer(player) < 0) {
                    if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Game is full");
                    game = null;
                }
            }
            this.games.notify();
        }
        return game;
    }


    public int leave() {
        //TODO impl - This is the leave server command
        //exitGame
        //exitServer - exit sever in Session after successful leaving game
        return -1;
    }


    //endregion



    private GameModel getGameByName(String name) {
        for(GameModel game : this.games) {
            if(name.equals(game.getName())) return game;
        }
        return null;
    }


    public void removeGame(GameModel game) {
        this.games.remove(game);
    }
}
