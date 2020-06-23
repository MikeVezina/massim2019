//{ include("common.asl") }

@thing_percept[atomic]
+percept::thing(X, Y, entity, TEAM)
    :   hasThingPerception(X, Y, entity, TEAM) &
        percept::team(TEAM) & // Same team
        percept::location(L_X, L_Y) & // Get current location
        percept::step(STEP) // Synchronize step
    <-  .print("Friendly seen at ", X, ", ", Y, " at Step ", STEP);
        .send(operator, tell, hasFriendly(STEP, X, Y, location(L_X, L_Y))).


getTeamAgentLocation(AGENT, relative(X, Y))
    :-  percept::teamAgent(X, Y, AGENT).

getTeamAgentLocation(AGENT, absolute(X, Y))
    :-  percept::teamAgent(R_X, R_Y, AGENT) &
        calculateAbsolutePosition(relative(R_X, R_Y), absolute(X, Y)).


