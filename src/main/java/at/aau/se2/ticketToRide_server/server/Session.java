package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Session {
    //---------- REGEX FOR PARSER ------------------
    private static String REGEX_NAME = "[^:;]*?";
    private static String REGEX_GAME_ID = "[\\d*]";
    //----------------------------------------------

    private static int sessionCounter = 0;

    private int id;
    Socket session;
    private ReceiveingThread receiveingThread;
    private SendingThread sendingThread;
    Lobby lobby;

    BufferedReader inFromClient;
    DataOutputStream sendToClient;

    Player sessionOwner;

    public Session(Socket session) throws Exception {
        this.id = sessionCounter++;
        this.session = session;
        this.lobby = Lobby.getInstance();
        new SetupSessionThread(this, session).start();
    }

    void parseCommand(String received) {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSession: received: " + received);

        String[] commands = received.split(";");

        for (String command : commands) {
            if (command.matches("enterLobby:" + REGEX_NAME)) {
                String[] words = command.split(":");
                if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tCreating Player " + words[1]);
                this.sessionOwner = lobby.createPlayer(words[1], this);
            }

            //----------- At this point in control flow the session leader must have called enterLobby to assign session Owner ----------
            //----------- when adding commands which don't necessarily need a session owner, put them above the following check ---------
            if (this.sessionOwner == null) {
                sendingThread.setCommand("ERROR: Not in Lobby");
                if (Configuration_Constants.debug)
                    System.out.printf("(DEBUG)\t Session %d received command '%s' while sessionOwner=null\n", this.id, command);
                return;
            }

            if (command.matches("createGame:" + REGEX_NAME)) {

                String[] words = command.split(":");
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\tCreating game " + words[1] + ", owner=" + this.sessionOwner.getName());
                lobby.createGame(words[1], sessionOwner);
            } else if (command.matches("joinGame:" +REGEX_NAME)) {
                String[] words = command.split(":");
                try {
                    lobby.joinGame(words[1], this.sessionOwner);
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\t player of name " + words[1] + "joint game of name " + this.sessionOwner.getName());
                } catch (IllegalArgumentException e) {
                    if (Configuration_Constants.debug) {
                        System.out.println("(debug)\t Error while executing " + command);
                        e.printStackTrace();
                    }
                }
            }


            else if (command.equals("startGame")) ;


            else if (command.equals("listGames")) {
                String gameList = listGames();
                if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tTo send: " + gameList);
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\t" + (send(gameList) == 0 ? "Session: sent" : "Session: unable to send"));
            }
        }
    }

    private String listGames() {
        if (lobby.getGames().size() == 0) return "empty";
        StringBuilder builder = new StringBuilder();
        ArrayList<GameModel> games = lobby.getGames();
        for (int i = 0; i < games.size() - 1; i++) {
            GameModel game = games.get(i);
            builder.append(game.getName()).append(":").append(game.getId()).append(";");
        }
        builder.append(games.get(games.size() - 1).getName()).append(":").append(games.get(games.size() - 1).getId());
        return builder.toString();
    }

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
