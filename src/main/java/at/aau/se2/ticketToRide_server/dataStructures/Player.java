package at.aau.se2.ticketToRide_server.dataStructures;

import at.aau.se2.ticketToRide_server.models.GameModel;
import at.aau.se2.ticketToRide_server.server.Configuration_Constants;
import at.aau.se2.ticketToRide_server.server.Lobby;
import at.aau.se2.ticketToRide_server.server.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Player-Class represents a person who is playing the Game
 */
public class Player implements Comparable {

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
    private GameModel game;

    private Session session;

    //game objects
    private int numStones;
    private ArrayList<TrainCard> handCards;
    private ArrayList<Mission> missions;
    private final ArrayList<RailroadLine> ownsRailroads = new ArrayList<>();
    //todo completed missions
    int points = 0;


    public Player(String name, Session session) {
        this.id = id++;
        setName(name);
        this.state = State.LOBBY;
        this.session = session;
    }


    //region ----------------------------------- LOBBY REQUESTS ------------------------------------------------------


    public String listPlayersLobby() {
        return Lobby.getInstance().listPlayersLobby();
    }


    public String listGames() {
        return Lobby.getInstance().listGames();
    }


    public String listPlayersGame(String gameName) {
        return Lobby.getInstance().listPlayersGame(gameName);
    }


    public String getGameState(String gameName) {
        return Lobby.getInstance().listPlayersGame(gameName);
    }


    //endregion




    //region ----------------------------------- LOBBY ACTIONS -------------------------------------------------------


    public static Player enterLobby(String name, Session session) {
        Player player = Lobby.getInstance().enterLobby(name, session);
        if (player == null) return null;
        return player;
    }


    public int createGame(String gameName) {
        GameModel game = Lobby.getInstance().createGame(gameName, this);
        if (game == null) return -1;
        return this.joinGame(gameName);
    }


    /**
     * Player joins the specified game if in state waiting for players
     *
     * @param gameName the name of the game
     * @return 0 on success, -1 on fail
     */
    public int joinGame(String gameName) {
        if (state.equals(State.GAMING)) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.joinGame() called while Player " + name + " was in game " + this.game.getName());
            return -1;
        }

        GameModel game = Lobby.getInstance().joinGame(gameName, this);
        if (game == null) {
            return -1;
        }

        state = State.GAMING;
        this.numStones = 45;
        this.handCards = new ArrayList<>();
        this.missions = new ArrayList<>();
        this.game = game;
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tPlayer joint game " + this.game.getName());
        return 0;
    }


    public int leave() {
        //TODO impl
        return -1;
    }


    //endregion




    //region ------------------------------------ GAME REQUESTS --------------------------------------------------------


    public String getHandCards() {
        if (this.state != State.GAMING) {
            return "getHandCards:null";
        }
        StringBuilder handCards = new StringBuilder("getHandCards:");
        for (TrainCard card : this.handCards) {
            handCards.append(card.getType().toString()).append(".");
        }
        return handCards.toString();
    }


    public String getOpenCards() {
        if (state != State.GAMING) return "openHandCard:null";
        return game.getOpenCards();
    }


    public String getMap() {
        if (state != State.GAMING) return "getMap:null";
        return game.getMap();
    }


    /**
     * Sends the points of all players in a string representation
     * to the client
     * @return format: getPoints:Player120.Player215. on success | getPoints:null on fail
     */
    public String getPoints() {
        if (state != State.GAMING) return "getPoints:null";
        return game.getPoints();
    }


    public String getColors() {
        if (state != State.GAMING) return "getColor:null";
        return game.getColors();
    }


    public String getMissions() {
        String retVal = "getMissions:null";
        if (state != State.GAMING) return retVal;
        synchronized (missions) {

            StringBuilder builder = new StringBuilder("getMissions");
            for (Mission mission : missions) {
                builder.append(":").append(mission.getId());
            }
            retVal= builder.toString();
            missions.notify();
        }
        return retVal;
    }

    public String cheatMission() {
        if( state != State.GAMING) {
            return "cheatMission:null";
        }
        return game.cheatMission();
    }


    public String getNumStones() {
        if (state != State.GAMING) return "getNumStones:null";
        return "getNumStones:" + ((Integer) numStones).toString();
    }


    //endregion




    //region ----- GAME COMMANDS ---------------------------------------------------------------------------------------


    /**
     * Tries to start the game
     * Games are started by their owners
     *
     * @return 0 on success, -1 on fail
     */
    public int startGame() {
        if (state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.startGame() called while not in game");
            return -1;
        }

        return this.game.startGame(this);
    }


    public int drawCardStack() {
        if (this.state != State.GAMING) {
            sendCommand("cardStack:null");
            return -1;
        }
        return game.drawCardFromStack(this);
    }


    public String drawMission() {
        if (this.state != State.GAMING) return "drawMission:null";
        return game.drawMission(this);
    }


    public int chooseMissions(LinkedList<Integer> chosen) {
        if (this.state != State.GAMING) return -1;
        return game.chooseMissions(chosen, this);
    }


    public int drawCardOpen(int id) {
        if (this.state != State.GAMING) return -1;
        return game.drawOpenCard(this, id);
    }


    public int buildRailroadLine(String dest1, String dest2, String color) {
        if (state != State.GAMING) return -1;

        RailroadLine railroadLine = game.getRailroadLineByName(dest1, dest2);
        if (railroadLine == null || numStones < railroadLine.getDistance()) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() Error while trying " + dest1 + " to " + dest2);
            return -1;
        }

        MapColor c = MapColor.getByString(color);
        if (railroadLine instanceof DoubleRailroadLine) {
            DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;

            if (doubleRailroadLine.getColor() != MapColor.GRAY && doubleRailroadLine.getColor2() != MapColor.GRAY && doubleRailroadLine.getColor() != c && doubleRailroadLine.getColor2() != c) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\t Player.buildRailroadLine() no Rail of such color! railroad from " + dest1 + " to " + dest2);
                return -1;
            }
        }
        else if (railroadLine.getColor() != MapColor.GRAY && railroadLine.getColor() != c) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() no Rail of such color! railroad from " + dest1 + " to " + dest2);
            return -1;
        }

        LinkedList<TrainCard> cards = getCardsToBuildRail(TrainCard.map_mapColor_to_TrainCardType(c), railroadLine.getDistance());
        if (cards == null) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " not enough cards of color " + c + ". Railroad from " + dest1 + " to " + dest2);
            return -1;
        }
        if (game.setRailRoadLineOwner(this, railroadLine, c, cards)==0) {
            this.handCards.removeAll(cards);
            this.points += getPointsForRoutes(railroadLine.getDistance());
            this.numStones -= railroadLine.getDistance();
            this.ownsRailroads.add(railroadLine);
            checkIfMissionsCompleted();
            if (Configuration_Constants.verbose)
                System.out.println("(DEBUG)\t Player.buildRailroadLine() Player " + this.name + " built railroad from " + dest1 + " to " + dest2);
            return 0;
        }
        else return -1;
    }


    private LinkedList<TrainCard> getCardsToBuildRail(TrainCard.Type cardType, int amount) {
        LinkedList<TrainCard> cards = new LinkedList<>();
        for (TrainCard card : this.handCards) {
            if (card.getType() == cardType) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        for (TrainCard card : this.handCards) {
            if (card.getType() == TrainCard.Type.LOCOMOTIVE) cards.add(card);
            if (amount <= cards.size()) return cards;
        }
        return null;
    }


    public int exitGame() {
        if (this.state != State.GAMING) return -1;
        game.exitGame(this);
        this.state = State.LOBBY;
        return 0;
    }


    //endregion




    //region ------------------------------------ ACTION HELPER METHODS ------------------------------------------------


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


    private void checkIfMissionsCompleted() {
        synchronized (missions) {
            for (Mission mission : missions) {
                if (!mission.isDone()) {
                    LinkedList<Destination> visited = new LinkedList<>();
                    LinkedList<Destination> toProcess = new LinkedList<>();

                    toProcess.add(mission.getDestination1());
                    while (toProcess.size() > 0) {
                        Destination currentDest = toProcess.remove(0);
                        for (RailroadLine line : this.ownsRailroads) {
                            if (line.getDestination1().equals(currentDest) && !visited.contains(line.getDestination2())) {
                                if (line.getDestination2().equals(mission.destination2)) {
                                    mission.setDone();
                                    return;
                                }
                                toProcess.add(line.getDestination2());
                            } else if (line.getDestination2().equals(currentDest) && !visited.contains(line.getDestination1())) {
                                if (line.getDestination1().equals(mission.destination2)) {
                                    mission.setDone();
                                    return;
                                }
                                toProcess.add(line.getDestination1());
                            }
                        }
                        visited.add(currentDest);
                    }
                }
            }
            missions.notify();
        }
    }


    //endregion




    //region ------------------------------------- INTERFACE GAME PLAYER -----------------------------------------------


    public void setPoints(int points) {
        this.points = points;
    }


    public void addHandCard(TrainCard card) {
        if (this.state != State.GAMING) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG\tPlayer: Tried to add HandCard while player " + name + "wasn't in a game.");
            throw new IllegalStateException("Player is not in Game!");
        }
        if (Configuration_Constants.verbose)
            System.out.println("(VERBOSE)\tPlayer.addHandCard() Card=" + card.getType().toString());
        if (card == null) throw new IllegalArgumentException("card is Null!");
        this.handCards.add(card);
        Collections.sort(handCards);
    }


    public void addMission(Mission mission) {
        synchronized (mission) {
            if (this.state != State.GAMING) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG\tPlayer: Tried to add mission while player " + name + "wasn't in a game.");
                throw new IllegalStateException("Player is not in Game!");
            }
            this.missions.add(mission);
            mission.notify();
        }
    }


    public void setPlayerColor(Color playerColor) {
        if (this.state == State.GAMING && this.playerColor != null) {
            if (Configuration_Constants.debug)
                System.out.println("(DEBUG)\tCalled Player.setPlayerColor() while Player was in Game!");
            return;
        }
        this.playerColor = playerColor;
    }


    public int getStones() {
        return this.numStones;
    }


    public void missionInit() {
        session.drawMission();
    }


    //endregion




    //region ------------------------------------- ENDING GAME METHODS -------------------------------------------------


    public int calculatePointsAtGameEnd(int additionalPoints) {
        //TODO call this method at the end of the game
        synchronized (missions) {
            //Punkte von Zielkarten dazuzählen und abziehen
            for (Mission mission : this.missions) {
                if (mission.isDone()) points = mission.getPoints();
                else points -= mission.getPoints();
            }

            //Zusatzpunkte für längste Strecke
            points += additionalPoints;
            notify();
        }
        return points;
    }


    public int findLongestConnection() {
        ArrayList<Integer> connectedRailroadLength = new ArrayList<>();
        for (RailroadLine railroadLine : this.ownsRailroads) {
            //Add length of connection from railroad
            connectedRailroadLength.add(findRailroadLine(railroadLine));
        }

        //Find the longest connection
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
     * prompts the client, that a player has cheated
     */
    public void cheat() {
        sendCommand("cheat");
    }


    /**
     * Notifies this player that this is player [name]'s turn
     */
    public void actionCall(String playerOnTheMove, int actionPoints) {
        this.sendCommand("actionCall:" + playerOnTheMove + ":" + actionPoints);
    }


    public void gameOver() {
        sendCommand("gameOver");
    }


    private void sendCommand(String command) {
        session.send(command);
    }


    //endregion




    // region ------------------------------ SETTER GETTER TO STRING ---------------------------------------------------


    public State getState() {
        return state;
    }


    public int getPlayerPoints() {
        return points;
    }


    public Color getPlayerColor() {
        return playerColor;
    }


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
