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
    private PreparedRailMaterial material = null;


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

    public int chooseMissoions(LinkedList<Integer> chosen) {
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


    public PreparedRailMaterial prepareRailroadLine(String dest1, String dest2) {
        RailroadLine railroadLine = game.getRailroadLineByName(dest1, dest2);
        if (railroadLine == null) {
            return null;
        }

        if (railroadLine instanceof DoubleRailroadLine) {
            DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;
            MapColor color1 = doubleRailroadLine.getColor();
            MapColor color2 = doubleRailroadLine.getColor2();

            LinkedList<TrainCard> cardsOfColor1 = getCardsToBuildRail(color1, railroadLine.getDistance());
            LinkedList<TrainCard> cardsOfColor2 = getCardsToBuildRail(color2, railroadLine.getDistance());

            if (cardsOfColor1 != null && cardsOfColor2 != null) {
                PreparedRailMaterial material = new PreparedRailMaterial();
                material.cards1 = cardsOfColor1;
                material.cards2 = cardsOfColor2;
                material.railroadLine = railroadLine;
                sendCommand("aknRailroad:" + dest1 + ":" + dest2 + ":" + color1 + ":" + color2);
                return null;
            } else if (cardsOfColor1 != null) {
                PreparedRailMaterial material = new PreparedRailMaterial();
                material.cards1 = cardsOfColor1;
                material.railroadLine = railroadLine;
                sendCommand("aknRailroad:" + dest1 + ":" + dest2 + ":" + color2);
                return material;
            } else if (cardsOfColor2 != null) {
                PreparedRailMaterial material = new PreparedRailMaterial();
                material.cards2 = cardsOfColor2;
                sendCommand("aknRailroad:" + dest1 + ":" + dest2 + ":" + color1);
                return material;
            }
            sendCommand("aknRailroad:null");
            return null;
        }

        LinkedList<TrainCard> cards = getCardsToBuildRail(railroadLine.getColor(), railroadLine.getDistance());
        if (cards != null) {
            this.material = new PreparedRailMaterial();
            material.cards1 = cards;
            material.railroadLine = railroadLine;
            return material;
        }
        return null;
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


    public int confirmBuild(String dest1, String dest2, String color) {
        if (this.material == null) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG) Player.confirmBuild(): No prepared material for player of name " + name);
            sendCommand("aknRailRoad:null");
            return -1;
        }

        String d1 = material.railroadLine.getDestination1().getName();
        String d2 = material.railroadLine.getDestination2().getName();

        if (!((d1.equals(dest1) && d2.equals(dest2)) || (d2.equals(dest1) && d1.equals(dest2)))) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG) Player.confirmBuild(): Destinations " + dest1 + " " + dest2 + " doesn't fit to RailroadLine from " + d1 + " to " + d2);
            sendCommand("aknRailRoad:null");
            return -1;
        }

        MapColor c = MapColor.getByString(color);
        if (c == null) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG) Player.confirmBuild(): Illegal color string: " + color);
            sendCommand("aknRailRoad:null");
            return -2;
        }
        if (game.setRailRoadLineOwner(this, material.railroadLine, c) == 0) {
            LinkedList<TrainCard> toRemove = null;
            if (material.railroadLine instanceof DoubleRailroadLine) {
                DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) material.railroadLine;
                if (c == doubleRailroadLine.getColor()) {
                    toRemove = material.cards1;
                }
                if (c == doubleRailroadLine.getColor2()) {
                    toRemove = material.cards2;
                }
                this.handCards.removeAll(toRemove);
                material = null;
                return 0;
            }
            this.handCards.removeAll(material.cards1);
            material = null;
            return 0;
        }
        sendCommand("aknRailRoad:null");
        return -1;
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
