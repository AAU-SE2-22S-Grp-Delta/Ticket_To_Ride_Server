package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.WelcomeSocket;

public class Main {
    public static void main(String[] args) {
        WelcomeSocket welcomeSocket = new WelcomeSocket();
        Thread server = new Thread(welcomeSocket);
        server.start();
    }
}
