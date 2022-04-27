package at.aau.se2.ticketToRide_server.server;

import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.models.GameModel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Session {
    //---------- REGEX FOR PARSER ------------------
    private static String REGEX_NAME = "[^:;]*?";
    private static String REGEX_GAME_ID = "[\\d*]";
    //----------------------------------------------

    Socket session;
    private ReceiveingThread receiveingThread;
    private SendingThread sendingThread;
    Lobby lobby;

    BufferedReader inFromClient;
    DataOutputStream sendToClient;

    public Session(Socket session) throws Exception {
        this.session = session;
        this.lobby = Lobby.getInstance();
        new SetupSessionThread(this, session).start();
    }

    void parseCommand(String received) {
        System.out.println("Session: received: " + received);

        String[] commands = received.split(";");

        for (String command : commands) {
            if (command.matches("enterLobby:" + REGEX_NAME)) {
                String[] words = command.split(":");
                System.out.println("Creating Player " + words[1]);
                lobby.createPlayer(words[1]);
            } else if (command.matches("createGame:" + REGEX_NAME + ":" + REGEX_NAME)) {
                String[] words = command.split(":");
                Player owner = lobby.getPlayerByName(words[2]);
                System.out.println("Creating game " + words[1] + ", owner=" + words[2]);
                lobby.createGame(words[1], owner);
            } else if (command.matches("joinGame:" + REGEX_GAME_ID + ":addPlayer:" + REGEX_NAME)) ;
            else if (command.equals("startGame")) ;
            if (command.equals("listGames")) {
                String gameList = listGames();
                System.out.println("To send: " + gameList);
                System.out.println(send(gameList) == 0 ? "Session: sent" : "Session: unable to send");
            }
        }
    }

    private String listGames() {
        if (lobby.getGames().size() == 0) return "empty";
        StringBuilder builder = new StringBuilder();
        ArrayList<GameModel> games = lobby.getGames();
        for (int i = 0; i < games.size()-1; i++) {
            GameModel game = games.get(i);
            builder.append(game.getName()).append(":").append(game.getId()).append(";");
        }
        builder.append(games.get(games.size()-1).getName()).append(":").append(games.get(games.size()-1).getId());
        return builder.toString();
    }

    private int send(String toClient) {
        if (sendingThread == null) return -1;
        sendingThread.setCommand(toClient);
        return 0;
    }

    void setReceiveingThread(ReceiveingThread receiveingThread) {
        this.receiveingThread = receiveingThread;
    }

    void setSendingThread(SendingThread sendingThread) {
        this.sendingThread = sendingThread;
    }
}
