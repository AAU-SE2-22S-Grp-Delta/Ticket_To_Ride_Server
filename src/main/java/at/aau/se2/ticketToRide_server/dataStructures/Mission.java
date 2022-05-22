package at.aau.se2.ticketToRide_server.dataStructures;

public class Mission {
    private static int idCounter = 0;

    int id;
    Destination destination1, destination2;
    private int points;
    private boolean done;

    public Mission(Destination destination1, Destination destination2, int points) {
        this.id = idCounter++;
        this.destination1 = destination1;
        this.destination2 = destination2;
        this.points = points;
        done = false;
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

    public int getId() {
        return id;
    }

    public void setDone() {
        this.done = true;
    }

    public boolean isDone() {
        return done;
    }
}
