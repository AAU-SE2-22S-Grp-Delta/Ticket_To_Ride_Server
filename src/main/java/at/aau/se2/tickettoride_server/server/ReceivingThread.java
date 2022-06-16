package at.aau.se2.tickettoride_server.server;

import at.aau.se2.tickettoride_server.Logger;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

public class ReceivingThread extends Thread {
    private final Session session;
    private final BufferedReader receive;

    public ReceivingThread(Socket clientSocket, Session session) throws Exception {
        this.receive = new BufferedReader(new InputStreamReader(new DataInputStream(clientSocket.getInputStream())));
        this.session = session;
    }

    @Override
    public void run() {
        Logger.log("ReceivingThread: has been started and is listening ...");
        while (true) {
            try {
                String line = receive.readLine();
                Logger.verbose("ReceivingThread: received " + line);
                session.parseCommand(line);
            } catch (SocketException se) {
                Logger.debug("ReceivingThread: lost connection");
                //Todo close session
                Logger.debug("ReceivingThread: shutting down ...");
                Logger.exception(se.getMessage());
                break;
            } catch (NullPointerException npe) {
                Logger.debug("ReceivingThread: lost connection");
                //Todo close session
                Logger.exception(npe.getMessage());
                Logger.debug("ReceivingThread: shutting down ...");
                break;
            } catch (Exception e) {
                Logger.debug("ReceivingThread: Some error occurred ...");
                Logger.exception(e.getMessage());
            }
        }
    }
}
