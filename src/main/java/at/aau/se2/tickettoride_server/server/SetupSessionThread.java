package at.aau.se2.tickettoride_server.server;

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
            System.out.println("SetupSessionThread: setting up communication threads ... ");
            sendingThread = new SendingThread(socket);
            receivingTread = new ReceivingThread(socket, session);
            sendingThread.start();
            receivingTread.start();
            session.setSendingThread(sendingThread);
            session.setReceivingThread(receivingTread);
            System.out.println("SetupSessionThread: launched communication threads");
        } catch (Exception e) {
            System.out.println("SetupSessionThread: Failed to launch communication threads");
            System.out.println(e.getMessage());
        }
    }
}
