package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.LinkedList;

public class Session {
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
    private static final String REQUEST_GET_MISSIONS = "getMissions";

    private static final String COMMAND_DRAW_CARD_STACK = "cardStack";
    private static final String COMMAND_DRAW_CARD_OPEN = "cardOpen:" + REGEX_OPEN_CARD_ID;
    private static final String COMMAND_BUILD_RAILROAD = "buildRailroad:" + REGEX_NAME + ":" + REGEX_NAME + ":" + REGEX_NAME;
    private static final String COMMAND_DRAW_MISSION = "getMission";
    private static final String COMMAND_CHOOSE_MISSION = "chooseMission:" + REGEX_MISSION_ID;
    //----------------------------------------------




    void parseCommand(String received) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession received: " + received);
        if (Configuration_Constants.echo) send("echo:" + received);

        String[] commands = received.split(";");

        for (String command : commands) {
            if (command.matches(COMMAND_ENTER_LOBBY)) {
                this.enterLobby(command);
                continue;
            }

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
            else if (command.matches(REQUEST_LIST_PLAYERS_GAME)) this.listPlayersGame(command);
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
            else if (command.matches(REQUEST_GET_MISSIONS)) this.getMissions();

                //----- IN GAME COMMANDS --------------------------------------------------------
            else if (command.matches(COMMAND_DRAW_CARD_STACK)) this.drawCardStack();
            else if (command.matches(COMMAND_DRAW_CARD_OPEN)) this.drawCardOpen(command);
            else if (command.matches(COMMAND_BUILD_RAILROAD)) this.buildRailroad(command);
            else if (command.matches(COMMAND_DRAW_MISSION)) this.drawMission();
            else if (command.matches(COMMAND_CHOOSE_MISSION)) this.chooseMission(command);
        }
    }


    //endregion




    //region---- LOBBY REQUESTS ---------------------------------------------------------


    private void listPlayersLobby() {
        send(player.listPlayersLobby());
    }


    private void listGames() {
        send(player.listGames());
    }


    private void listPlayersGame(String command) {
        String[] words = command.split(DELIMITER_COMMAND);
        send(player.listPlayersGame(words[1]));
    }


    private void getGameState(String command) {
        String[] words = command.split(DELIMITER_COMMAND);
        send(player.getGameState(words[1]));
    }


    //endregion




    //region ---- LOBBY COMMANDS ---------------------------------------------------------


    private void enterLobby(String command) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession.enterLobby() called");
        if (this.player != null) {
            this.send("enterLobby:null");
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\tSession.enterLobby() failed: session already belongs to player " + player.getName());
            return;
        }

        String[] words = command.split(":");
        this.player = Player.enterLobby(words[1], this);
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated Player " + words[1]);
    }


    private void createGame(String command) {
        String[] words = command.split(":");

        if (player.createGame(words[1]) < 0) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\tCreating game " + words[1] + " failed");
            send("createGame:null");
            return;
        }
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreated game " + words[1]);
    }


    private void joinGame(String command) {
        String[] words = command.split(":");
        if (player.joinGame(words[1]) < 0) send("joinGame:null");
    }


    private void leave() {

    }


    //endregion




    //region ----- GAME REQUESTS --------------------------------------------------------


    private void getHandCards() {
        send(player.getHandCards());
    }


    private void getOpenCards() {
        send(player.getOpenCards());
    }


    private void getMap() {
        send(player.getMap());
    }


    private void getPoints() {
        send(player.getPoints());
    }


    private void getColors() {
        send(player.getPoints());
    }


    private void getMissions() {
        send(player.getMissions());
    }


    //endregion




    //region ----- GAME COMMANDS -------------------------------------------------------


    private void startGame() {
        if (player.startGame() < 0) send("startGame:null");
    }


    private void drawCardStack() {
        if (player.drawCardStack()<0) send("getCardStack:null)");
    }


    private void drawCardOpen(String command) {
        String[] words = command.split(DELIMITER_COMMAND);
        player.drawCardOpen(Integer.parseInt(words[1]));
    }


    private void buildRailroad(String command) {
        String[] words = command.split(":");
        player.buildRailroadLine(words[1], words[2], words[3]);
    }


    private void drawMission() {
        //TODO impl
    }


    private void chooseMission(String command) {
        String[] words = command.split(DELIMITER_COMMAND);
        LinkedList<Integer> chosen = new LinkedList<>();
        for (int i = 1; i < words.length; i++) {
            chosen.add(Integer.parseInt(words[i]));
        }
        if (player.chooseMissions(chosen)<0) send("choseMission:null");
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
