package at.aau.se2.ticketToRide_server.dataStructures;

import java.util.Comparator;

public class TrainCard implements Comparable{


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


    public static Type map_mapColor_to_TrainCardType(MapColor mapColor) {
        switch (mapColor) {
            case YELLOW:
                return Type.YELLOW;
            case ORANGE:
                return Type.ORANGE;
            case WHITE:
                return Type.WHITE;
            case BLACK:
                return Type.BLACK;
            case BLUE:
                return Type.BLUE;
            case GREEN:
                return Type.GREEN;
            case PINK:
                return Type.PINK;
            case RED:
                return Type.RED;
        }
        return null;
    }


    @Override
    public int compareTo(Object o) {
        if (!(o instanceof TrainCard)) return -100;
        return Integer.compare(mapCardToInt((TrainCard) o), mapCardToInt(this));
    }

    private int mapCardToInt(TrainCard card) {
        switch (card.type) {
            case LOCOMOTIVE: return 1;
            case RED: return 2;
            case GREEN: return 3;
            case BLUE: return 4;
            case ORANGE: return 5;
            case BLACK: return 6;
            case WHITE: return 7;
            case PINK: return 8;
            case YELLOW: return 9;
        }
        return -1;
    }
}
