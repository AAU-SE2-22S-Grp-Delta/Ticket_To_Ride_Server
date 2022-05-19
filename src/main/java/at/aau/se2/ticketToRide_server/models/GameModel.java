package at.aau.se2.ticketToRide_server.models;

import at.aau.se2.ticketToRide_server.dataStructures.*;
import at.aau.se2.ticketToRide_server.helpers.PointsHelper;
import at.aau.se2.ticketToRide_server.server.Configuration_Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

enum State {
    WAITING_FOR_PLAYERS, RUNNING, OVER, CRASHED
}

public class GameModel implements Runnable {
    private static int idCounter = 0;
    private static Map map = getMap();

    //meta
    private int id;
    private String name;
    private State state;
    private int colorCounter = 0;   //to assign colors to players
    private int actionsLeft;        //to manage a move
    private int countdown = -1;          // for the last moves before end


    //invisible
    private static ArrayList<Player> players;
    private Player owner;
    private ArrayList<TrainCard> trainCards;
    private ArrayList<Mission> missions;
    private int activePlayer = 0;  //counts who is next
    private int pointerTrainCards = 110;

    //visible to all
    private ArrayList<TrainCard> openCards = new ArrayList<>();

    public GameModel(String name, Player owner) {
        this.id = idCounter++;
        this.name = name;
        this.state = State.WAITING_FOR_PLAYERS;
        players = new ArrayList<>();

        addPlayer(owner);

        this.trainCards = getTrainCards();
        this.missions = getMissions();
    }

    //region ----------------  WAITING FOR PLAYERS ---------------------------------------

    public int addPlayer(Player player) {
        try {
            if (player == null) throw new IllegalArgumentException("Player is NULL!");
            if (players.size() > 4) throw new IllegalArgumentException("Board is full!");
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
            case 0 -> player.setPlayerColor(Player.Color.RED);
            case 1 -> player.setPlayerColor(Player.Color.BLUE);
            case 2 -> player.setPlayerColor(Player.Color.GREEN);
            case 3 -> player.setPlayerColor(Player.Color.YELLOW);
            case 4 -> player.setPlayerColor(Player.Color.BLACK);
        }
        player.setGaming();
        this.owner = player;
        return 0;
    }

    public void startGame(Player whoIsPerformingThisAction) {
        if (!whoIsPerformingThisAction.equals(owner)) throw new IllegalCallerException(whoIsPerformingThisAction.getName() + " is not the owner, aborting to start game!");
        if (this.state != State.WAITING_FOR_PLAYERS) throw new IllegalStateException("Game is not in state WAITING_FOR_PLAYERS!");
        this.state = State.RUNNING;
        Thread gameLoop = new Thread(this);
        Collections.shuffle(players);
        gameLoop.start();
    }

    private void exitGameCrashed() {
        this.state = State.CRASHED;
        //TODO
        //try to recover?
        //endGame
        //notify Players
    }

    //endregion




    //region -------------------- GAME INITIALIZATION ------------------------------

    //TODO init visible cards
    public ArrayList<TrainCard> getOpenCards()
    {
        ArrayList<TrainCard> openCards = new ArrayList<>();
        ArrayList<TrainCard>  allTrainCards = getTrainCards();

        for(int i = 1; i < 5; i++)
        {
            openCards.add(allTrainCards.get(pointerTrainCards));
            pointerTrainCards--;
        }

        return openCards;
    }


    //endregion




    //region ----------------------- GAMING -----------------------------------------

    @Override
    public void run() {
        while (checkIfOver()) {
            move();
        }
        calculatePointsAndSendResult();
    }

    /**
     * checks if the game is over
     * @return true on over
     */
    private boolean checkIfOver() {
        if(countdown != -1) {
            countdown--;
        }

        if(countdown == 0) {
            state = State.OVER;
            return true;
        } else {
            for (Player player:players) {
                if(player.getNumStones() <= 2)
                {
                    if(countdown != -1)
                    {
                        countdown = players.size();
                    }
                }
            }
            return false;
        }
        //check if each player has at least 2 wagons or, if there is a running countdown
        //if a player has less than 2, each other player has one move left (start count down)
        //if countdown is over, set state to OVER

        return false;
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
        //wait for move and inform clients
        this.actionsLeft = 3;
        for (Player player : players) {
            player.doMove(players.get(activePlayer).getName(), actionsLeft);
        }

        while (actionsLeft > 0) {
            //lock info
            //write
            //unlock
            //broadcast sync flag
        }
    }

    private void calculatePointsAndSendResult() {
        //TODO impl
        for (Player player: players) {
            pointsHelper.numberOfConnectedRailroads(player);
            pointsHelper.calculateSum(player);
        }
    }

    //endregion




    //region ---------------------- PLAYER ACTIONS ----------------------------------


    public int drawOpenCard(Player player, int openCardId) {
        if (!players.get(activePlayer).equals(player)) {
            //TODO return failure or block info?
            if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\t Player" + player.getName() +" was blocked trying pick open card while players " + players.get(activePlayer) + "turn.");
            return -1;
        }

        //TODO: check if the chosen card is a Traincard (costs TrainCard = 3 => then turn is over)

        if (actionsLeft == 3) {
            //TODO: draw cards and call player.addHandCard(getCardfromStack(OpenCardID) or something
            return actionsLeft-=2;
        }
        if (actionsLeft == 2|| actionsLeft==1) {
            return --actionsLeft;
        }
        throw new IllegalStateException("(FATAL) GameModel: No more moves left when called drawOpenCard");
    }

    public int drawCardFromStack(Player player) {
        if (!players.get(activePlayer).equals(player)) {
            //TODO return failure or block info?
            if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\t Player" + player.getName() +" was blocked trying pick card from stack while players " + players.get(activePlayer) + "turn.");
            return -1;
        }
        if (actionsLeft > 0 && trainCards.size() > 0) {
            //TODO this is a temp workaround for testing purposes
            TrainCard card = trainCards.remove(0);
            player.addHandCard(card);
            --actionsLeft;
        }
        throw new IllegalStateException("(FATAL) GameModel: At this point the move should be processed");
    }

    public int buildRailroad(Player player, RailroadLine railroadLine, MapColor color) {
        if (!players.get(activePlayer).equals(player)) {
            //TODO return failure or block info?
            if (Configuration_Constants.verbose) System.out.println("(VERBOSE)\t Player" + player.getName() +" was blocked trying to build road while players " + players.get(activePlayer) + "turn.");
            return -1;
        }
        //TODO impl Method
        //check costs
//        if (railroadLine.getColor() == color && railroadLine.getDistance() )
        //build
        //remove handcards (impl method in Player)
        //check if a mission was completed
        throw new IllegalStateException("(FATAL) GameModel: At this point the move should be processed");
    }

    public int drawMission(Player player) {
        //TODO impl Method
        throw new IllegalStateException("(FATAL) GameModel: At this point the move should be processed");
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
                + ", owner=" + owner.getName()+ "\n"
                + "\tPlayers:";
        for(Player player : players) toString+="\t"+player.toString()+"\n";
        return toString;
    }


    //endregion


    //region ---------------------- STATIC GENERATORS ---------------------------------------


    private static Map getMap() {
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

        map.addRailroadLine(new RailroadLine(vancouver, calgary, MapColors.GRAY, 3));
        map.addRailroadLine(new RailroadLine(calgary,winnipeg, MapColors.WHITE, 6));
        map.addRailroadLine(new RailroadLine(winnipeg, saultstmarie, MapColors.GRAY, 6));
        map.addRailroadLine(new RailroadLine(saultstmarie, montreal, MapColors.BLACK, 5));
        map.addRailroadLine(new DoubleRailroadLine(montreal, boston, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(montreal, newyork, MapColors.BLUE, 3));
        map.addRailroadLine(new RailroadLine(montreal, toronto, MapColors.GRAY, 3));
        map.addRailroadLine(new DoubleRailroadLine(newyork, boston, MapColors.YELLOW, 2, MapColors.RED));
        map.addRailroadLine(new DoubleRailroadLine(newyork, pittsburgh, MapColors.YELLOW, 2, MapColors.GREEN));
        map.addRailroadLine(new RailroadLine(toronto, pittsburgh, MapColors.GRAY,2));
        map.addRailroadLine(new RailroadLine(toronto, saultstmarie, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(toronto, duluth, MapColors.PINK, 6));
        map.addRailroadLine(new RailroadLine(saultstmarie, duluth, MapColors.GRAY, 3));
        map.addRailroadLine(new RailroadLine(duluth, winnipeg, MapColors.BLACK,4));
        map.addRailroadLine(new RailroadLine(winnipeg, helena, MapColors.BLUE, 4));
        map.addRailroadLine(new RailroadLine(helena, calgary, MapColors.GRAY, 4));
        map.addRailroadLine(new RailroadLine(helena, duluth, MapColors.ORANGE, 6));
        map.addRailroadLine(new RailroadLine(helena, seattle, MapColors.YELLOW, 6));
        map.addRailroadLine(new DoubleRailroadLine(seattle, vancouver, MapColors.GRAY, 1, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(seattle, calgary, MapColors.GRAY, 4));
        map.addRailroadLine(new DoubleRailroadLine(seattle, portland, MapColors.GRAY, 1, MapColors.GRAY));
        map.addRailroadLine(new DoubleRailroadLine(portland, sanfrancisco, MapColors.GREEN, 5, MapColors.PINK));
        map.addRailroadLine(new DoubleRailroadLine(sanfrancisco, saltlakecity, MapColors.ORANGE, 5, MapColors.WHITE));
        map.addRailroadLine(new RailroadLine(saltlakecity, portland, MapColors.BLUE, 6));
        map.addRailroadLine(new RailroadLine(saltlakecity, helena, MapColors.PINK, 3));
        map.addRailroadLine(new RailroadLine(helena, omaha, MapColors.RED, 5));
        map.addRailroadLine(new DoubleRailroadLine(omaha, duluth, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(duluth, chicago, MapColors.RED, 3));
        map.addRailroadLine(new RailroadLine(chicago, toronto, MapColors.WHITE, 4));
        map.addRailroadLine(new DoubleRailroadLine(newyork, washington, MapColors.ORANGE, 2, MapColors.BLACK));
        map.addRailroadLine(new RailroadLine(washington, pittsburgh, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(pittsburgh, chicago, MapColors.BLACK, 3));
        map.addRailroadLine(new RailroadLine(chicago, omaha, MapColors.BLUE, 4));
        map.addRailroadLine(new RailroadLine(omaha, denver, MapColors.PINK, 4));
        map.addRailroadLine(new RailroadLine(denver, helena, MapColors.GREEN,4));
        map.addRailroadLine(new RailroadLine(denver, saltlakecity, MapColors.YELLOW, 3));
        map.addRailroadLine(new RailroadLine(saltlakecity, lasvegas, MapColors.ORANGE, 3));
        map.addRailroadLine(new RailroadLine(lasvegas, losangeles, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(losangeles, phoenix, MapColors.GRAY, 3));
        map.addRailroadLine(new RailroadLine(phoenix, elpaso, MapColors.GRAY, 3));
        map.addRailroadLine(new RailroadLine(elpaso, losangeles, MapColors.BLACK, 6));
        map.addRailroadLine(new DoubleRailroadLine(losangeles, sanfrancisco, MapColors.YELLOW, 3, MapColors.PINK));
        map.addRailroadLine(new RailroadLine(santafe, denver, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(santafe, oklahomacity, MapColors.BLUE, 3));
        map.addRailroadLine(new RailroadLine(santafe, elpaso, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(santafe, phoenix, MapColors.GRAY, 3));
        map.addRailroadLine(new RailroadLine(elpaso, oklahomacity, MapColors.YELLOW, 5));
        map.addRailroadLine(new RailroadLine(elpaso, dallas, MapColors.RED,4));
        map.addRailroadLine(new RailroadLine(elpaso, houston, MapColors.GREEN, 6));
        map.addRailroadLine(new RailroadLine(houston, neworleans, MapColors.RED, 2));
        map.addRailroadLine(new DoubleRailroadLine(houston, dallas, MapColors.GRAY, 1, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(dallas, littlerock, MapColors.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(dallas, oklahomacity, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(oklahomacity, littlerock, MapColors.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(oklahomacity, kansascity, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(oklahomacity, denver, MapColors.RED, 4));
        map.addRailroadLine(new RailroadLine(littlerock, neworleans, MapColors.GREEN, 3));
        map.addRailroadLine(new RailroadLine(littlerock, nashville, MapColors.WHITE, 3));
        map.addRailroadLine(new RailroadLine(littlerock, saintlouis, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(neworleans, miami, MapColors.RED, 6));
        map.addRailroadLine(new DoubleRailroadLine(neworleans, atlanta, MapColors.ORANGE, 4, MapColors.YELLOW));
        map.addRailroadLine(new RailroadLine(atlanta, miami, MapColors.BLUE, 5));
        map.addRailroadLine(new RailroadLine(atlanta,charleston, MapColors.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(atlanta, raleigh, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(atlanta, nashville, MapColors.GRAY, 1));
        map.addRailroadLine(new RailroadLine(miami, charleston, MapColors.PINK, 4));
        map.addRailroadLine(new RailroadLine(charleston, raleigh, MapColors.GRAY, 2));
        map.addRailroadLine(new DoubleRailroadLine(raleigh, washington, MapColors.GRAY, 2, MapColors.GRAY));
        map.addRailroadLine(new RailroadLine(raleigh, pittsburgh, MapColors.GRAY, 2));
        map.addRailroadLine(new RailroadLine(raleigh, nashville, MapColors.BLACK, 3));
        map.addRailroadLine(new RailroadLine(nashville, pittsburgh, MapColors.YELLOW, 4));
        map.addRailroadLine(new RailroadLine(nashville, saintlouis, MapColors.GRAY,2));
        map.addRailroadLine(new RailroadLine(saintlouis, pittsburgh, MapColors.GREEN, 5));
        map.addRailroadLine(new DoubleRailroadLine(saintlouis, chicago, MapColors.GREEN, 2, MapColors.WHITE));
        map.addRailroadLine(new DoubleRailroadLine(saintlouis, kansascity, MapColors.BLUE, 2, MapColors.PINK));
        map.addRailroadLine(new DoubleRailroadLine(kansascity, omaha, MapColors.GRAY, 1, MapColors.GRAY));
        map.addRailroadLine(new DoubleRailroadLine(kansascity, denver, MapColors.BLACK, 4, MapColors.ORANGE));
        map.addRailroadLine(new RailroadLine(denver, phoenix, MapColors.WHITE, 5));

        return map;
    }


    private static ArrayList<Mission> getMissions() {
        ArrayList<Mission> missions = new ArrayList<>();

        missions.add(new Mission(map.getDestinationByName("Boston"),map.getDestinationByName("Miami"),12));
        missions.add(new Mission(map.getDestinationByName("Calgary"),map.getDestinationByName("Phoenix"),13));
        missions.add(new Mission(map.getDestinationByName("Calgary"),map.getDestinationByName("Salt Lake City"),7));
        missions.add(new Mission(map.getDestinationByName("Chicago"),map.getDestinationByName("New Orleans"),7));
        missions.add(new Mission(map.getDestinationByName("Chicago"),map.getDestinationByName("Santa Fe"),9));
        missions.add(new Mission(map.getDestinationByName("Dallas"),map.getDestinationByName("New York"),11));
        missions.add(new Mission(map.getDestinationByName("Denver"),map.getDestinationByName("El Paso"),4));
        missions.add(new Mission(map.getDestinationByName("Denver"),map.getDestinationByName("Pittsburgh"),11));
        missions.add(new Mission(map.getDestinationByName("Duluth"),map.getDestinationByName("El Paso"),10));
        missions.add(new Mission(map.getDestinationByName("Duluth"),map.getDestinationByName("Houston"),8));
        missions.add(new Mission(map.getDestinationByName("Helena"),map.getDestinationByName("Los Angeles"),8));
        missions.add(new Mission(map.getDestinationByName("Kansas City"),map.getDestinationByName("Houston"),5));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"),map.getDestinationByName("Chicago"),16));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"),map.getDestinationByName("Miami"),20));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"),map.getDestinationByName("New York"),21));
        missions.add(new Mission(map.getDestinationByName("Montreal"),map.getDestinationByName("Atlanta"),9));
        missions.add(new Mission(map.getDestinationByName("Montreal"),map.getDestinationByName("New Orleans"),13));
        missions.add(new Mission(map.getDestinationByName("New York"),map.getDestinationByName("Atlanta"),6));
        missions.add(new Mission(map.getDestinationByName("Portland"),map.getDestinationByName("Nashville"),17));
        missions.add(new Mission(map.getDestinationByName("Portland"),map.getDestinationByName("Phoenix"),11));
        missions.add(new Mission(map.getDestinationByName("San Francisco"),map.getDestinationByName("Atlanta"),17));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"),map.getDestinationByName("Nashville"),8));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"),map.getDestinationByName("Oklahoma City"),9));
        missions.add(new Mission(map.getDestinationByName("Seattle"),map.getDestinationByName("Los Angeles"),9));
        missions.add(new Mission(map.getDestinationByName("Seattle"),map.getDestinationByName("New York"),22));
        missions.add(new Mission(map.getDestinationByName("Toronto"),map.getDestinationByName("Miami"),10));
        missions.add(new Mission(map.getDestinationByName("Vancouver"),map.getDestinationByName("Montreal"),20));
        missions.add(new Mission(map.getDestinationByName("Vancouver"),map.getDestinationByName("Santa Fe"),13));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"),map.getDestinationByName("Houston"),12));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"),map.getDestinationByName("Little Rock"),11));

        return missions;
    }


    private static ArrayList<TrainCard> getTrainCards() {
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

        Collections.shuffle(cards);

        return cards;
    }


    //endregion
}
