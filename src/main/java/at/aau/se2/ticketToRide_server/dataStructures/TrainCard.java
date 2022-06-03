package at.aau.se2.ticketToRide_server.dataStructures;

public class TrainCard implements Comparable<Object> {
    public enum Type {
        PINK("pink"), BLUE("blue"), GREEN("green"), YELLOW("yellow"), RED("red"), WHITE("white"), ORANGE("orange"), BLACK("black"), LOCOMOTIVE("locomotive");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static Type getByString(String color) {
            switch (color) {
                case "pink":
                    return PINK;
                case "blue":
                    return BLUE;
                case "green":
                    return GREEN;
                case "yellow":
                    return YELLOW;
                case "red":
                    return RED;
                case "white":
                    return WHITE;
                case "orange":
                    return ORANGE;
                case "black":
                    return BLACK;
                case "locomotive":
                    return LOCOMOTIVE;
                default:
                    return null;
            }
        }
    }

    private final Type type;

    public TrainCard(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }


    @Override
    public String toString() {
        return type.toString();
    }


    @Override
    public int compareTo(Object o) {
        if (o instanceof TrainCard) {
            return ((TrainCard) o).type == this.type ? 0 : 1;
        }

        if (o instanceof MapColor) {
            MapColor color = (MapColor) o;
            switch (this.type) {
                case PINK:
                    return color == MapColor.PINK ? 0 : 1;
                case BLUE:
                    return color == MapColor.BLUE ? 0 : 1;
                case GREEN:
                    return color == MapColor.GREEN ? 0 : 1;
                case YELLOW:
                    return color == MapColor.YELLOW ? 0 : 1;
                case RED:
                    return color == MapColor.RED ? 0 : 1;
                case WHITE:
                    return color == MapColor.WHITE ? 0 : 1;
                case ORANGE:
                    return color == MapColor.ORANGE ? 0 : 1;
                case BLACK:
                    return color == MapColor.BLACK ? 0 : 1;
            }
        }

        return -1;
    }
}
