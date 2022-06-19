package at.aau.se2.tickettoride_server.models;

import at.aau.se2.tickettoride_server.Logger;
import at.aau.se2.tickettoride_server.datastructures.*;
import at.aau.se2.tickettoride_server.datastructures.Map;
import at.aau.se2.tickettoride_server.server.ConfigurationConstants;
import at.aau.se2.tickettoride_server.server.Lobby;

import java.util.*;

enum State {
    WAITING_FOR_PLAYERS, RUNNING, OVER
}

public class GameModel implements Runnable {
    private static final String PLAYER = "Player";
    private static final String TURN = "turn";

    //meta
    private final String name;
    private State state;
    private int colorCounter = 0;                                       //to assign colors to players
    private int actionsLeft;                                            //to manage a move
    private int countdown = -1;                                         //for the last moves before end
    private LinkedList<Mission>[] set3s;                                //when player has to choose missions, the options are temp in here
    private boolean[] waitForCoice;                                     //when player has to choose missions, the game will remember to wait for the coice
    boolean stateChanged = false;
    private Player winner;


    //invisible
    private final ArrayList<Player> players;
    private Player owner;
    private final ArrayList<TrainCard> trainCardsStack;
    private final LinkedList<TrainCard> discardPile;
    private final ArrayList<Mission> missions;
    private int activePlayer = 0;  //counts who is next


    //visible to all
    private final Map map = getMapInstance();
    private final TrainCard[] openCards = new TrainCard[5];

    public boolean isActive(Player player)
    {
        return players.get(activePlayer).equals(player);
    }

    public GameModel(String name, Player owner) {
        this.name = name;
        this.state = State.WAITING_FOR_PLAYERS;
        players = new ArrayList<>();
        this.trainCardsStack = getTrainCards();
        discardPile = new LinkedList<>();
        this.missions = getMissions();
        this.owner = owner;
    }




    //region ----------------  WAITING FOR PLAYERS ---------------------------------------


    public int addPlayer(Player player) {
        try {
            if (player == null) throw new IllegalArgumentException("Player is NULL!");
            if (players.size() > 4) throw new IllegalStateException("Board is full!");
            if (this.state != State.WAITING_FOR_PLAYERS) throw new IllegalStateException("Game has already started!");
        } catch (IllegalArgumentException e) {
            Logger.exception(e.getMessage());
            return -1;
        }
        players.add(player);
        if (colorCounter > 4) {
            Logger.fatal("GameModel: colorCounter raised over 5, max value when executing addPlayer at this point should be 5. Execution crashed.");
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


    //endregion




    //region ------ REQUESTS FROM LOBBY --------------------------------------------------------------------------------


    //Format listPlayersGame:Player1.Player2.
    public String listPlayersGame() {
        StringBuilder builder = new StringBuilder("listPlayersGame:");

        synchronized (this) {
            for (Player player : this.players) {
                builder.append(player.getName()).append(".");
            }
            this.notifyAll();
        }
        return builder.toString();
    }


    public String getState() {
        String state;
        synchronized (this) {
            state = this.state.toString();
            this.notifyAll();
        }
        return state;
    }


    //endregion




    //region -------------------- GAME INITIALIZATION ------------------------------


    //TODO init visible cards
    private void initOpenCards() {
        for (int i = 0; i < 5; i++) {
            this.openCards[i] = this.trainCardsStack.remove(0);
        }
    }


    private void initMissionChoosers() {
        @SuppressWarnings("unchecked")
        LinkedList<Mission>[] missions = new LinkedList[players.size()];
        this.set3s = missions;
        this.waitForCoice = new boolean[players.size()];
        for (int i = 0; i < players.size(); i++) {
            set3s[i] = new LinkedList<>();
            waitForCoice[i] = false;
        }

    }


    private void playersDrawingMissionsAtGameStart() {
        for (this.activePlayer = 0; activePlayer < players.size(); activePlayer++) {
            players.get(activePlayer).missionInit();
            Logger.verbose("Prompting player " + players.get(activePlayer).getName() + " to draw Mission");
        }
        activePlayer = 0;
        synchronized (this) {
            try {
                boolean checkAllChosen;
                do {
                    this.wait();
                    checkAllChosen = false;
                    int check = 0;
                    for (int i = 0; i < players.size(); i++) {
                        if (!waitForCoice[i]) check++;
                    }
                    if (check == players.size()) checkAllChosen = true;
                } while (!checkAllChosen);
            } catch (InterruptedException e) {
                Logger.exception(e.getMessage());
                Thread.currentThread().interrupt();
            }
            this.notifyAll();
        }
    }

    //endregion




    //region ----- GAME LOOP -------------------------------------------------------------------------------------------


    @Override
    public void run() {
        this.initOpenCards();
        initMissionChoosers();
        playersDrawingMissionsAtGameStart();

        Logger.verbose("GameModel.run() Game loop up");
        while (!checkIfOver()) {
            move();
            Logger.verbose("GameModel.run() Next round");
            activePlayer = ++activePlayer % players.size();
        }
        Logger.verbose("GameModel.run() Game loop broke");
        synchronized (this) {
            calculatePointsAndFindWinner();
            for (Player player : players) player.gameOver();
            this.state = State.OVER;
            this.notifyAll();
        }
    }


    /**
     * checks if the game is over
     *
     * @return true on over
     */
    private boolean checkIfOver() {
        if (players.isEmpty()) {
            state = State.OVER;
            return true;
        }
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
        Logger.verbose("GameModel.move() move start");
        this.actionsLeft = 2;
        while (actionsLeft > 0) {
            try {
                synchronized (this) {
                    actionCall();
                    stateChanged = false;
                    Logger.verbose("GameModel.move() called and waiting for action");
                    this.wait(); //Waits until a action is done
                    sync();      //then the sync broadcast
                }
            } catch (InterruptedException e) {
                Logger.debug("Error in GameModel.move");
                Logger.exception(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Broadcasts to all players that this is player [name]'s turn
     */
    private void actionCall() {
        Logger.verbose("GameModel.actionCall() calling players...");
        String playerOnTheMove = players.get(this.activePlayer).getName();
        for (Player p : this.players) {
            p.actionCall(playerOnTheMove, actionsLeft);
        }
    }


    /**
     * Notifies all players that the game model has changed
     */
    private void sync() {
        if (!stateChanged) return;
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




    //region ----- END GAME METHODS ------------------------------------------------------------------------------------


    private void calculatePointsAndFindWinner() {
        Player longest = findPlayerWithLongestConnection();
        int counter = 0;
        int[] pointsAtEnd = new int[players.size()];
        for (Player player : this.players) {
            int additionalPoints = 0;
            if (longest != null && player.getName().equals(longest.getName())) additionalPoints = 10;
            pointsAtEnd[counter++] = player.calculatePointsAtGameEnd(additionalPoints);
        }

        int max = 0;
        for (int i = 0; i < pointsAtEnd.length; i++) {
            if (pointsAtEnd[i] > max) winner = players.get(i);
            else if (pointsAtEnd[i] == max) winner = null;
        }
    }


    private Player findPlayerWithLongestConnection() {
        Player player = null;
        int max = 0;
        for (Player p : this.players) {
            int current = p.findLongestConnection();
            if (current > max) {
                max = current;
                player = p;
            } else if (current == max) {
                player = null;
            }
        }
        return player;
    }


    //endregion




    //region ----- GAME REQUESTS ---------------------------------------------------------------------------------------


    //Format getOpenCards:Card1.Card2.
    public String getOpenCards() {
        StringBuilder builder = new StringBuilder("getOpenCards:");
        synchronized (this) {
            for (TrainCard card : this.openCards) {
                builder.append(card).append(".");
            }
            this.notifyAll();
        }
        return builder.toString();
    }


    //Format getMap:Line1,Line2,Owner1,Owner2.Line3,Line4,Owner3.
    public String getMap() {
        StringBuilder builder = new StringBuilder("getMap:");
        synchronized (this) {
            for (RailroadLine line : map.getRailroadLines()) {
                Player owner = line.getOwner();
                builder.append(line.getDestination1().getName()).append(",").append(line.getDestination2().getName()).append(",").append(owner == null ? "null" : owner.getName());
                if (line instanceof DoubleRailroadLine) {
                    Player owner2 = ((DoubleRailroadLine) line).getOwner2();
                    builder.append(",").append(owner2 == null ? "null" : owner2.getName());
                }
                builder.append(".");
            }

            this.notifyAll();
        }
        return builder.toString();
    }


    //Format getColors:Player1Green.Player2Blue.
    public String getColors() {
        StringBuilder builder = new StringBuilder("getColors:");
        for (Player player : this.players) {
            builder.append(player.getName()).append(player.getPlayerColor().toString()).append(".");
        }

        return builder.toString();
    }


    //Format


    /**
     * Returns the Points of all players in a String representation
     *
     * @return format = getPoints:Player120.Player215.
     */
    public String getPoints() {
        StringBuilder builder = new StringBuilder("getPoints:");
        synchronized (this) {
            for (Player player : this.players) {
                builder.append(player.getName()).append(player.getPlayerPoints()).append(".");
            }
            this.notifyAll();
        }
        return builder.toString();
    }


    public String cheatMission() {
        StringBuilder builder = new StringBuilder("cheatMission");
        synchronized (this) {
            for (Player player : this.players) {
                String missions = player.getMissions();
                String[] splitMissions = missions.split(":");
                builder.append(":").append(player.getName()).append(",");

                for (int i = 1; i < splitMissions.length; i++) {
                    if (i == splitMissions.length - 1) {
                        builder.append(splitMissions[i]);
                    } else {
                        builder.append(splitMissions[i]).append(",");
                    }
                }
            }
            this.notifyAll();
//            0. befehlsformat und was kommt zurück
//            Befehl vom Client 		cheatMission
//            Server schickt zurück		cheatMission:[playerName1],[mission1], .... , [missionN]:....:[playerNameN],[mission1], .... , [missionN]
        }
        cheat();
        return builder.toString();
    }


    /**
     * Notifies all players, that a player has cheated
     */
    public void cheat() {
        for (Player p : this.players) {
            p.cheat();
        }
    }


    //endregion


    /**
     * sends the name of the winner to the client, if the game is over
     * the winner is the player with the most points, if there are two
     * players who have the same number of points, there is no winner
     * there is still the option to request the points
     *
     * @return getWinner:[nameWinner] or getWinner:none on success, getWinner:null if the game isn't over yet
     */
    public String getWinner() {
        if (this.state != State.OVER) return "getWinner:null";
        if (winner == null) return "getWinner:none";
        return winner.getName();
    }


    //region ----- GAME COMMANDS ---------------------------------------------------------------------------------------


    /**
     * starts the game, if the owner calls this method and there are more than two players in the game
     *
     * @param whoIsPerformingThisAction calling player - should be owner
     * @return 0 on success, -1 on fail
     */
    public int startGame(Player whoIsPerformingThisAction) {
        Logger.verbose(("GameModel.startGame() starting game " + this.name + "..."));
        if (!whoIsPerformingThisAction.equals(owner)) {
            Logger.debug("GameModel.startGame() called from player who is not owner");
            return -1;
        }

        if (this.state != State.WAITING_FOR_PLAYERS) {
            Logger.debug("GameModel.startGame() Game is not in state WAITING_FOR_PLAYERS!");
            return -1;
        }

        this.state = State.RUNNING;
        Thread gameLoop = new Thread(this);
        Collections.shuffle(players);
        gameLoop.start();
        return 0;
    }


    public int drawOpenCard(Player player, int openCardId) {
        int retVal = -1;
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                Logger.verbose(PLAYER + player.getName() + " was blocked trying pick open card while players " + players.get(activePlayer) + TURN);
            } else if (openCardId < 0 || openCardId > 4) {
                Logger.verbose(PLAYER + player.getName() + " tried to pick card out of range: openCardId=" + openCardId);
            } else {
                boolean locomotive = openCards[openCardId].getType() == TrainCard.Type.LOCOMOTIVE;
                if (actionsLeft == 2 && locomotive) {
                    player.addHandCard(openCards[openCardId]);
                    openCards[openCardId] = drawCardFromStack();
                    stateChanged = true;
                    retVal = 0;
                    actionsLeft = 0;
                } else if (actionsLeft == 1 && locomotive) {
                    Logger.debug("GameModel.drawOpenCard() Player " + player.getName() + " tried to pick locomotive when actionsLeft=1");
                } else {
                    player.addHandCard(openCards[openCardId]);
                    openCards[openCardId] = drawCardFromStack();
                    stateChanged = true;
                    retVal = 0;
                    actionsLeft--;
                }
            }
            this.notifyAll();
        }
        return retVal;
    }


    public String drawCardFromStack(Player player) {
        String response = "cardStack:null";
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                Logger.verbose("GameModel.drawCardFromStack() Player" + player.getName() + " was blocked trying pick card from stack while players " + players.get(activePlayer) + TURN);
            } else {
                TrainCard card = drawCardFromStack();
                if (card != null) {
                    player.addHandCard(card);
                    stateChanged = true;
                    response = "cardStack:" + card.getType().toString();
                    actionsLeft--;
                }
            }
            this.notifyAll();
        }
        return response;
    }


    private TrainCard drawCardFromStack() {
        TrainCard card = null;
        synchronized (this) {
            Logger.verbose("GameModel.drawCardFromStack() drawing card...");

            boolean abort = false;
            if (trainCardsStack.isEmpty()) {
                while (!discardPile.isEmpty()) trainCardsStack.add(discardPile.remove());
                if (trainCardsStack.isEmpty()) {
                    actionsLeft = 0; //deadlock possible -> this is reset
                    abort = true;
                }
                Collections.shuffle(trainCardsStack);
            }

            if (!trainCardsStack.isEmpty() && !abort) {
                card = trainCardsStack.remove(0);
            }
            this.notifyAll();
        }
        return card;
    }


    public int setRailRoadLineOwner(Player player, RailroadLine railroadLine, MapColor color, LinkedList<TrainCard> cardsToBuildRail) {
        int retVal = -1;
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                Logger.debug(PLAYER + player.getName() + " was blocked trying to build road while players " + players.get(activePlayer) + TURN);
            }

            if ((railroadLine.getColor() == MapColor.GRAY || railroadLine.getColor() == color) && railroadLine.getOwner() == null) {
                railroadLine.setOwner(player);
                retVal = 0;
            } else if (railroadLine instanceof DoubleRailroadLine) {
                DoubleRailroadLine doubleRailroadLine = (DoubleRailroadLine) railroadLine;
                if ((doubleRailroadLine.getColor2() == MapColor.GRAY || doubleRailroadLine.getColor2() == color) && doubleRailroadLine.getOwner2() == null) {
                    doubleRailroadLine.setOwner2(player);
                    retVal = 0;
                }
            }
            if (retVal == 0) {
                this.actionsLeft = 0;
                returnCardsToDiscordPile(cardsToBuildRail);
                stateChanged = true;
                Logger.verbose("GameModel.setRailRoadLineOwner() RailOwner=" + player.getName());
            }
            this.notifyAll();
        }
        return retVal;
    }


    private void returnCardsToDiscordPile(LinkedList<TrainCard> cards) {
        discardPile.addAll(cards);
    }


    public String drawMission(Player player) {
        StringBuilder response = new StringBuilder("drawMission:null");
        synchronized (this) {
            if (!players.get(activePlayer).equals(player)) {
                Logger.debug(PLAYER + player.getName() + " was blocked trying to draw mission while players " + players.get(activePlayer) + TURN);
            } else {
                if (missions.isEmpty()) response = new StringBuilder("drawMission:empty");
                else if (!waitForCoice[activePlayer]) {
                    set3s[activePlayer] = new LinkedList<>();
                    for (int i = 0; !missions.isEmpty() && i < 3; i++) {
                        set3s[activePlayer].add(missions.remove(0));
                    }

                    response = new StringBuilder("drawMission");
                    for (Mission mission : set3s[activePlayer]) {
                        response.append(":").append(mission.getId());
                    }
                    waitForCoice[activePlayer] = true;
                } else {
                    Logger.debug("GameModel.drawMission() called when player has to coose mission");
                }
            }
            this.notifyAll();
        }
        return response.toString();
    }


    public int chooseMissions(LinkedList<Integer> chosen, Player player) {
        int retVal = -1;
        synchronized (this) {
            int playerPosition = 0;
            while (players.get(playerPosition).compareTo(player) != 0) playerPosition++; //find position in list
            if (waitForCoice[playerPosition]) {
                int counter = chosen.size(); //to check if all missions was dealt by the game
                LinkedList<Mission> toAdd = new LinkedList<>(), toDrop = new LinkedList<>();
                for (int choice : chosen) {
                    for (Mission mission : set3s[playerPosition]) {
                        if (mission.getId() == choice) {
                            counter--;
                            toAdd.add(mission);
                        } else toDrop.add(mission);
                    }
                }
                if (counter > 0) {
                    Logger.debug("GameModel.chooseMissions() illegal choice when called");
                } else {
                    for (Mission mission : toAdd) player.addMission(mission);
                    retVal = 0;
                    this.actionsLeft = 0;
                    waitForCoice[playerPosition] = false;
                }
                dropMissions(toDrop);
            } else {
                Logger.debug("GameModel.chooseMissions() called when not waiting on Player " + player.getName());
            }
            this.notifyAll();
        }
        return retVal;
    }


    public void dropMissions(LinkedList<Mission> backToStack) {
        missions.addAll(backToStack);
        Collections.shuffle(missions);
    }


    public void exitGame(Player player, ArrayList<TrainCard> handCards) {
        synchronized (this) {

            this.discardPile.addAll(handCards);
            int playerPosition = 0;
            while (players.get(playerPosition).compareTo(player) != 0) playerPosition++; //find position in list

            if (playerPosition == activePlayer) {
                actionsLeft = 0;
            }

            @SuppressWarnings("unchecked")
            LinkedList<Mission>[] newSet3s = new LinkedList[players.size() - 1];
            boolean[] newWaitForCoice = new boolean[players.size() - 1];

            //this is cause game could wait for player to choose missions
            if (state == State.RUNNING) {
                int counterNew = 0, counterOld = 0;
                for (Player p : this.players) {
                    if (!p.equals(player)) {
                        newSet3s[counterNew] = set3s[counterOld];
                        newWaitForCoice[counterNew++] = waitForCoice[counterOld++];
                    } else {
                        if (waitForCoice[counterOld]) dropMissions(set3s[counterOld]);
                        counterOld++;
                    }
                }
                set3s = newSet3s;
                waitForCoice = newWaitForCoice;
            }

            players.remove(playerPosition);
            Logger.verbose("GameModel.exitGame() removed Player " + player.getName() + "from game " + name);

            if (this.players.size() == 0) Lobby.getInstance().removeGame(this);
            if (player == this.owner &&players.size()>0) {
                this.owner = players.get(0);
            }

            this.notifyAll();
        }
    }


    //endregion




    //region ------------------- GETTER SETTER TO_STRING ----------------------------


    public String getName() {
        return name;
    }


    public Player getOwner() {
        return owner;
    }


    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder("name='" + name +
                ", state=" + state
                + ", owner=" + owner.getName() + "\n"
                + "\tPlayers:");
        for (Player player : players) toString.append("\t").append(player.toString()).append("\n");
        return toString.toString();
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

        if (!ConfigurationConstants.DOUBLE_RAILS) {
            Player dummy = Player.getDummy();
            for (RailroadLine rail : map.getRailroadLines()) {
                if (rail instanceof DoubleRailroadLine) {
                    ((DoubleRailroadLine) rail).setOwner2(dummy);
                }
            }
        }

        return map;
    }


    private ArrayList<Mission> getMissions() {
        ArrayList<Mission> missions = new ArrayList<>();

        missions.add(new Mission(map.getDestinationByName("Boston"), map.getDestinationByName("Miami"), 12, 1));
        missions.add(new Mission(map.getDestinationByName("Calgary"), map.getDestinationByName("Phoenix"), 13, 2));
        missions.add(new Mission(map.getDestinationByName("Calgary"), map.getDestinationByName("Salt Lake City"), 7, 3));
        missions.add(new Mission(map.getDestinationByName("Chicago"), map.getDestinationByName("New Orleans"), 7, 4));
        missions.add(new Mission(map.getDestinationByName("Chicago"), map.getDestinationByName("Santa Fe"), 9, 5));
        missions.add(new Mission(map.getDestinationByName("Dallas"), map.getDestinationByName("New York"), 11, 6));
        missions.add(new Mission(map.getDestinationByName("Denver"), map.getDestinationByName("El Paso"), 4, 7));
        missions.add(new Mission(map.getDestinationByName("Denver"), map.getDestinationByName("Pittsburgh"), 11, 8));
        missions.add(new Mission(map.getDestinationByName("Duluth"), map.getDestinationByName("El Paso"), 10, 9));
        missions.add(new Mission(map.getDestinationByName("Duluth"), map.getDestinationByName("Houston"), 8, 10));
        missions.add(new Mission(map.getDestinationByName("Helena"), map.getDestinationByName("Los Angeles"), 8, 11));
        missions.add(new Mission(map.getDestinationByName("Kansas City"), map.getDestinationByName("Houston"), 5, 12));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("Chicago"), 16, 13));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("Miami"), 20, 14));
        missions.add(new Mission(map.getDestinationByName("Los Angeles"), map.getDestinationByName("New York"), 21, 15));
        missions.add(new Mission(map.getDestinationByName("Montreal"), map.getDestinationByName("Atlanta"), 9, 16));
        missions.add(new Mission(map.getDestinationByName("Montreal"), map.getDestinationByName("New Orleans"), 13, 17));
        missions.add(new Mission(map.getDestinationByName("New York"), map.getDestinationByName("Atlanta"), 6, 18));
        missions.add(new Mission(map.getDestinationByName("Portland"), map.getDestinationByName("Nashville"), 17, 19));
        missions.add(new Mission(map.getDestinationByName("Portland"), map.getDestinationByName("Phoenix"), 11, 20));
        missions.add(new Mission(map.getDestinationByName("San Francisco"), map.getDestinationByName("Atlanta"), 17, 21));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"), map.getDestinationByName("Nashville"), 8, 22));
        missions.add(new Mission(map.getDestinationByName("Sault St. Marie"), map.getDestinationByName("Oklahoma City"), 9, 23));
        missions.add(new Mission(map.getDestinationByName("Seattle"), map.getDestinationByName("Los Angeles"), 9, 24));
        missions.add(new Mission(map.getDestinationByName("Seattle"), map.getDestinationByName("New York"), 22, 25));
        missions.add(new Mission(map.getDestinationByName("Toronto"), map.getDestinationByName("Miami"), 10, 26));
        missions.add(new Mission(map.getDestinationByName("Vancouver"), map.getDestinationByName("Montreal"), 20, 27));
        missions.add(new Mission(map.getDestinationByName("Vancouver"), map.getDestinationByName("Santa Fe"), 13, 28));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"), map.getDestinationByName("Houston"), 12, 29));
        missions.add(new Mission(map.getDestinationByName("Winnipeg"), map.getDestinationByName("Little Rock"), 11, 30));

        Collections.shuffle(missions);
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
