package eis.agent;

import eis.iilang.*;
import serializers.GsonInstance;
import map.Direction;
import map.Position;

import java.util.LinkedList;

public class AgentLocation extends Percept {

    public AgentLocation(Position p) {
        this();
        this.currentLocation = p;
    }

    public String toJsonString() {
        return GsonInstance.getInstance().toJson(this.getCurrentLocation());
    }

    public enum ActionResultPerception {
        RESULT("lastActionResult"),
        ACTION("lastAction"),
        PARAMS("lastActionParams");

        private String name;

        ActionResultPerception(String name) {
            this.name = name;
        }

        public String getPerceptName() {
            return this.name;
        }

        public static ActionResultPerception GetEnumFromPerceptName(String name) {
            for (ActionResultPerception value : ActionResultPerception.values()) {
                if (value.getPerceptName().equalsIgnoreCase(name))
                    return value;
            }
            return null;
        }
    }

    private Position currentLocation;

    public AgentLocation() {
        super("location");
        currentLocation = new Position(0, 0);
    }


    public Position getCurrentLocation() {
        return currentLocation;
    }

    public void updateAgentLocation(Direction dir) {
        if (dir == null)
            return;


        this.currentLocation = currentLocation.add(dir.getPosition());
        System.out.println("Position is " + currentLocation + " after going " + dir.toString());
    }

    @Override
    public String toProlog()
    {
        return "location(" + currentLocation.getX() + ", " + currentLocation.getY() + ")";
    }

    @Override
    public LinkedList<Parameter> getParameters() {
        LinkedList<Parameter> params = new LinkedList<>();

        params.add(new Numeral(currentLocation.getX()));
        params.add(new Numeral(currentLocation.getY()));

        return params;
    }


}
