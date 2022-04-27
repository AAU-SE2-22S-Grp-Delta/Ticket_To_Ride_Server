package at.aau.se2.ticketToRide_server.models;

import at.aau.se2.ticketToRide_server.dataStructures.*;

import java.util.ArrayList;

enum State {
    WAITING_FOR_PLAYERS, GAMING, ENDED
}

public class GameModel {
    private static int idCounter = 0;

    private int id;
    private String name;
    private State state;
    private int colorCounter = 0;

    private Map map;
    private ArrayList<Player> players;
    private Player owner;
    private ArrayList<TrainCard> trainCards;
    private ArrayList<Mission> missions;

    public GameModel(String name, Player owner) {
        this.id = idCounter++;
        this.name = name;
        this.state = State.WAITING_FOR_PLAYERS;
        players = new ArrayList<>();

        addPlayer(owner);

        map = getMap();
        this.trainCards = getTraincards();
        this.missions = getMissions();
    }

    public int addPlayer(Player player) {
        try {
            if (player == null) throw new IllegalArgumentException("Player is NULL!");
            if (players.size() > 5) throw new IllegalArgumentException("Board is full!");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            return -1;
        }
        players.add(player);
        player.setGaming();
        this.owner = player;
        return 0;
    }

    private static Map getMap() {
        Map map = new Map();

        return map;
    }

    private static ArrayList<TrainCard> getTraincards() {
        ArrayList<TrainCard> cards = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            cards.add(new TrainCard(TrainCard.Type.BLACK));
            cards.add(new TrainCard(TrainCard.Type.BLUE));
            cards.add(new TrainCard(TrainCard.Type.GRAY));
            cards.add(new TrainCard(TrainCard.Type.GREEN));
            cards.add(new TrainCard(TrainCard.Type.LOCOMOTIVE));
            cards.add(new TrainCard(TrainCard.Type.ORANGE));
            cards.add(new TrainCard(TrainCard.Type.RED));
            cards.add(new TrainCard(TrainCard.Type.WHITE));
            cards.add(new TrainCard(TrainCard.Type.YELLOW));
        }

        return cards;
    }

    private static ArrayList<Mission> getMissions() {
        ArrayList<Mission> missions = new ArrayList<>();
        //TODO generate Missions
        return missions;
    }


    public static int getIdCounter() {
        return idCounter;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public Player getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        String toString = "GameModelid=" + id +
                ", name='" + name +
                ", state=" + state
                + ", owner=" + owner.getName()+ "\n"
                + "\tPlayers:";
        for(Player player : players) toString+="\t"+player.toString()+"\n";
        return toString;
    }
}
