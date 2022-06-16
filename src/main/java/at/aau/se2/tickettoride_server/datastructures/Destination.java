package at.aau.se2.tickettoride_server.datastructures;



import java.util.HashSet;
import java.util.Set;

/**
 * Destination-Class represents a Point on the map which can be connected
 * with other destinations via railroad lines.
 */
public class Destination {
    //TODO exception-handling

    private static Set<String> names = new HashSet<>();
    private String name;

    /**
     * Creates a Destination Object and marks the name as used
     * @param name a unique name
     */
    public Destination(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name is null");
        if (name.length() == 0) throw new IllegalArgumentException("name.length is 0");
        if (names.contains(name)) System.out.println("Name already taken");
        names.remove(this.name);
        names.add(name);
        this.name = name;
    }
}
