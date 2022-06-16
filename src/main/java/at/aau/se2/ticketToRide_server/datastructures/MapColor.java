package at.aau.se2.ticketToRide_server.datastructures;

public enum MapColor {
    BLUE("blue"), GREEN("green"), YELLOW("yellow"), RED("red"), WHITE("white"), ORANGE("orange"), GRAY("gray"), BLACK("black"), PINK("pink");

    private String value;

    MapColor(String value) {
    }

    @Override
    public String toString() {
        return value;
    }

    public static MapColor getByString(String color) {
        if (color.equals("blue")) return BLUE;
        if (color.equals("green")) return GREEN;
        if (color.equals("yellow")) return YELLOW;
        if (color.equals("red")) return RED;
        if (color.equals("white")) return WHITE;
        if (color.equals("orange")) return ORANGE;
        if (color.equals("gray")) return GRAY;
        if (color.equals("black")) return BLACK;
        if (color.equals("pink")) return PINK;
        return null;
    }
}
