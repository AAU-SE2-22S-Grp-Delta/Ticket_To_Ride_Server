package at.aau.se2.ticketToRide_server.dataStructures;



public class TrainCard implements Comparable{


    public enum Type {
        BLUE, GREEN, YELLOW, RED, WHITE, ORANGE, BLACK, LOCOMOTIVE;
    }

    private Type type;

    public TrainCard(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof TrainCard) {
            if (((TrainCard) o).type == this.type) return 0;
            return 1;
        }
        if (o instanceof MapColor) {
            MapColor color = (MapColor) o;
            switch (this.type) {
                case BLUE:
                    if (color == MapColor.BLUE) return 0;
                    return 1;
                case RED:
                    if (color == MapColor.RED) return 0;
                    return 1;
                case BLACK:
                    if (color == MapColor.BLACK) return 0;
                    return 1;
                case GREEN:
                    if (color == MapColor.GREEN) return 0;
                    return 1;
                case WHITE:
                    if (color == MapColor.WHITE) return 0;
                    return 1;
                case ORANGE:
                    if (color == MapColor.ORANGE) return 0;
                    return 1;
                case YELLOW:
                    if (color == MapColor.YELLOW) return 0;
                    return 1;
            }
        }

        return -1;
    }
}
