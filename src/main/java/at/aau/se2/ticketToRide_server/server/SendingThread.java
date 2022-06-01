package at.aau.se2.ticketToRide_server.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendingThread extends Thread {
    private final DataOutputStream send;
    private final Object lock = new Object();
    private String command = null;

    public SendingThread(Socket clientSocket) throws Exception {
        this.send = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("SendingThread: has been started ...");
                synchronized (lock) {
                    if (command == null) {
                        System.out.println("SendingThread: Pause sending thread");
                        lock.wait();
                        System.out.println("SendingThread: Continue sending thread");
                    }
                    if (command != null && sendCommand(command) == 0) {
                        command = null;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setCommand(String command) {
        this.command = command;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public int sendCommand(String command) {
        try {
            System.out.println("SendingThread: sending " + command);
            send.writeBytes(command + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
}
