package at.aau.se2.ticketToRide_server.dataStructures;

public class Mission {
    Destination destination1, destination2;
    private int points;

    public Mission(Destination destination1, Destination destination2, int points) {
        this.destination1 = destination1;
        this.destination2 = destination2;
        this.points = points;
    }

    public Destination getDestination1() {
        return destination1;
    }

    public Destination getDestination2() {
        return destination2;
    }

    public int getPoints() {
        return points;
    }
}
