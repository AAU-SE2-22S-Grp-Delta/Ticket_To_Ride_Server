package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Session {
    //---------- REGEX FOR PARSER ------------------
    private static final String REGEX_NAME = "[^:;]*?";
    private static final String REGEX_OPEN_CARD_ID = "[01234]";
    private static final String REGEX_MISSION_ID = "[//d][//d]?";

    private static final String COMMAND_ENTER_LOBBY = "enterLobby:" + REGEX_NAME;
    private static final String COMMAND_CREATE_GAME = "createGame";
    private static final String COMMAND_EXIT_GAME = "exitGame";
    private static final String COMMAND_START_GAME = "startGame";
    private static final String COMMAND_JOIN_GAME = "joinGame:" + REGEX_NAME;
    private static final String COMMAND_LEAVE = "leave";

    private static final String REQUEST_LIST_GAMES = "listGames";
    private static final String REQUEST_LIST_PLAYERS_LOBBY = "listPlayersLobby";
    private static final String REQUEST_LIST_PLAYERS_GAME = "listPlayersGame:" + REGEX_NAME;
    private static final String REQUEST_GAME_STATE = "getGameState:" + REGEX_NAME;

    private static final String REQUEST_GET_HAND_CARDS = "getHandCards";
    private static final String REQUEST_GET_OPEN_CARDS = "getOpenCards";
    private static final String REQUEST_GET_MAP = "getMap";
    private static final String REQUEST_GET_POINTS = "getPoints";
    private static final String REQUEST_GET_COLORS = "getColors";

    private static final String COMMAND_GET_CARD_STACK = "cardStack";
    private static final String COMMAND_GET_CARD_OPEN = "cardOpen:" + REGEX_OPEN_CARD_ID;
    private static final String COMMAND_BUILD_RAILROAD = "buildRailRoad:" + REGEX_NAME + "+" + REGEX_NAME;
    private static final String COMMAND_GET_MISSION = "getMission";
    private static final String COMMAND_CHOOSE_MISSION = "chooseMission:" + REGEX_MISSION_ID;
    //----------------------------------------------

    private static int sessionCounter = 0;

    private final int id;
    Socket session;
    private ReceiveingThread receiveingThread;
    private SendingThread sendingThread;
    BufferedReader inFromClient;
    DataOutputStream sendToClient;

    Player sessionOwner;

    public Session(Socket session) throws Exception {
        this.id = sessionCounter++;
        this.session = session;
        new SetupSessionThread(this, session).start();
    }

    //region --------------------------------- PARSING -----------------------------------------------------------------

    void parseCommand(String received) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession: received: " + received);

        String[] commands = received.split(";");

        for (String command : commands) {
            if (command.matches(COMMAND_ENTER_LOBBY)) this.enterLobby(command);

            //----------- At this point in control flow the session leader must have called enterLobby to assign session Owner ----------
            //----------- when adding commands which don't necessarily need a session owner, put them above the following check ---------
            if (this.sessionOwner == null) {
                sendingThread.setCommand("ERROR: Not in Lobby");
                if (Configuration_Constants.debug) {
                    System.out.printf("(DEBUG)\t Session %d received command '%s' while sessionOwner=null\n", this.id, command);
                    send(command + ":null");
                }
                return;
            }

            //---- GENERAL REQUESTS ---------------------------------------------------------
            if (command.equals(REQUEST_LIST_GAMES)) this.listGames();
            else if (command.equals(REQUEST_LIST_PLAYERS_LOBBY)) this.listPlayersLobby();
            else if (command.equals(REQUEST_LIST_PLAYERS_GAME)) this.listPlayersGame();
            else if (command.equals(REQUEST_GAME_STATE)) this.getGameState(command);

                //---- GENERAL COMMANDS ---------------------------------------------------------
            else if (command.matches(COMMAND_CREATE_GAME)) this.createGame(command);
            else if (command.matches(COMMAND_JOIN_GAME)) this.joinGame(command);
            else if (command.equals(COMMAND_START_GAME)) this.startGame();
            else if (command.equals(COMMAND_EXIT_GAME)) this.exitGame();
            else if (command.equals(COMMAND_LEAVE)) this.leave();

                //----- IN GAME REQUESTS --------------------------------------------------------
            else if (command.equals(REQUEST_GET_HAND_CARDS)) this.getHandCards();
            else if (command.equals(REQUEST_GET_OPEN_CARDS)) this.getOpenCards();
            else if (command.equals(REQUEST_GET_MAP)) this.getMap();
            else if (command.equals(REQUEST_GET_POINTS)) this.getPoints();
            else if (command.equals(REQUEST_GET_COLORS)) this.getColors();

                //----- IN GAME COMMANDS --------------------------------------------------------
            else if (command.equals(COMMAND_GET_CARD_STACK)) this.getCardStack();
            else if (command.equals(COMMAND_GET_CARD_OPEN)) this.getCardOpen(command);
            else if (command.equals(COMMAND_BUILD_RAILROAD)) this.buildRailRoad(command);
            else if (command.equals(COMMAND_GET_MISSION)) this.getMission();
            else if (command.equals(COMMAND_CHOOSE_MISSION)) this.chooseMission(command);
        }
    }


    //---- GENERAL REQUESTS ---------------------------------------------------------

    private void listPlayersLobby() {
    }

    private String listGames() {
        if (Lobby.getInstance().getGames().size() == 0) return "empty";
        StringBuilder builder = new StringBuilder();
        ArrayList<GameModel> games = Lobby.getInstance().getGames();
        for (int i = 0; i < games.size() - 1; i++) {
            GameModel game = games.get(i);
            builder.append(game.getName()).append(":").append(game.getId()).append(";");
        }
        builder.append(games.get(games.size() - 1).getName()).append(":").append(games.get(games.size() - 1).getId());
        return builder.toString();
    }

    private void listPlayersGame() {
    }

    private void getGameState(String command) {
    }

    //---- GENERAL COMMANDS ---------------------------------------------------------

    private void enterLobby(String command) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession.enterLobby() called");
        if (this.sessionOwner != null) {
            this.send("enterLobby:null");
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\tSession.enterLobby() failed: session already belongs to player " + sessionOwner.getName());
            return;
        }

        String[] words = command.split(":");
        this.sessionOwner = Lobby.getInstance().createPlayer(words[1], this);
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated Player " + words[1]);
    }

    private void createGame(String command) {
        String[] words = command.split(":");
        if (Lobby.getInstance().createGame(words[1], this.sessionOwner) < 0) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\tCreating game " + words[1] + " failed");
            send("createGame:null");
            return;
        }
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated game " + words[1]);
    }

    private void joinGame(String command) {
        String[] words = command.split(":");
        sessionOwner.joinGame(words[1]);
    }

    private void leave() {

    }

    //----- IN GAME REQUESTS --------------------------------------------------------

    private void getHandCards() {

    }

    private void getOpenCards() {

    }

    private void getMap() {

    }

    private void getPoints() {

    }

    private void getColors() {
    }


    //----- IN GAME COMMANDS -------------------------------------------------------

    private void startGame() {
    }

    private void getCardStack() {
    }

    private void getCardOpen(String command) {
    }

    private void buildRailRoad(String command) {
    }

    private void getMission() {
    }

    private void chooseMission(String command) {
    }

    private void exitGame() {
    }


    //endregion


    public int send(String toClient) {
        if (sendingThread == null) return -1;
        sendingThread.setCommand(toClient);
        return 0;
    }

    void setReceivingThread(ReceiveingThread receiveingThread) {
        this.receiveingThread = receiveingThread;
    }

    void setSendingThread(SendingThread sendingThread) {
        this.sendingThread = sendingThread;
    }
}
