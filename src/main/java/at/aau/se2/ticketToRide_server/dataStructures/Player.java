package at.aau.se2.ticketToRide_server.dataStructures;

import at.aau.se2.ticketToRide_server.server.Session;

/**
 * Player-Class represents a person who is playing the Game
 */
public class Player
{
    public enum State {
        LOBBY, GAMING
    }

    private String name;
    private int id = 0;
    private int playerColor;
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

    public int getPlayerColor()
    {
        return playerColor;
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
}
