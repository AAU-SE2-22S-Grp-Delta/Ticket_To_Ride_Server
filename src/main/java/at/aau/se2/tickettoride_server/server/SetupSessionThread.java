package at.aau.se2.tickettoride_server.server;

import at.aau.se2.tickettoride_server.Logger;

import java.net.Socket;

public class SetupSessionThread extends Thread {
    private Session session;

    protected Socket socket;
    private SendingThread sendingThread;
    private ReceivingThread receivingTread;

    public SetupSessionThread(Session session, Socket socket) {
        this.session = session;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            Logger.log("SetupSessionThread: setting up communication threads ... ");
            sendingThread = new SendingThread(socket);
            receivingTread = new ReceivingThread(socket, session);
            sendingThread.start();
            receivingTread.start();
            session.setSendingThread(sendingThread);
            session.setReceivingThread(receivingTread);
            Logger.log("SetupSessionThread: launched communication threads");
        } catch (Exception e) {
            Logger.log("SetupSessionThread: Failed to launch communication threads");
            Logger.exception(e.getMessage());
        }
    }
}
