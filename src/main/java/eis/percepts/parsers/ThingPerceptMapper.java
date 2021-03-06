package eis.percepts.parsers;

import eis.iilang.Percept;
import eis.percepts.parsers.PerceptMapper;
import eis.percepts.things.Thing;

public class ThingPerceptMapper extends PerceptMapper<Thing> {

    public ThingPerceptMapper() {
        super();
    }

    @Override
    protected boolean shouldHandlePercept(Percept p) {
        return Thing.canParse(p);
    }

    @Override
    protected Thing mapPercept(Percept p) {
        return Thing.ParseThing(p);
    }
}
