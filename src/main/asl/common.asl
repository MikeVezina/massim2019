
// Initial Beliefs for things
thingType(entity).
thingType(block).
thingType(dispenser).

blockingType(entity).
blockingType(block).

// Agent to username translations
getAgentUsername(agentA1, "agent-TRG1").
getAgentUsername(agentA2, "agent-TRG2").
getAgentUsername(agentA3, "agent-TRG3").
getAgentUsername(agentA4, "agent-TRG4").
getAgentUsername(agentA5, "agent-TRG5").
getAgentUsername(agentA6, "agent-TRG6").
getAgentUsername(agentA7, "agent-TRG7").
getAgentUsername(agentA8, "agent-TRG8").
getAgentUsername(agentA9, "agent-TRG9").
getAgentUsername(agentA10, "agent-TRG10").


/***** Rules for Asserting List Properties ******/
assertListEmpty(L) :-
    .list(L) &
    L == [].

assertListHasElements(L) :-
    .list(L) &
    L \== [].

hasMarker(X, Y)
    :-  hasThingPerception(X, Y, marker, _).

hasTeamPerception(X, Y)
    :-  percept::team(TEAM) &
        hasThingPerception(X, Y, entity, TEAM).

/* Finds a 'thing' percept. If the thing is an entity, do not perceive self (X = 0,Y = 0) */
hasThingPerception(X, Y, TYPE, DETAILS) :-
	percept::thing(X, Y, TYPE, DETAILS) &
	(TYPE \== entity |
	(TYPE == entity & (X \== 0 | Y \== 0))).

/*** Rules for checking if block is attached ***/
// This only checks if the thing is attached to us.
hasAttached(X, Y, TYPE, DETAILS) :-
     eis.internal.get_attached_blocks(BLOCKS) &
     .member(ATT, BLOCKS) &
     (attached(X, Y, TYPE, DETAILS) = ATT).

hasAttached(X, Y) :-
   hasAttached(X, Y, _, _).

hasBlockingPerception(X, Y) :-
    hasThingPerception(X, Y, TYPE, _) &
    blockingType(TYPE).

calculateAbsolutePosition(relative(R_X, R_Y), absolute(A_X, A_Y)) :-
    not(.ground(A_X)) &
    not(.ground(A_Y)) &
    percept::location(L_X, L_Y) &
    (A_X = L_X + R_X) &
    (A_Y = L_Y + R_Y).

calculateRelativePosition(relative(R_X, R_Y), absolute(A_X, A_Y)) :-
    not(.ground(R_X)) &
    not(.ground(R_Y)) &
    percept::location(L_X, L_Y) &
    (R_X = A_X - L_X) &
    (R_Y = A_Y - L_Y).
     //&    .print("Abs: ", A_X, ", ", A_Y, ". Rel:", R_X, ", ", R_Y).

hasGoalPerception(X, Y) :-
    percept::goal(X, Y).

hasBlockPerception(X, Y, BLOCK) :-
    hasThingPerception(X, Y, block, BLOCK).

hasBlockPerception(BLOCK) :-
    hasBlockPerception(_,_,BLOCK).

hasBlockAttached(X, Y, BLOCK) :-
    hasAttached(X, Y, block, BLOCK).

hasBlockAttached(BLOCK) :-
    hasAttached(_, _, block, BLOCK).

hasDispenser(X, Y, BLOCK) :-
    hasThingPerception(X, Y, dispenser, BLOCK).