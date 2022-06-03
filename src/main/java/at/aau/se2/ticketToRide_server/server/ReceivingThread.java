package at.aau.se2.ticketToRide_server.server;


import java.io.DataInputStream;
import java.net.Socket;
import java.net.SocketException;

public class ReceivingThread extends Thread {
    private Session session;
    protected DataInputStream receive;

    protected ReceivingThread(Socket clientSocket, Session session) throws Exception{
        receive = new DataInputStream(clientSocket.getInputStream());
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
            }
            catch (NullPointerException npe) {
                System.out.println("ReceivingThread: lost connection");
                //Todo close session
                npe.printStackTrace();
                System.out.println("ReceivingThread: shutting down ...");
                break;
            }
            catch (Exception e) {
                System.out.println("ReceivingThread: Some error occurred ...");
                e.printStackTrace();
            }
        }
    }
}
