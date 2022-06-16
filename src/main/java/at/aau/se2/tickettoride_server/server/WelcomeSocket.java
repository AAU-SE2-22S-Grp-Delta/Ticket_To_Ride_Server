package at.aau.se2.tickettoride_server.server;

import at.aau.se2.tickettoride_server.Logger;

import java.net.*;

public class WelcomeSocket implements Runnable {
    private boolean active = false;

    public WelcomeSocket() {
    }

    @Override
    public void run() {
        this.active = true;
        launchWelcomeSocket();
    }

    private void launchWelcomeSocket() {
        try {
            try (ServerSocket welcomeSocket = new ServerSocket(8001)) {
                Logger.log("Server running on port 8001");

                while (active) {
                    Logger.verbose("WelcomeSocket: waiting for connection... ");
                    Socket connection = welcomeSocket.accept();
                    Logger.verbose("WelcomeSocket: connection requested");
                    Session session = new Session(connection);
                    Logger.verbose("WelcomeSocket: launched session");
                }
            }
        } catch (Exception e) {
            Logger.exception(e.getMessage());
        }

    }

    public void shutdown() {
        this.active = false;
    }
}

