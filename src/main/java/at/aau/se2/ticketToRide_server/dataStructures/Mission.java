package at.aau.se2.ticketToRide_server.dataStructures;

public class Mission {
    Destination destination1, destination2;

    public Mission(Destination destination1, Destination destination2) {
        this.destination1 = destination1;
        this.destination2 = destination2;
    }

    public Destination getDestination1() {
        return destination1;
    }

    public Destination getDestination2() {
        return destination2;
    }
}
