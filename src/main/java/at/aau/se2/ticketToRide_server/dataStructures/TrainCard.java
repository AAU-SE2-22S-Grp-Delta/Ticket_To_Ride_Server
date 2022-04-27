package at.aau.se2.ticketToRide_server.dataStructures;



public class TrainCard {
    public enum Type {
        BLUE, GREEN, YELLOW, RED, WHITE, ORANGE, GRAY, BLACK, LOCOMOTIVE
    }


    private Type type;

    public TrainCard(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
