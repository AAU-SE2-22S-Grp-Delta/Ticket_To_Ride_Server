package at.aau.se2.tickettoride_server.server;

import at.aau.se2.tickettoride_server.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class SendingThread extends Thread {
    private final DataOutputStream send;
    private final Object lock = new Object();
    private final LinkedList<String> outputBuffer = new LinkedList<>();

    public SendingThread(Socket clientSocket) throws Exception {
        this.send = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        Logger.verbose("SendingThread: has been started ...");
        String command;
        while (true) {
            try {
                synchronized (lock) {
                    if (outputBuffer.isEmpty()) {
                        Logger.verbose("SendingThread: Pause sending thread");
                        lock.wait();
                        Logger.verbose("SendingThread: Continue sending thread");
                    }
                    command = outputBuffer.remove();
                }
                sendToClient(command);
            } catch (InterruptedException e) {
                Logger.exception(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendCommand(String command) {
        synchronized (lock) {
            this.outputBuffer.add(command);
            lock.notifyAll();
        }
    }

    private int sendToClient(String command) {
        try {
            Logger.verbose("SendingThread: sending " + command);
            send.writeBytes(command + "\n");
        } catch (IOException e) {
            Logger.exception(e.getMessage());
            return -1;
        }
        return 0;
    }
}
