package at.aau.se2.tickettoride_server;

import at.aau.se2.tickettoride_server.server.ConfigurationConstants;

public class Logger {
    private Logger() {
    }

    public static void debug(String msg) {
        if (ConfigurationConstants.DEBUG) {
            log("(DEBUG)\t" + msg);
        }
    }

    public static void exception(String msg) {
        log("(EXCEPTION)\t" + msg);
    }

    public static void fatal(String msg) {
        log("(FATAL)\t" + msg);
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void verbose(String msg) {
        if (ConfigurationConstants.VERBOSE) {
            log("(VERBOSE)\t" + msg);
        }
    }
}
