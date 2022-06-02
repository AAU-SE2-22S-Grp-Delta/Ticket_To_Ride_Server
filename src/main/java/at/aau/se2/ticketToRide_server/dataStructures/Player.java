package at.aau.se2.ticketToRide_server.dataStructures;

import at.aau.se2.ticketToRide_server.models.GameModel;
import at.aau.se2.ticketToRide_server.server.Configuration_Constants;
import at.aau.se2.ticketToRide_server.server.Session;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;

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
        LOBBY, GAMING;
    }

    //player management
    private String name;
    private int id = 0;
    private Color playerColor;
    private State state;
    private GameModel game;

    private Session session;

    //game objects
    private int numStones;
    private ArrayList<TrainCard> handCards;
    private ArrayList<Mission> missions;
    private ArrayList<RailroadLine> ownsRailroads;
    //todo completed missions
    int points = 0;


    public Player(String name, Session session) {
        this.id = id++;
        setName(name);
        this.state = State.LOBBY;
        this.session = session;
    }


    //region ----------------------------------- GENERAL ACTIONS -------------------------------------------------------

    /**
     * Player joins the specified game if in state waiting for players
     *
     * @param game
     * @return 0 on success, -1 on fail
     */
    public int joinGame(GameModel game) {
        if (state.equals(State.GAMING)) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.joinGame() called while Player " + name + " was in game " + this.game.getName());
            return -1;
        }
        state = State.GAMING;
        this.numStones = 45;
        this.handCards = new ArrayList<>();
        this.missions = new ArrayList<>();
        if (game.addPlayer(this) < 0) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Game is full");
            return -2;
        }
        this.game = game;
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tPlayer joint game " + this.game.getName());
        return 0;
    }

    //endregion


    //region ----------------------------------- IN GAME METHODS -------------------------------------------------------


    public int startGame() {
        return -1;
    }

    public TrainCard getCardStack() {
        return null;
    }

    public TrainCard getCardOpen(int id) {
        return null;
    }

    public int chooseMissions(LinkedList<Integer> chosen) {
        return -1;
    }

    public int exitGame() {
        return -1;
    }


    public void setPoints(int points) {
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


    public void addHandCard(TrainCard card) {
        if (this.state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG\tPlayer: Tried to add HandCard while player " + name + "wasn't in a game.");
            throw new IllegalStateException("Player is not in Game!");
        }
        if (card == null) throw new IllegalArgumentException("card is Null!");
        this.handCards.add(card);
    }


    public void buildRailroadLine(String dest1, String dest2, String color) {
        RailroadLine railroadLine = game.getRailroadLineByName(dest1, dest2);
        if (railroadLine == null) {
            sendCommand("buildRailroad:null");
            return;
        }

        MapColor c = MapColor.getByString(color);
        if (railroadLine instanceof DoubleRailroadLine) {
            DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;

            if (doubleRailroadLine.getColor() != c || (doubleRailroadLine.getColor() != c && doubleRailroadLine.getColor2() != c)) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\t Player.buildRailroadLine() no Rail of such color! railroad from " + dest1 + " to " + dest2);
                sendCommand("buildRailroad:null");
                return;
            }

            LinkedList<TrainCard> cards = getCardsToBuildRail(c, railroadLine.getDistance());
            if (cards == null) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " not enough cards of color " + c + ". Railroad from " + dest1 + " to " + dest2);
                sendCommand("buildRailroad:null");
                return;
            }
            game.setRailRoadLineOwner(this, railroadLine, c);
            this.handCards.removeAll(cards);
            if (Configuration_Constants.verbose)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " built railroad from " + dest1 + " to " + dest2);
        }

        LinkedList<TrainCard> cards = getCardsToBuildRail(c, railroadLine.getDistance());
        if (cards == null) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " not enough cards of color " + c + ". Railroad from " + dest1 + " to " + dest2);
            sendCommand("buildRailroad:null");
            return;
        }
        game.setRailRoadLineOwner(this, railroadLine, c);
        this.handCards.removeAll(cards);
        this.points = getPointsForRoutes(railroadLine.getDistance());
        if (Configuration_Constants.verbose)
            System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " built railroad from " + dest1 + " to " + dest2);
        return;
    }


    private LinkedList<TrainCard> getCardsToBuildRail(MapColor color, int amount) {
        LinkedList<TrainCard> cards = new LinkedList<>();
        for (TrainCard card : this.handCards) {
            if (card.equals(color)) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        for (TrainCard card : this.handCards) {
            if (card.getType() == TrainCard.Type.LOCOMOTIVE) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        return null;
    }


    //Punkte für vollständige Strecken
    private int getPointsForRoutes(int lengthOfRoute) {
        switch (lengthOfRoute) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 7;
            case 5:
                return 10;
            case 6:
                return 15;
            default:
                throw new IllegalStateException("Unexpected value: " + lengthOfRoute);
        }
    }


    public void addMission(Mission mission) {
        if (this.state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG\tPlayer: Tried to add mission while player " + name + "wasn't in a game.");
            throw new IllegalStateException("Player is not in Game!");
        }
        this.missions.add(mission);
    }


    private boolean checkIfCompleted(Mission mission) {
        LinkedList<Destination> visited = new LinkedList<>();
        LinkedList<Destination> toProcess = new LinkedList<>();

        toProcess.add(mission.getDestination1());
        while (toProcess.size() > 0) {
            Destination currentDest = toProcess.remove(0);
            for (RailroadLine line : this.ownsRailroads) {
                if (line.getDestination1().equals(currentDest) && !visited.contains(line.getDestination2())) {
                    if (line.getDestination2().equals(mission.destination2)) return true;
                    toProcess.add(line.getDestination2());
                } else if (line.getDestination2().equals(currentDest) && !visited.contains(line.getDestination1())) {
                    if (line.getDestination1().equals(mission.destination2)) return true;
                    toProcess.add(line.getDestination1());
                }
            }

            visited.add(currentDest);
        }

        return false;
    }


    //endregion


    //region ---------------------------- ENDING GAME METHODS ----------------------------------------------------------


    public void calculatePointsAtGameEnd() {
        //TODO call this method at the end of the game

        //Punkte von Zielkarten dazuzählen und abziehen
        for (Mission mission : this.missions) {
            if (mission.isDone()) points = mission.getPoints();
            else points -= mission.getPoints();
        }

        //Zusatzpunkte für längste Strecke
        if (game.hasLongestRailroad(this)) points += 10;
    }

    public int findLongestConnection() {
        ArrayList<Integer> connectedRailroadLength = new ArrayList<>();
        for (RailroadLine railroadLine : this.ownsRailroads) {
            //Add length of connection from railroad
            connectedRailroadLength.add(findRailroadLine(railroadLine));
        }

        //Find longest connection
        int longestConnection = 0;
        for (Integer counter : connectedRailroadLength) {
            if (counter > longestConnection) longestConnection = counter;
        }

        return longestConnection;
    }

    //Count length of connection
    private int findRailroadLine(RailroadLine railroadLine) {
        Destination startDestination = railroadLine.getDestination2();
        int counter = 0;
        for (RailroadLine otherRailroadLine : this.ownsRailroads) {
            if (startDestination == otherRailroadLine.getDestination1()) {
                counter++;
                startDestination = otherRailroadLine.getDestination2();
            }
        }
        return counter;
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
     * Notifies this player that this is player [name]'s turn
     */
    public void actionCall(String playerOnTheMove) {
        this.sendCommand("actionCall:"+playerOnTheMove);
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


    public int getNumStones() {
        return numStones;
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

    public GameModel getGame(){
        return game;
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
