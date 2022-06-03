package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.DoubleRailroadLine;
import at.aau.se2.ticketToRide_server.dataStructures.Map;
import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.dataStructures.RailroadLine;
import at.aau.se2.ticketToRide_server.dataStructures.TrainCard;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class Session {
    //---------- REGEX FOR PARSER ------------------
    private static final String DELIMITER_COMMAND = ":";
    private static final String DELIMITER_MULTI = ";";
    private static final String DELIMITER_VALUE = ",";

    private static final String REGEX_NULL = "null";
    private static final String REGEX_NAME = "[^//W]+";
    private static final String REGEX_OPEN_CARD_ID = "[01234]";
    private static final String REGEX_MISSION_ID = "[//d][//d]?";

    private static final String COMMAND_ENTER_LOBBY = "enterLobby:" + REGEX_NAME;
    private static final String COMMAND_CREATE_GAME = "createGame:" + REGEX_NAME;
    private static final String COMMAND_EXIT_GAME = "exitGame";
    private static final String COMMAND_START_GAME = "startGame";
    private static final String COMMAND_JOIN_GAME = "joinGame:" + REGEX_NAME;
    private static final String COMMAND_LEAVE = "leave";
    private static final String COMMAND_LIST_PLAYERS_GAME = "listPlayersGame";

    private static final String REQUEST_LIST_GAMES = "listGames";
    private static final String REQUEST_LIST_PLAYERS_LOBBY = "listPlayersLobby";
    private static final String REQUEST_LIST_PLAYERS_GAME = COMMAND_LIST_PLAYERS_GAME + DELIMITER_COMMAND + REGEX_NAME;
    private static final String REQUEST_GAME_STATE = "getGameState:" + REGEX_NAME;

    private static final String REQUEST_GET_HAND_CARDS = "getHandCards";
    private static final String REQUEST_GET_OPEN_CARDS = "getOpenCards";
    private static final String REQUEST_GET_MAP = "getMap";
    private static final String REQUEST_GET_POINTS = "getPoints";
    private static final String REQUEST_GET_COLORS = "getColors";

    private static final String COMMAND_GET_CARD_STACK = "cardStack";
    private static final String COMMAND_GET_CARD_OPEN = "cardOpen:" + REGEX_OPEN_CARD_ID;
    private static final String COMMAND_BUILD_RAILROAD = "buildRailroad:" + REGEX_NAME + ":" + REGEX_NAME + ":" + REGEX_NAME;
    private static final String COMMAND_GET_MISSION = "getMission";
    private static final String COMMAND_CHOOSE_MISSION = "chooseMission:" + REGEX_MISSION_ID;
    //----------------------------------------------

    private static int sessionCounter = 0;

    private final int id;
    Socket session;
    private ReceivingThread receivingThread;
    private SendingThread sendingThread;
    BufferedReader inFromClient;
    DataOutputStream sendToClient;

    Player player;

    public Session(Socket session) throws Exception {
        this.id = sessionCounter++;
        this.session = session;
        new SetupSessionThread(this, session).start();
    }

    //region --------------------------------- PARSING -----------------------------------------------------------------

    void parseCommand(String received) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession received: " + received);
        if (Configuration_Constants.echo) send("echo:"+received);

        String[] commands = received.split(";");

        for (String command : commands) {
            if (command.matches(COMMAND_ENTER_LOBBY)) this.enterLobby(command);

            //----------- At this point in control flow the session leader must have called enterLobby to assign session Owner ----------
            //----------- when adding commands which don't necessarily need a session owner, put them above the following check ---------
            if (this.player == null) {
                sendingThread.sendCommand("ERROR: Not in Lobby");
                if (Configuration_Constants.debug) {
                    System.out.printf("(DEBUG)\t Session %d received command '%s' while sessionOwner=null\n", this.id, command);
                    send(command + ":null");
                }
                return;
            }

            //---- GENERAL REQUESTS ---------------------------------------------------------
            if (command.matches(REQUEST_LIST_GAMES)) this.listGames();
            else if (command.matches(REQUEST_LIST_PLAYERS_LOBBY)) this.listPlayersLobby();
            else if (command.matches(REQUEST_LIST_PLAYERS_GAME)) this.listPlayersGame();
            else if (command.matches(REQUEST_GAME_STATE)) this.getGameState(command);

                //---- GENERAL COMMANDS ---------------------------------------------------------
            else if (command.matches(COMMAND_CREATE_GAME)) this.createGame(command);
            else if (command.matches(COMMAND_JOIN_GAME)) this.joinGame(command);
            else if (command.matches(COMMAND_START_GAME)) this.startGame();
            else if (command.matches(COMMAND_EXIT_GAME)) this.exitGame();
            else if (command.matches(COMMAND_LEAVE)) this.leave();

                //----- IN GAME REQUESTS --------------------------------------------------------
            else if (command.matches(REQUEST_GET_HAND_CARDS)) this.getHandCards();
            else if (command.matches(REQUEST_GET_OPEN_CARDS)) this.getOpenCards();
            else if (command.matches(REQUEST_GET_MAP)) this.getMap();
            else if (command.matches(REQUEST_GET_POINTS)) this.getPoints();
            else if (command.matches(REQUEST_GET_COLORS)) this.getColors();

                //----- IN GAME COMMANDS --------------------------------------------------------
            else if (command.matches(COMMAND_GET_CARD_STACK)) this.getCardStack();
            else if (command.matches(COMMAND_GET_CARD_OPEN)) this.getCardOpen(command);
            else if (command.matches(COMMAND_BUILD_RAILROAD)) this.buildRailroad(command);
            else if (command.matches(COMMAND_GET_MISSION)) this.getMission();
            else if (command.matches(COMMAND_CHOOSE_MISSION)) this.chooseMission(command);
        }
    }


    //---- GENERAL REQUESTS ---------------------------------------------------------

    private void listPlayersLobby() {
        StringBuilder builder = new StringBuilder();
        ArrayList<Player> players = Lobby.getInstance().getPlayers();

        if (players.isEmpty()) {
            builder.append("null");
        } else {
            players.forEach(p -> builder.append(p.getName()).append(DELIMITER_VALUE));
        }

        String toClient = prepareSend(REQUEST_LIST_GAMES, builder.toString());
        send(toClient);
    }

    private void listGames() {
        StringBuilder builder = new StringBuilder();
        ArrayList<GameModel> games = Lobby.getInstance().getGames();

        if (games.isEmpty()) {
            builder.append("null");
        } else {
            games.forEach(g -> builder.append(g.getName()).append(DELIMITER_VALUE));
        }

        String toClient = prepareSend(REQUEST_LIST_GAMES, builder.toString());
        send(toClient);
    }

    private void listPlayersGame() {
        StringBuilder builder = new StringBuilder();
        ArrayList<Player> players = player.getGame().getPlayers();

        if (players.isEmpty()) {
            builder.append("null");
        } else {
            players.forEach(p -> builder.append(p.getName()).append(DELIMITER_VALUE));
        }

        String toClient = prepareSend(COMMAND_LIST_PLAYERS_GAME, builder.toString());
        send(toClient);
    }

    private void getGameState(String command) {
    }

    //---- GENERAL COMMANDS ---------------------------------------------------------

    private void enterLobby(String command) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession.enterLobby() called");
        if (this.player != null) {
            this.send("enterLobby:null");
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\tSession.enterLobby() failed: session already belongs to player " + player.getName());
            return;
        }

        String[] words = command.split(":");
        this.player = Lobby.getInstance().createPlayer(words[1], this);
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated Player " + words[1]);
    }

    private void createGame(String command) {
        String[] words = command.split(":");
        GameModel game = Lobby.getInstance().createGame(words[1], this.player);
        if (game == null) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\tCreating game " + words[1] + " failed");
            send("createGame:null");
            return;
        }
        player.joinGame(game);
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated game " + words[1]);
    }

    private void joinGame(String command) {
        String[] words = command.split(":");
        GameModel game = Lobby.getInstance().getGameByName(words[1]);
        if (player.joinGame(game)<0) send("joinGame:null");
    }

    private void leave() {

    }

    //----- IN GAME REQUESTS --------------------------------------------------------

    private void getHandCards() {
        if (player.getState()!= Player.State.GAMING) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\tSession.getHandCards() player is not gaming");
            send(REQUEST_GET_HAND_CARDS + DELIMITER_COMMAND + REGEX_NULL);
            return;
        }

        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession.getHandCards() listing cards...");
        ArrayList<TrainCard> handCards = player.getHandCards();

        if (handCards.size() == 0) {
            send(REQUEST_GET_HAND_CARDS + DELIMITER_COMMAND + REGEX_NULL);
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < handCards.size() - 1; i++) {
            TrainCard handCard = handCards.get(i);
            builder.append(handCard.getType().toString()).append(":");
        }

        TrainCard lastHandCard = handCards.get(handCards.size() - 1);
        builder.append(lastHandCard.getType().toString());

        send(builder.toString());
    }

    private void getOpenCards() {
        StringBuilder builder = new StringBuilder();
        GameModel gameModel = player.getGame();

        if (gameModel == null) {
            builder.append("null");
        } else {
            ArrayList<TrainCard> openCards = gameModel.getOpenCards();
            openCards.forEach(c -> builder.append(c.getType()).append(DELIMITER_VALUE));
        }

        //TODO send not return string LOCK while accessing game!!! Do this in player class cause encapsulation
        //return builder.toString();

        String toClient = prepareSend(REQUEST_GET_OPEN_CARDS, builder.toString());
        send(toClient);
    }

    private String getMap() {
        GameModel gameModel = player.getGame();
        if (gameModel == null) return "null";

        StringBuilder builder = new StringBuilder();
        Map map = gameModel.getMap();
        ArrayList<RailroadLine> railroadLines = new ArrayList<>(map.getRailroadLines());
        for (int i = 0; i < railroadLines.size() - 1; i++) {
            if (railroadLines.get(i) instanceof DoubleRailroadLine)
                builder.append(i).append(":").append(railroadLines.get(i).getOwner()).append(":").append(((DoubleRailroadLine) railroadLines.get(i)).getOwner2()).append(";");
            else
                builder.append(i).append(":").append(railroadLines.get(i).getOwner()).append(";");
        }
        if (railroadLines.get(railroadLines.size() - 1) instanceof DoubleRailroadLine)
            builder.append(railroadLines.size() - 1).append(":").append(railroadLines.get(railroadLines.size() - 1).getOwner()).append(":").append(((DoubleRailroadLine) railroadLines.get(railroadLines.size() - 1)).getOwner2()).append(";");
        else
            builder.append(railroadLines.size() - 1).append(":").append(railroadLines.get(railroadLines.size() - 1).getOwner()).append(";");

        return builder.toString();
    }

    private String getPoints() {
        GameModel gameModel = player.getGame();
        if (gameModel == null) return "null";

        StringBuilder builder = new StringBuilder();
        ArrayList<Player> players = gameModel.getPlayers();
        for (int i = 0; i < players.size() - 1; i++) {
            Player player = players.get(i);
            builder.append(player.getName()).append(":").append(player.getPoints()).append(":");
        }
        Player lastPlayer = players.get(players.size() - 1);
        builder.append(lastPlayer.getName()).append(":").append(lastPlayer.getPoints());

        //TODO send not return string LOCK while accessing game!!! Do this in player class cause encapsulation
        return builder.toString();
    }

    private String getColors() {
        GameModel gameModel = player.getGame();
        if (gameModel == null) return "null";

        StringBuilder builder = new StringBuilder();
        ArrayList<Player> players = gameModel.getPlayers();
        for (int i = 0; i < players.size() - 1; i++) {
            Player player = players.get(i);
            builder.append(player.getName()).append(":").append(player.getPlayerColor().toString()).append(":");
        }
        Player lastplayer = players.get(players.size() - 1);
        builder.append(lastplayer.getName()).append(":").append(lastplayer.getPlayerColor().toString());

        //TODO send not return string LOCK while accessing game!!! Do this in player class cause encapsulation
        return builder.toString();
    }


    //----- IN GAME COMMANDS -------------------------------------------------------

    private void startGame() {
        if (player.startGame()< 0) send("startGame:null");
    }

    private void getCardStack() {
        player.getCardStack();
    }

    private void getCardOpen(String command) {

//        player.getCardOpen(id);
    }

    private void buildRailroad(String command) {
        String[] words = command.split(":");
        player.buildRailroadLine(words[1], words[2], words[3]);
    }

    private void getMission() {
        player.getMissions();
    }

    private void chooseMission(String command) {
        LinkedList<Integer> chosen = new LinkedList<>();
        player.chooseMissions(chosen);
    }

    private void exitGame() {
        player.exitGame();
    }


    //endregion


    //region ------------------------------------ NETWORK ACTIVITY -----------------------------------------------------

    private String prepareSend(String command, String toClient) {
        // If delimiter exists at the end remove it before send
        if (toClient.endsWith(DELIMITER_VALUE)) {
            toClient = toClient.substring(0, toClient.length() - 1);
        }

        // Build string for sending to the client
        return command + DELIMITER_COMMAND + toClient;
    }

    public int send(String toClient) {
        if (sendingThread == null) {
            return -1;
        }
        sendingThread.sendCommand(toClient);
        return 0;
    }

    void setReceivingThread(ReceivingThread receivingThread) {
        this.receivingThread = receivingThread;
    }


    void setSendingThread(SendingThread sendingThread) {
        this.sendingThread = sendingThread;
    }


    //endregion
}
