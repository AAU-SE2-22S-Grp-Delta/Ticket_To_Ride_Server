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
            ServerSocket welcomeSocket = new ServerSocket(8001);
            System.out.println("Server running on port 8001");

            while (active) {
                System.out.println("WelcomeSocket: waiting for connection... ");
                Socket connection = welcomeSocket.accept();
                System.out.println("WelcomeSocket: connection requested");
                new Session(connection);
                System.out.println("WelcomeSocket: launched session");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void shutdown() {
        this.active = false;
    }
}

