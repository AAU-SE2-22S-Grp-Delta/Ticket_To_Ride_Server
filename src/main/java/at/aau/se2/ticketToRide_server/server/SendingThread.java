package at.aau.se2.ticketToRide_server.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class SendingThread extends Thread {
    private final DataOutputStream send;
    private final Object lock = new Object();
    private LinkedList<String> outputBuffer = new LinkedList<>();

    public SendingThread(Socket clientSocket) throws Exception {
        this.send = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSendingThread: has been started ...");
        String command = null;
        while (true) {
            try {
                synchronized (lock) {
                    if (outputBuffer.isEmpty()) {
                        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSendingThread: Pause sending thread");
                        lock.wait();
                        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSendingThread: Continue sending thread");
                    }
                    command = outputBuffer.remove();
                }
                sendToClient(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendCommand(String command) {
        synchronized (lock) {
            this.outputBuffer.add(command);
            lock.notify();
        }
    }

    private int sendToClient(String command) {
        try {
            if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tSendingThread: sending " + command);
            send.writeBytes(command + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
}
