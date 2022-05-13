package at.aau.se2.ticketToRide_server.dataStructures;

import at.aau.se2.ticketToRide_server.server.Configuration_Constants;
import at.aau.se2.ticketToRide_server.server.Session;


/**
 * Player-Class represents a person who is playing the Game
 */
public class Player implements  Comparable
{
    public enum Color {
        RED(0), BLUE(1), GREEN(2), YELLOW(3), BLACK(4), WHITE(5);

        Color(int i) {
        }
    }

    public enum State {
        LOBBY, GAMING
    }

    private String name;
    private int id = 0;
    private Color playerColor;
    private int numStones;
    private State state;

    private Session session;

    public Player(String name, Session session)
    {
        this.id = id++;
        setName(name);
        this.state = State.LOBBY;
        this.session = session;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    //TODO unique name check
    public void setName(String name)
    {
        if (name == null) throw new IllegalArgumentException("name is null");
        if (name.length() == 0) throw new IllegalArgumentException("name.length is 0");
        this.name = name;
    }

    public void setGaming()
    {
        //TODO throws illegal state error
        if (state.equals(State.GAMING)) throw new IllegalStateException("Player is already gaming");
        state = State.GAMING;
        this.numStones = 45;
    }

    public Color getPlayerColor()
    {
        return playerColor;
    }

    public void setPlayerColor(Color playerColor) {
        if (this.state == State.GAMING) {
            if (Configuration_Constants.debug) System.out.println("(DEBUG)\tCalled Player.getPlayerColor() while Player was in Game!");
            return;
        }
        this.playerColor = playerColor;
    }

    public int sendCommand(String command) {
        return session.send(command);
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

    /**
     * compares this player with an object
     * @param player to be compared with this
     * @return 1 if no instanceof Player, 0 if the names are equal, -1 if names differ
     */
    @Override
    public int compareTo(Object player) {
        if  (!(player instanceof Player)) return 1;
        if (this.name.equals(((Player) player).name)) return 0;
        else return -1;
    }
}
