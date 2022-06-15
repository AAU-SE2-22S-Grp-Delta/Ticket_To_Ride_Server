package at.aau.se2.ticketToRide_server.server;

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
        System.out.println("ReceivingThread: has been started and is listening ...");
        while (true) {
            try {
                String line = receive.readLine();
                System.out.println("ReceivingThread: received " + line);
                session.parseCommand(line);
            } catch (SocketException se) {
                System.out.println("ReceivingThread: lost connection");
                //Todo close session
                System.out.println("ReceivingThread: shutting down ...");
                se.printStackTrace();
                break;
            } catch (NullPointerException npe) {
                System.out.println("ReceivingThread: lost connection");
                //Todo close session
                npe.printStackTrace();
                System.out.println("ReceivingThread: shutting down ...");
                break;
            } catch (Exception e) {
                System.out.println("ReceivingThread: Some error occurred ...");
                e.printStackTrace();
            }
        }
    }
}
