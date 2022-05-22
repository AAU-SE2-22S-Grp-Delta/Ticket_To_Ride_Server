package at.aau.se2.ticketToRide_server.helpers;

import static at.aau.se2.ticketToRide_server.models.GameModel.map;

import java.util.ArrayList;

import at.aau.se2.ticketToRide_server.dataStructures.Destination;
import at.aau.se2.ticketToRide_server.dataStructures.Mission;
import at.aau.se2.ticketToRide_server.dataStructures.Player;
import at.aau.se2.ticketToRide_server.dataStructures.RailroadLine;
import at.aau.se2.ticketToRide_server.models.GameModel;

public class PointsHelper {

    //Punkte für vollständige Strecken
    public int getPointsForRoutes(int lengthOfRoute){
        switch (lengthOfRoute){
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

    public void numberOfConnectedRailroads(Player player){
        ArrayList<RailroadLine> railroadLines = new ArrayList<>();
        int counter = 0;

        for (RailroadLine railroadLine: map.getRailroadLines()) {
            if(railroadLine.getOwner()==player) railroadLines.add(railroadLine);
        }
        //First destination
        Destination destination = railroadLines.get(0).getDestination2();
        for (int i = 0; i < railroadLines.size(); i++) {
            RailroadLine railroadLine = findRailroadLine(destination, railroadLines);
            if(railroadLine!= null){
                counter++;
                destination = railroadLine.getDestination2();
            }
        }
        player.setNumberOfConnectedRailroads(counter);
    }

    private RailroadLine findRailroadLine(Destination destination, ArrayList<RailroadLine> railroadLines){
        for (RailroadLine railroadLine: railroadLines){
            if (destination == railroadLine.getDestination1()) return railroadLine;
        }
        return null;
    }

    private boolean playerHasLongestRailroad(Player player){
        for(Player p: GameModel.getPlayers()){
            if(p.getNumberOfConnectedRailroads()>player.getNumberOfConnectedRailroads()) return false;
        }
        return true;
    }

    public int calculateSum(Player player){
        int sum = player.getNumStones();

        //Punkte von Zielkarten dazuzählen und abziehen
        for (Mission mission: player.getMissions()) {
            if (mission.isDone()) sum+=mission.getPoints();
            else sum-=mission.getPoints();
        }

        //Zusatzpunkte für längste Strecke
        if(playerHasLongestRailroad(player)) sum+=10;

        return sum;
    }
}
