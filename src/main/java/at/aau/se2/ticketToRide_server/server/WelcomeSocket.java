package at.aau.se2.ticketToRide_server.server;

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
                System.out.println("Server running on port 8001");

                while (active) {
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tWelcomeSocket: waiting for connection... ");
                    Socket connection = welcomeSocket.accept();
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tWelcomeSocket: connection requested");
                    Session session = new Session(connection);
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tWelcomeSocket: launched session");
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void shutdown() {
        this.active = false;
    }
}

