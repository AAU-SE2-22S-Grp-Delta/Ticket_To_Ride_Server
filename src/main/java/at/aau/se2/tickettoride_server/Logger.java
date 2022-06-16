package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.Configuration_Constants;

public class Logger {
    public static void debug(String msg) {
        if (Configuration_Constants.DEBUG) {
            System.out.println("(DEBUG)\t" + msg);
        }
    }

    public static void exception(String msg) {
        System.out.println("(EXCEPTION)\t" + msg);
    }

    public static void fatal(String msg) {
        System.out.println("(FATAL)\t" + msg);
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void verbose(String msg) {
        if (Configuration_Constants.VERBOSE) {
            System.out.println("(VERBOSE)\t" + msg);
        }
    }
}
