package at.aau.se2.ticketToRide_server.dataStructures;

import at.aau.se2.ticketToRide_server.server.Configuration_Constants;
import at.aau.se2.ticketToRide_server.server.Session;

import java.util.ArrayList;


/**
 * Player-Class represents a person who is playing the Game
 */
public class Player implements Comparable {


    public enum Command {
        SYNC, //game model has changed -> prompts the client to synchronize
        DO_MOVE //informs this that the game is waiting for the valid player to perform a move
        ;
    }

    public enum Color {
        RED(0), BLUE(1), GREEN(2), YELLOW(3), BLACK(4);

        Color(int i) {
        }

    }


    public enum State {
        LOBBY, GAMING
    }

    //player management
    private String name;
    private int id = 0;
    private Color playerColor;
    private State state;

    private Session session;

    //game objects
    private int numStones;
    private ArrayList<TrainCard> handCards;
    private ArrayList<Mission> missions;
    private ArrayList<RailroadLine> ownsRailroads;
    //todo completed missions
    int points=0;




    public Player(String name, Session session) {
        this.id = id++;
        setName(name);
        this.state = State.LOBBY;
        this.session = session;
    }









    //region ----------------------------------- IN GAME METHODS -------------------------------------------------------


    public void setGaming() {
        //TODO throws illegal state error / process
        if (state.equals(State.GAMING)) throw new IllegalStateException("Player is already gaming");
        state = State.GAMING;
        this.numStones = 45;
        this.handCards = new ArrayList<>();
        this.missions = new ArrayList<>();
    }

    public int getNumStones() { return numStones;}

    public void setPoints(int points){
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    public ArrayList<TrainCard> getHandCards() {
        return handCards;
    }

    public ArrayList<Mission> getMissions() {
        return missions;
    }

//    public void setNumberOfConnectedRailroads(int numberOfConnectedRailroads) {
//        this.numberOfConnectedRailroads = numberOfConnectedRailroads;
//    }
//
//    public int getNumberOfConnectedRailroads() {
//        return numberOfConnectedRailroads;
//    }

    public void addHandCard(TrainCard card) {
        if (this.state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG\tPlayer: Tried to add HandCard while player " + name + "wasn't in a game.");
            throw new IllegalStateException("Player is not in Game!");
        }
        if (card == null) throw new IllegalArgumentException("card is Null!");
        this.handCards.add(card);
    }


    public boolean buildRailroadLine(RailroadLine railroadLine) {
        if (railroadLine instanceof DoubleRailroadLine) {
            DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;
            MapColor color1 = doubleRailroadLine.getColor();
            MapColor color2 = doubleRailroadLine.getColor2();

            ArrayList<TrainCard> railOfColor1 = getCardsToBuildRail(color1, railroadLine.getDistance());
            ArrayList<TrainCard> railOfColor2 = getCardsToBuildRail(color2, railroadLine.getDistance());

            if (railOfColor1 != null && railOfColor2 != null) {
                //TODO ask player which one and then confirm
            }
            else if (railOfColor1 != null) {
                //TODO ask player to confirm
            }
            else if (railOfColor2 != null) {
                //TODO ask player to confirm
            }
            else {
                //TODO inform player not enough handCards;
            }
            return false;
        }

        ArrayList<TrainCard> cards = getCardsToBuildRail(railroadLine.getColor(), railroadLine.getDistance());
        if (cards != null) {
            //TODO ask player to confirm
            this.ownsRailroads.add(railroadLine);
            this.handCards.removeAll(cards);
            return true;
        }
        return false;
    }


    private ArrayList<TrainCard> getCardsToBuildRail(MapColor color, int amount) {
        ArrayList<TrainCard> cards = new ArrayList<>();
        for (TrainCard card : this.handCards) {
            if (card.equals(color)) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        for (TrainCard card: this.handCards) {
            if (card.getType() == TrainCard.Type.LOCOMOTIVE) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        return null;
    }


    public void addMission(Mission mission) {
        if (this.state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG\tPlayer: Tried to add mission while player " + name + "wasn't in a game.");
            throw new IllegalStateException("Player is not in Game!");
        }
        this.missions.add(mission);
    }


    //endregion




    //region --------------------------- SERVER CLIENT COMMUNICATION ---------------------------------------------------


    /**
     * prompts the client to sync
     */
    public void sync() {
        sendCommand("sync");
    }


    /**
     * informs this client that the game is waiting for the valid player to perform a move
     */
    public void doMove(String playerName, int actionsLeft) {
        sendCommand("doMove:" + playerName + ":" + actionsLeft);
    }


    private int sendCommand(String command) {
        return session.send(command);
    }


    //endregion




    // region ------------------------------ SETTER GETTER TO STRING ---------------------------------------------------


    public int getId() {
        return id;
    }


    //unique name check in lobby
    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name is null");
        if (name.length() == 0) throw new IllegalArgumentException("name.length is 0");
        this.name = name;
    }


    public String getName() {
        return name;
    }


    public Color getPlayerColor() {
        return playerColor;
    }


    public void setPlayerColor(Color playerColor) {
        if (this.state == State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\tCalled Player.getPlayerColor() while Player was in Game!");
            return;
        }
        this.playerColor = playerColor;
    }


    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", playerColor=" + playerColor +
                ", numStones=" + numStones +
                ", state=" + state +
                '}';
    }


    //endregion




    /**
     * compares this player with an object
     *
     * @param player to be compared with this
     * @return 1 if no instanceof Player, 0 if the names are equal, -1 if names differ
     */
    @Override
    public int compareTo(Object player) {
        if (!(player instanceof Player)) return 1;
        if (this.name.equals(((Player) player).name)) return 0;
        else return -1;
    }
}
