package at.aau.se2.ticketToRide_server.models;

import at.aau.se2.ticketToRide_server.dataStructures.*;
import at.aau.se2.ticketToRide_server.server.Configuration_Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

enum State {
    WAITING_FOR_PLAYERS, RUNNING, OVER, CRASHED
}

public class GameModel implements Runnable {
    private static int idCounter = 0;

    //meta
    private int id;
    private String name;
    private State state;
    private int colorCounter = 0;   //to assign colors to players
    private int actionsLeft;        //to manage a move
    private int countdown = -1;     // for the last moves before end


    //invisible
    private ArrayList<Player> players;
    private Player owner;
    private final ArrayList<TrainCard> trainCardsStack;
    //todo Ablagestapel
    private ArrayList<Mission> missions;
    private int activePlayer = 0;  //counts who is next


    //visible to all
    private final Map map = getMapInstance();
    private ArrayList<TrainCard> openCards = new ArrayList<>();
    private HashMap<Player, Integer> longestConnectionsForEachPlayer = new HashMap<>();

    public GameModel(String name, Player owner) {
        this.id = idCounter++;
        this.name = name;
        this.state = State.WAITING_FOR_PLAYERS;
        players = new ArrayList<>();
        this.trainCardsStack = getTrainCards();
        this.initOpenCards();

        this.missions = getMissions();
        this.owner=owner;
    }

    //region ----------------  WAITING FOR PLAYERS ---------------------------------------

    public int addPlayer(Player player) {
        try {
            if (player == null) throw new IllegalArgumentException("Player is NULL!");
            if (players.size() > 4) throw new IllegalStateException("Board is full!");
            if (this.state != State.WAITING_FOR_PLAYERS) throw new IllegalStateException("Game has already started!");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            return -1;
        }
        players.add(player);
        if (colorCounter > 4) {
            System.out.println("(FATAL) GameModel: colorCounter raised over 5, max value when executing addPlayer at this point should be 5. Execution crashed.");
            exitGameCrashed();
        }
        switch (colorCounter++) {
            case 0:
                player.setPlayerColor(Player.Color.RED);
                break;
            case 1:
                player.setPlayerColor(Player.Color.BLUE);
                break;
            case 2:
                player.setPlayerColor(Player.Color.GREEN);
                break;
            case 3:
                player.setPlayerColor(Player.Color.YELLOW);
                break;
            case 4:
                player.setPlayerColor(Player.Color.BLACK);
                break;
        }

        return 0;
    }


    private void exitGameCrashed() {
        this.state = State.CRASHED;
        //TODO
        //try to recover?
        //endGame
        //notify Players
    }

    //endregion




    //region ------------------- REQUESTS FROM LOBBY -------------------------------


    public String listPlayersGame() {
        StringBuilder builder = new StringBuilder("listPlayersGame");

        synchronized (this) {
            for (Player player : this.players) {
                builder.append(":").append(player.getName());
            }
            this.notify();
        }
        return builder.toString();
    }


    private String getGameState(String command) {
        String state;
        synchronized (this) {
            state = this.state.toString();
            this.notify();
        }
        return state;
    }


    //endregion




    //region -------------------- GAME INITIALIZATION ------------------------------

    //TODO init visible cards
    private void initOpenCards() {
        this.openCards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            this.openCards.add(this.trainCardsStack.remove(0));
        }
    }

    //endregion




    //region ----------------------- GAMING -----------------------------------------

    @Override
    public void run() {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tGameModel.run() Game loop up");
        while (!checkIfOver()) {
            move();
            if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tGameModel.run() Next round");
            activePlayer = ++activePlayer%players.size();
        }
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tGameModel.run() Game loop broke");
        //TODO ending game methods
    }

    /**
     * checks if the game is over
     *
     * @return true on over
     */
    private boolean checkIfOver() {
        if (countdown != -1) {
            countdown--;
        }

        if (countdown == 0) {
            state = State.OVER;
            return true;
        } else {
            for (Player player : players) {
                if (player.getStones() <= 2) {
                    if (countdown != -1) {
                        countdown = players.size();
                    }
                }
            }
            return false;
        }
        //check if each player has at least 2 wagons or, if there is a running countdown
        //if a player has less than 2, each other player has one move left (start count down)
        //if countdown is over, set state to OVER

//        return false;
    }

    /**
     * sends a signal to the player whose turn it is
     * and waits for his move
     * refreshes the model based on the move
     * broadcasts a sync to all clients,
     * while this is waiting, information is still readable
     * while actualisation information is locked
     */
    private void move() {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tGameModel.move() move start");
        this.actionsLeft = 2;
        while (actionsLeft > 0) {
            try {
                synchronized (this) {
                    actionCall();
                    if (Configuration_Constants.verbose)
                        System.out.println("(VERBOSE)\tGameModel.move() called and waiting for action");
                    this.wait(); //Waits until a action is done
                    sync();      //then the sync broadcast
                }
            } catch (Exception e) {
                if (Configuration_Constants.debug) System.out.println("(DEBUG)\t Error in GameModel.move");
                e.printStackTrace();
            }
        }
    }


    /**
     * Broadcasts to all players that this is player [name]'s turn
     */
    private void actionCall() {
        if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\tGameModel.actionCall() calling players...");
        String playerOnTheMove = players.get(this.activePlayer).getName();
        for (Player p : this.players) {
            p.actionCall(playerOnTheMove, actionsLeft);
        }
    }

    /**
     * Notifies all players that the game model has changed
     */
    private void sync() {
        for (Player p : this.players) {
            p.sync();
        }
    }


    /**
     * searches for a railroadLine which names are equal to dest1 and dest2
     *
     * @param destination1 the name of one destination of the line
     * @param destination2 the name of one destination of the line
     * @return the RailroadLine on success, null on fail
     */
    public RailroadLine getRailroadLineByName(String destination1, String destination2) {
        for (RailroadLine r : map.getRailroadLines()) {
            String d1 = r.getDestination1().getName();
            String d2 = r.getDestination2().getName();
            if (destination1.equals(d1) && destination2.equals(d2) || destination1.equals(d2) && destination2.equals(d1)) {
                return r;
            }
        }
        return null;
    }


    //endregion




    //region -------------------------------- END GAME METHODS ---------------------------------------------------------


    private void calculatePoints() {
        for (Player player : this.players) {
            player.calculatePointsAtGameEnd();
        }
    }

    public boolean hasLongestRailroad(Player player) {
        getLongestConnectionFromEachPlayer();
        for (Player p : this.players) {
            if (p.equals(player)) continue;
            if (this.longestConnectionsForEachPlayer.get(player) <= this.longestConnectionsForEachPlayer.get(p)) {
                return false;
            }
        }
        return true;
    }


    private void getLongestConnectionFromEachPlayer() {
        //TODO call this method
        for (Player p : this.players) {
            this.longestConnectionsForEachPlayer.put(p, p.findLongestConnection());
        }
    }


    //endregion




    //region ----- GAME REQUESTS ---------------------------------------------------------------------------------------


    public String getOpenCards() {
        StringBuilder builder = new StringBuilder("getOpenCards");
        synchronized (this) {
            for (TrainCard card : this.trainCardsStack) {
                builder.append(":").append(trainCardsStack);
            }
            this.notify();
        }
        return builder.toString();
    }


    public String getMap() {
        StringBuilder builder = new StringBuilder();
        synchronized (this) {
            ArrayList<RailroadLine> railroadLines = new ArrayList<>(map.getRailroadLines());
            for (int i = 0; i < railroadLines.size() - 1; i++) {
                if (railroadLines.get(i) instanceof DoubleRailroadLine)
                    builder.append(i).append(":").append(railroadLines.get(i).getOwner()).append(":").append(((DoubleRailroadLine) railroadLines.get(i)).getOwner2()).append(";");
                else
                    builder.append(i).append(":").append(railroadLines.get(i).getOwner()).append(";");
            }
            if (railroadLines.get(railroadLines.size() - 1) instanceof DoubleRailroadLine)
                builder.append(railroadLines.size() - 1).append(":").append(railroadLines.get(railroadLines.size() - 1).getOwner()).append(":").append(((DoubleRailroadLine) railroadLines.get(railroadLines.size() - 1)).getOwner2()).append(";");
            else
                builder.append(railroadLines.size() - 1).append(":").append(railroadLines.get(railroadLines.size() - 1).getOwner()).append(";");
            this.notify();
        }
        return builder.toString();
    }


    public String getColors() {
        StringBuilder builder = new StringBuilder("getColors");
        for (Player player : this.players) {
            builder.append(player.getName()).append(player.getPlayerColor().toString());
        }

        return builder.toString();
    }


    public String getPoints() {
        StringBuilder builder = new StringBuilder("getColors");
        synchronized (this) {
            for (Player player : this.players) {
                builder.append(player.getName()).append(player.getPlayerPoints());
            }
            this.notify();
        }
        return builder.toString();
    }


    //endregion




    //region ----- GAME COMMANDS ---------------------------------------------------------------------------------------


    /**
     * starts the game, if the owner calls this method and there are more than two players in the game
     * @param whoIsPerformingThisAction calling player - should be owner
     * @return 0 on success, -1 on fail
     */
    public int startGame (Player whoIsPerformingThisAction) {
        if (Configuration_Constants.verbose) System.out.println(("(VERBOSE)\t GameModel.startGame() starting game " + this.name + "..."));
        if (!whoIsPerformingThisAction.equals(owner)) {
            System.out.println("(DEBUG)\tGameModel.startGame() called from player who is not owner");
            return -1;
        }

        if (this.state != State.WAITING_FOR_PLAYERS) {
            System.out.println("(DEBUG)\tGameModel.startGame() Game is not in state WAITING_FOR_PLAYERS!");
            return -1;
        }

        this.state = State.RUNNING;
        Thread gameLoop = new Thread(this);
        Collections.shuffle(players);
        gameLoop.start();
        return 0;
    }


    public int drawOpenCard(Player player, int openCardId) {
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\t Player" + player.getName() + " was blocked trying pick open card while players " + players.get(activePlayer) + "turn.");
                return -1;
            }

            //TODO: check if the chosen card is a TrainCard (costs TrainCard = 3 => then turn is over)

            if (actionsLeft == 2) {
                //TODO: draw cards and call player.addHandCard(getCardfromStack(OpenCardID) or something
                return actionsLeft -= 2;
            };
            if (actionsLeft == 2 || actionsLeft == 1) {
                return --actionsLeft;
            }
            this.notify();
        }
        throw new IllegalStateException("(FATAL) GameModel: No more moves left when called drawOpenCard");
    }


    public int drawCardFromStack(Player player) {
        int retVal = -1;
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\tGameModel.drawCardFromStack() Player" + player.getName() + " was blocked trying pick card from stack while players " + players.get(activePlayer) + "turn.");
            }
            else {
                if (Configuration_Constants.verbose)
                    System.out.println("(VERBOSE)\tGameModel.drawCardFromStack() drawing card...");
                if (actionsLeft > 0 && trainCardsStack.size() > 0) {
                    TrainCard card = trainCardsStack.remove(0);
                    player.addHandCard(card);
                    --actionsLeft;
                    retVal = 0;
                }
            }
            this.notify();
        }
        return retVal;
    }


    public int setRailRoadLineOwner(Player player, RailroadLine railroadLine, MapColor color){
        int retVal = -1;
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\tPlayer" + player.getName() + " was blocked trying to build road while players " + players.get(activePlayer) + "turn.");
            }
            else if (railroadLine.getOwner() != null) {
                if (Configuration_Constants.debug)
                    System.out.println("(DEBUG)\tGameModel.setRailRoadLineOwner() Rail has owner named " + railroadLine.getOwner().getName() + ". Can't set owner to " + player.getName());
            }
            else if (railroadLine instanceof DoubleRailroadLine) {
                DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;
                if (doubleRailroadLine.getColor2().equals(color) && doubleRailroadLine.getOwner2() == null) {
                    doubleRailroadLine.setOwner2(player);
                    retVal = 0;
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\nGameModel.setRailRoadLineOwner() DoubleRailOwner="+player.getName());
                }
                else if (doubleRailroadLine.getColor().equals(color)) {
                    doubleRailroadLine.setOwner(player);
                    retVal = 0;
                    if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\nGameModel.setRailRoadLineOwner() DoubleRailOwner="+player.getName());
                }
            }
            else {
                this.actionsLeft = 0;
                railroadLine.setOwner(player);
                retVal = 0;
                if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\nGameModel.setRailRoadLineOwner() RailOwner="+player.getName());
            }
            this.notify();
        }
        return retVal;
    }

    public String drawMission(Player player) {
        //TODO impl Method
        throw new IllegalStateException("(FATAL) GameModel: At this point the move should be processed");
    }


    public int exitGame (Player player) {
        //TODO impl
        return -1;
    }


    //endregion





    //region ------------------- GETTER SETTER TO_STRING ----------------------------
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
                + ", owner=" + owner.getName() + "\n"
                + "\tPlayers:";
        for (Player player : players) toString += "\t" + player.toString() + "\n";
        return toString;
    }


    //endregion




    //region ---------------------- STATIC GENERATORS ---------------------------------------


    private static Map getMapInstance() {
        Map map = new Map();
        Destination atlanta = new Destination("Atlanta");
        Destination boston = new Destination("Boston");
        Destination calgary = new Destination("Calgary");
        Destination chicago = new Destination("Chicago");
        Destination dallas = new Destination("Dallas");
        Destination denver = new Destination("Denver");
        Destination duluth = new Destination("Duluth");
        Destination elpaso = new Destination("El Paso");
        Destination helena = new Destination("Helena");
        Destination houston = new Destination("Houston");
        Destination kansascity = new Destination("Kansas City");
        Destination littlerock = new Destination("Little Rock");
        Destination losangeles = new Destination("Los Angeles");
        Destination miami = new Destination("Miami");
        Destination montreal = new Destination("Montreal");
        Destination nashville = new Destination("Nashville");
        Destination neworleans = new Destination("New Orleans");
        Destination newyork = new Destination("New York");
        Destination oklahomacity = new Destination("Oklahoma City");
        Destination phoenix = new Destination("Phoenix");
        Destination pittsburgh = new Destination("Pittsburgh");
        Destination portland = new Destination("Portland");
        Destination saltlakecity = new Destination("Salt Lake City");
        Destination sanfrancisco = new Destination("San Francisco");
        Destination santafe = new Destination("Santa Fe");
        Destination saultstmarie = new Destination("Sault St. Marie");
        Destination seattle = new Destination("Seattle");
        Destination toronto = new Destination("Toronto");
        Destination vancouver = new Destination("Vancouver");
        Destination winnipeg = new Destination("Winnipeg");
        Destination omaha = new Destination("Omaha");
        Destination washington = new Destination("Washington");
        Destination lasvegas = new Destination("Las Vegas");
        Destination charleston = new Destination("Charleston");
        Destination saintlouis = new Destination("Saint Louis");
        Destination raleigh = new Destination("Raleigh");
        map.addDestination(raleigh);
        map.addDestination(charleston);
        map.addDestination(saintlouis);
        map.addDestination(lasvegas);
        map.addDestination(washington);
        map.addDestination(omaha);
        map.addDestination(atlanta);
        map.addDestination(boston);
        map.addDestination(calgary);
        map.addDestination(chicago);
        map.addDestination(dallas);
        map.addDestination(denver);
        map.addDestination(duluth);
        map.addDestination(elpaso);
        map.addDestination(helena);
        map.addDestination(houston);
        map.addDestination(kansascity);
        map.addDestination(littlerock);
        map.addDestination(losangeles);
        map.addDestination(miami);
        map.addDestination(montreal);
        map.addDestination(nashville);
        map.addDestination(neworleans);
        map.addDestination(newyork);
        map.addDestination(oklahomacity);
        map.addDestination(phoenix);
        map.addDestination(pittsburgh);
        map.addDestination(portland);
        map.addDestination(saltlakecity);
        map.addDestination(sanfrancisco);
        map.addDestination(santafe);
        map.addDestination(saultstmarie);
        map.addDestination(seattle);
        map.addDestination(toronto);
        map.addDestination(vancouver);
        map.addDestination(winnipeg);

        map.addRailroadLine(new RailroadLine(vancouver, calgary, MapColor.GRAY, 3));
        map.addRailroadLine(new RailroadLine(calgary, winnipeg, MapColor.WHITE, 6));
        map.addRailroadLine(new RailroadLine(winnipeg, saultstmarie, MapColor.GRAY, 6));
        map.addRailroadLine(new RailroadLine(saultstmarie, montreal, MapColor.BLACK, 5));
        map.addRailroadLine(new DoubleRailroadLine(montreal, boston, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(montreal, newyork, MapColor.BLUE, 3));
        map.addRailroadLine(new RailroadLine(montreal, toronto, MapColor.GRAY, 3));
        map.addRailroadLine(new DoubleRailroadLine(newyork, boston, MapColor.YELLOW, 2, MapColor.RED));
        map.addRailroadLine(new DoubleRailroadLine(newyork, pittsburgh, MapColor.YELLOW, 2, MapColor.GREEN));
        map.addRailroadLine(new RailroadLine(toronto, pittsburgh, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(toronto, saultstmarie, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(toronto, duluth, MapColor.PINK, 6));
        map.addRailroadLine(new RailroadLine(saultstmarie, duluth, MapColor.GRAY, 3));
        map.addRailroadLine(new RailroadLine(duluth, winnipeg, MapColor.BLACK, 4));
        map.addRailroadLine(new RailroadLine(winnipeg, helena, MapColor.BLUE, 4));
        map.addRailroadLine(new RailroadLine(helena, calgary, MapColor.GRAY, 4));
        map.addRailroadLine(new RailroadLine(helena, duluth, MapColor.ORANGE, 6));
        map.addRailroadLine(new RailroadLine(helena, seattle, MapColor.YELLOW, 6));
        map.addRailroadLine(new DoubleRailroadLine(seattle, vancouver, MapColor.GRAY, 1, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(seattle, calgary, MapColor.GRAY, 4));
        map.addRailroadLine(new DoubleRailroadLine(seattle, portland, MapColor.GRAY, 1, MapColor.GRAY));
        map.addRailroadLine(new DoubleRailroadLine(portland, sanfrancisco, MapColor.GREEN, 5, MapColor.PINK));
        map.addRailroadLine(new DoubleRailroadLine(sanfrancisco, saltlakecity, MapColor.ORANGE, 5, MapColor.WHITE));
        map.addRailroadLine(new RailroadLine(saltlakecity, portland, MapColor.BLUE, 6));
        map.addRailroadLine(new RailroadLine(saltlakecity, helena, MapColor.PINK, 3));
        map.addRailroadLine(new RailroadLine(helena, omaha, MapColor.RED, 5));
        map.addRailroadLine(new DoubleRailroadLine(omaha, duluth, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(duluth, chicago, MapColor.RED, 3));
        map.addRailroadLine(new RailroadLine(chicago, toronto, MapColor.WHITE, 4));
        map.addRailroadLine(new DoubleRailroadLine(newyork, washington, MapColor.ORANGE, 2, MapColor.BLACK));
        map.addRailroadLine(new RailroadLine(washington, pittsburgh, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(pittsburgh, chicago, MapColor.BLACK, 3));
        map.addRailroadLine(new RailroadLine(chicago, omaha, MapColor.BLUE, 4));
        map.addRailroadLine(new RailroadLine(omaha, denver, MapColor.PINK, 4));
        map.addRailroadLine(new RailroadLine(denver, helena, MapColor.GREEN, 4));
        map.addRailroadLine(new RailroadLine(denver, saltlakecity, MapColor.YELLOW, 3));
        map.addRailroadLine(new RailroadLine(saltlakecity, lasvegas, MapColor.ORANGE, 3));
        map.addRailroadLine(new RailroadLine(lasvegas, losangeles, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(losangeles, phoenix, MapColor.GRAY, 3));
        map.addRailroadLine(new RailroadLine(phoenix, elpaso, MapColor.GRAY, 3));
        map.addRailroadLine(new RailroadLine(elpaso, losangeles, MapColor.BLACK, 6));
        map.addRailroadLine(new DoubleRailroadLine(losangeles, sanfrancisco, MapColor.YELLOW, 3, MapColor.PINK));
        map.addRailroadLine(new RailroadLine(santafe, denver, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(santafe, oklahomacity, MapColor.BLUE, 3));
        map.addRailroadLine(new RailroadLine(santafe, elpaso, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(santafe, phoenix, MapColor.GRAY, 3));
        map.addRailroadLine(new RailroadLine(elpaso, oklahomacity, MapColor.YELLOW, 5));
        map.addRailroadLine(new RailroadLine(elpaso, dallas, MapColor.RED, 4));
        map.addRailroadLine(new RailroadLine(elpaso, houston, MapColor.GREEN, 6));
        map.addRailroadLine(new RailroadLine(houston, neworleans, MapColor.RED, 2));
        map.addRailroadLine(new DoubleRailroadLine(houston, dallas, MapColor.GRAY, 1, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(dallas, littlerock, MapColor.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(dallas, oklahomacity, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(oklahomacity, littlerock, MapColor.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(oklahomacity, kansascity, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(oklahomacity, denver, MapColor.RED, 4));
        map.addRailroadLine(new RailroadLine(littlerock, neworleans, MapColor.GREEN, 3));
        map.addRailroadLine(new RailroadLine(littlerock, nashville, MapColor.WHITE, 3));
        map.addRailroadLine(new RailroadLine(littlerock, saintlouis, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(neworleans, miami, MapColor.RED, 6));
        map.addRailroadLine(new DoubleRailroadLine(neworleans, atlanta, MapColor.ORANGE, 4, MapColor.YELLOW));
        map.addRailroadLine(new RailroadLine(atlanta, miami, MapColor.BLUE, 5));
        map.addRailroadLine(new RailroadLine(atlanta, charleston, MapColor.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(atlanta, raleigh, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(atlanta, nashville, MapColor.GRAY, 1));
        map.addRailroadLine(new RailroadLine(miami, charleston, MapColor.PINK, 4));
        map.addRailroadLine(new RailroadLine(charleston, raleigh, MapColor.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(raleigh, washington, MapColor.GRAY, 2, MapColor.GRAY));
        map.addRailroadLine(new RailroadLine(raleigh, pittsburgh, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(raleigh, nashville, MapColor.BLACK, 3));
        map.addRailroadLine(new RailroadLine(nashville, pittsburgh, MapColor.YELLOW, 4));
        map.addRailroadLine(new RailroadLine(nashville, saintlouis, MapColor.GRAY, 2));
        map.addRailroadLine(new RailroadLine(saintlouis, pittsburgh, MapColor.GREEN, 5));
        map.addRailroadLine(new DoubleRailroadLine(saintlouis, chicago, MapColor.GREEN, 2, MapColor.WHITE));
        map.addRailroadLine(new DoubleRailroadLine(saintlouis, kansascity, MapColor.BLUE, 2, MapColor.PINK));
        map.addRailroadLine(new DoubleRailroadLine(kansascity, omaha, MapColor.GRAY, 1, MapColor.GRAY));
        map.addRailroadLine(new DoubleRailroadLine(kansascity, denver, MapColor.BLACK, 4, MapColor.ORANGE));
        map.addRailroadLine(new RailroadLine(denver, phoenix, MapColor.WHITE, 5));

        return map;
    }


    private ArrayList<Mission> getMissions() {
        ArrayList<Mission> missions = new ArrayList<>();

        missions.add(new Mission(map.getDestinationByName("Boston"), map.getDestinationByName("Miami"), 12));
        missions.add(new Mission(map.getDestinationByName("Calgary"), map.getDestinationByName("Phoenix"), 13));
        missions.add(new Mission(map.getDestinationByName("Calgary"), map.getDestinationByName("Salt Lake City"), 7));
        missions.add(new Mission(map.getDestinationByName("Chicago"), map.getDestinationByName("New Orleans"), 7));
        missions.add(new Mission(map.getDestinationByName("Chicago"), map.getDestinationByName("Santa Fe"), 9));
        missions.add(new Mission(map.getDestinationByName("Dallas"), map.getDestinationByName("New York"), 11));
        missions.add(new Mission(map.getDestinationByName("Denver"), map.getDestinationByName("El Paso"), 4));
        missions.add(new Mission(map.getDestinationByName("Denver"), map.getDestinationByName("Pittsburgh"), 11));
        missions.add(new Mission(map.getDestinationByName("Duluth"), map.getDestinationByName("El Paso"), 10));
        missions.add(new Mission(map.getDestinationByName("Duluth"), map.getDestinationByName("Houston"), 8));
        missions.add(new Mission(map.getDestinationByName("Helena"), map.getDestinationByName("Los Angeles"), 8));
        missions.add(new Mission(map.getDestinationByName("Kansas City"), map.getDestinationByName("Houston"), 5));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("Chicago"), 16));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("Miami"), 20));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("New York"), 21));
        missions.add(new Mission(map.getDestinationByName("Montreal"), map.getDestinationByName("Atlanta"), 9));
        missions.add(new Mission(map.getDestinationByName("Montreal"), map.getDestinationByName("New Orleans"), 13));
        missions.add(new Mission(map.getDestinationByName("New York"), map.getDestinationByName("Atlanta"), 6));
        missions.add(new Mission(map.getDestinationByName("Portland"), map.getDestinationByName("Nashville"), 17));
        missions.add(new Mission(map.getDestinationByName("Portland"), map.getDestinationByName("Phoenix"), 11));
        missions.add(new Mission(map.getDestinationByName("San Francisco"), map.getDestinationByName("Atlanta"), 17));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"), map.getDestinationByName("Nashville"), 8));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"), map.getDestinationByName("Oklahoma City"), 9));
        missions.add(new Mission(map.getDestinationByName("Seattle"), map.getDestinationByName("Los Angeles"), 9));
        missions.add(new Mission(map.getDestinationByName("Seattle"), map.getDestinationByName("New York"), 22));
        missions.add(new Mission(map.getDestinationByName("Toronto"), map.getDestinationByName("Miami"), 10));
        missions.add(new Mission(map.getDestinationByName("Vancouver"), map.getDestinationByName("Montreal"), 20));
        missions.add(new Mission(map.getDestinationByName("Vancouver"), map.getDestinationByName("Santa Fe"), 13));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"), map.getDestinationByName("Houston"), 12));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"), map.getDestinationByName("Little Rock"), 11));

        return missions;
    }


    private static ArrayList<TrainCard> getTrainCards() {
        ArrayList<TrainCard> cards = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            cards.add(new TrainCard(TrainCard.Type.PINK));
            cards.add(new TrainCard(TrainCard.Type.BLUE));
            cards.add(new TrainCard(TrainCard.Type.GREEN));
            cards.add(new TrainCard(TrainCard.Type.YELLOW));
            cards.add(new TrainCard(TrainCard.Type.RED));
            cards.add(new TrainCard(TrainCard.Type.WHITE));
            cards.add(new TrainCard(TrainCard.Type.ORANGE));
            cards.add(new TrainCard(TrainCard.Type.BLACK));
        }
        for (int i = 0; i < 14; i++) {
            cards.add(new TrainCard(TrainCard.Type.LOCOMOTIVE));
        }

        Collections.shuffle(cards);

        return cards;
    }


    //endregion
}
